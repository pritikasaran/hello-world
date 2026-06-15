package net.java.spnego;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

public abstract class SpnegoServerAuthModule implements ServerAuthModule {

    public static final String AUTH_TYPE_INFO_KEY = "jakarta.servlet.http.authType";

    private static final Logger LOG = Logger.getLogger(SpnegoServerAuthModule.class.getName());

    private static final String DEBUG_OPTIONS_KEY          = "debug";
    private static final String POLICY_CONTEXT_OPTIONS_KEY = "jakarta.security.jacc.PolicyContext";
    private static final String IS_MANDATORY_INFO_KEY      = "jakarta.security.auth.message.MessagePolicy.isMandatory";
    private static final String AUTHORIZATION_HEADER       = "authorization";
    private static final String AUTHENTICATION_HEADER      = "WWW-Authenticate";
    private static final String NEGOTIATE                  = "Negotiate";
    private static final String NTLM_INITIAL_TOKEN         = "NTLMSSP";

    private static final Class<?>[] SUPPORTED_MESSAGE_TYPES =
            new Class<?>[]{ HttpServletRequest.class, HttpServletResponse.class };

    private MessagePolicy        requestPolicy;
    private MessagePolicy        responsePolicy;
    private CallbackHandler      handler;
    private Map<String, Object>  options;

    private boolean debug;
    private Level   debugLevel;
    private String  policyContextID;
    private boolean mandatory;
    private GSSManager gssManager;

    // -------------------------------------------------------------------------
    // ServerAuthModule lifecycle
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(MessagePolicy requestPolicy,
                           MessagePolicy responsePolicy,
                           CallbackHandler handler,
                           Map options) throws AuthException {
        this.requestPolicy  = requestPolicy;
        this.responsePolicy = responsePolicy;
        this.mandatory      = requestPolicy.isMandatory();
        this.handler        = handler;
        this.options        = (Map<String, Object>) options;

        if (options != null) {
            this.debug           = options.containsKey(DEBUG_OPTIONS_KEY);
            this.policyContextID = (String) options.get(POLICY_CONTEXT_OPTIONS_KEY);
        } else {
            this.debug           = false;
            this.policyContextID = null;
        }

        this.debugLevel = (LOG.isLoggable(Level.FINE) && !this.debug) ? Level.FINE : Level.INFO;
        this.gssManager = GSSManager.getInstance();
    }

    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    // -------------------------------------------------------------------------
    // Core authentication
    // -------------------------------------------------------------------------

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo,
                                      Subject clientSubject,
                                      Subject serviceSubject) throws AuthException {

        HttpServletRequest  request  = (HttpServletRequest)  messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        debugRequest(request);

        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        if (authorization != null && authorization.startsWith(NEGOTIATE)) {

            authorization = authorization.substring(NEGOTIATE.length() + 1);
            byte[] requestToken = Base64.decodeBase64(authorization.getBytes());

            try {
                GSSContext gssContext = gssManager.createContext((GSSCredential) null);
                byte[] gssToken = gssContext.acceptSecContext(requestToken, 0, requestToken.length);

                if (gssToken != null) {
                    byte[] responseToken = Base64.encodeBase64(gssToken);
                    response.setHeader(AUTHENTICATION_HEADER, NEGOTIATE + new String(responseToken));
                    debugToken("jmac.servlet.authentication.token", responseToken);
                }

                if (!gssContext.isEstablished()) {
                    if (debug || LOG.isLoggable(Level.FINE))
                        LOG.log(debugLevel, "jmac.gss_dialog_continued");
                    response.setStatus(401);
                    return AuthStatus.SEND_CONTINUE;
                }

                String mechOid;
                try {
                    Oid oid = gssContext.getMech();
                    mechOid = oid.toString();
                } catch (GSSException gsse) {
                    mechOid = "Undefined GSS Mechanism";
                    if (debug || LOG.isLoggable(Level.FINE))
                        LOG.log(debugLevel, "jmac.gss_mechanism_undefined", gsse);
                }

                GSSName name = gssContext.getSrcName();
                if (!setCallerPrincipal(name, clientSubject))
                    return sendFailureMessage(response, "Failed setting caller principal");

                messageInfo.getMap().put(AUTH_TYPE_INFO_KEY, mechOid);

                if (debug || LOG.isLoggable(Level.FINE))
                    LOG.log(debugLevel, "jmac.gss_dialog_complete");

            } catch (GSSException gsse) {
                LOG.severe("GSSException occurred. Major: " + gsse.getMajorString()
                        + ". Minor: " + gsse.getMinorString());
                if (requestToken != null) {
                    debugToken("jmac.servlet.authorization.token", requestToken);
                    if (isNTLMToken(requestToken))
                        return sendFailureMessage(response, "No support for NTLM");
                }
                if (debug || LOG.isLoggable(Level.FINE))
                    LOG.log(debugLevel, "jmac.gss_dialog_failed", gsse);
                AuthException ae = new AuthException();
                ae.initCause(gsse);
                throw ae;
            }

        } else {
            if (mandatory) {
                response.setHeader(AUTHENTICATION_HEADER, NEGOTIATE);
                response.setStatus(401);
                if (debug || LOG.isLoggable(Level.FINE))
                    LOG.log(debugLevel, "jmac.sevlet_header_added_to_response", NEGOTIATE);
                return AuthStatus.SEND_CONTINUE;
            }
            if (authorization != null)
                LOG.warning("jmac.servlet_authorization_header_ignored");
        }

        return AuthStatus.SUCCESS;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo,
                                     Subject serviceSubject) throws AuthException {
        LOG.fine("secureResponse called");
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        LOG.fine("cleanSubject called");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    AuthStatus sendFailureMessage(HttpServletResponse response, String message) {
        response.setStatus(403);
        return AuthStatus.SEND_FAILURE;
    }

    /**
     * Replaces the removed com.sun.security.jgss.GSSUtil.createSubject() call.
     * Extracts the Kerberos principal from the GSSName and fires the JASPIC callbacks.
     */
    private boolean setCallerPrincipal(GSSName name, Subject clientSubject) {
        try {
            Principal caller = new KerberosPrincipal(name.toString());
            clientSubject.getPrincipals().add(caller);

            CallerPrincipalCallback callerCB =
                    new CallerPrincipalCallback(clientSubject, caller);
            GroupPrincipalCallback groupCB =
                    new GroupPrincipalCallback(clientSubject, getGroupsForCaller(caller));

            handler.handle(new Callback[]{ callerCB, groupCB });

            if (debug || LOG.isLoggable(Level.FINE))
                LOG.log(debugLevel, "jmac.caller_principal", new Object[]{ caller });

            return true;

        } catch (GSSException | IOException | UnsupportedCallbackException e) {
            LOG.log(Level.WARNING, "jmac.failed_to_set_caller", e);
            return false;
        }
    }

    public abstract String[] getGroupsForCaller(Principal principal);

    boolean isNTLMToken(byte[] bytes) {
        return new String(bytes).startsWith(NTLM_INITIAL_TOKEN);
    }

    private void debugToken(String message, byte[] bytes) {
        if (debug || LOG.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append("Token ").append(Base64.isBase64(bytes) ? "is" : "is Not")
              .append(" Base64 encoded\n").append("bytes: ");
            boolean first = true;
            for (byte b : bytes) {
                if (first) { sb.append((int) b); first = false; }
                else       { sb.append(", ").append((int) b); }
            }
            LOG.log(debugLevel, message, sb);
        }
    }

    private void debugRequest(HttpServletRequest request) {
        if (debug || LOG.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder("\n");
            try {
                sb.append("Request: ").append(request.getRequestURL()).append("\n")
                  .append("UserPrincipal: ").append(request.getUserPrincipal()).append("\n")
                  .append("AuthType: ").append(request.getAuthType()).append("\n")
                  .append("Headers:\n");
                Enumeration<String> names = request.getHeaderNames();
                while (names.hasMoreElements()) {
                    String n = names.nextElement();
                    sb.append("\t").append(n).append("\t").append(request.getHeader(n)).append("\n");
                }
                LOG.log(debugLevel, "jmac.servlet_request", sb);
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "jmac.servlet_debug_request", t);
            }
        }
    }
}
