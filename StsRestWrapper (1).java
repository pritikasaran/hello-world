package de.deltalloyd.partnerdb.rest.restWrapper;

import de.deltalloyd.partnerdb.rest.DTO.sts.GetAttributeByUserIdDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.GetAttributeDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.IsUserInARole;
import de.deltalloyd.partnerdb.rest.DTO.sts.IsUserInRoleDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.LoginDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.LoginWithTimeoutDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.TokenDto;
import de.deltalloyd.sst.sts.base.SecurityTokenService;
import de.deltalloyd.sst.sts.base.SecurityTokenService.UserAttribute;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/sts")
@Produces(MediaType.APPLICATION_XML)
@Consumes(MediaType.APPLICATION_XML)
public class StsRestWrapper {

    // NOTE: SecurityTokenService is a separate app — inject via your existing
    // client mechanism (e.g. @WebServiceRef or lookup) rather than @Inject.
    // Replace this field with however the partner app currently accesses STS.
    @jakarta.ejb.EJB
    SecurityTokenService securityTokenService;

    /**
     * Login — returns session token.
     * POST /sts/login
     */
    @POST
    @Path("/login")
    public String login(LoginDto dto) throws Exception {
        return securityTokenService.login(
                dto.getUserId(),
                dto.getPw());
    }

    /**
     * Login with custom session timeout — returns session token.
     * POST /sts/login/timeout
     * LoginWithTimeoutDto extends LoginDto, adds sessionTimeoutSeconds.
     */
    @POST
    @Path("/login/timeout")
    public String loginWithSessionTimeout(LoginWithTimeoutDto dto) throws Exception {
        return securityTokenService.loginWithSessionTimeout(
                dto.getUserId(),
                dto.getPw(),
                dto.getSessionTimeoutSeconds());
    }

    /**
     * Logout — invalidates the token.
     * POST /sts/logout
     * Uses TokenDto (authToken only).
     */
    @POST
    @Path("/logout")
    public void logout(TokenDto dto) throws Exception {
        securityTokenService.logout(dto.getAuthToken());
    }

    /**
     * Get remaining token lifetime in seconds. Does not extend token.
     * POST /sts/token/lifetime
     * Uses TokenDto (authToken only).
     */
    @POST
    @Path("/token/lifetime")
    public long getRestLifeTimeSeconds(TokenDto dto) {
        return securityTokenService.getRestLifeTimeSeconds(dto.getAuthToken());
    }

    /**
     * Check if token is still valid.
     * POST /sts/token/validate
     * Uses TokenDto (authToken only).
     */
    @POST
    @Path("/token/validate")
    public boolean isValid(TokenDto dto) throws Exception {
        return securityTokenService.isValid(dto.getAuthToken());
    }

    /**
     * Get user ID for a token.
     * POST /sts/user/id
     * Uses TokenDto (authToken only).
     */
    @POST
    @Path("/user/id")
    public String getUserID(TokenDto dto) throws Exception {
        return securityTokenService.getUserID(dto.getAuthToken());
    }

    /**
     * Get a user attribute by token.
     * POST /sts/user/attribute
     * GetAttributeDto extends TokenDto, adds attribute (UID/GIVENNAME/SURNAME/MAIL).
     */
    @POST
    @Path("/user/attribute")
    public String getAttribute(GetAttributeDto dto) throws Exception {
        return securityTokenService.getAttribute(
                dto.getAuthToken(),
                UserAttribute.valueOf(dto.getAttribute()));
    }

    /**
     * Get a user attribute by userId — no token needed.
     * POST /sts/user/attribute/by-userid
     */
    @POST
    @Path("/user/attribute/by-userid")
    public String getAttributeByUserId(GetAttributeByUserIdDto dto) throws Exception {
        return securityTokenService.getAttributeByUserId(
                dto.getUserId(),
                UserAttribute.valueOf(dto.getAttribute()));
    }

    /**
     * Check if user has a specific role.
     * POST /sts/user/role/check
     * IsUserInRoleDto extends TokenDto, adds roleName.
     */
    @POST
    @Path("/user/role/check")
    public boolean isUserInRole(IsUserInRoleDto dto) throws Exception {
        return securityTokenService.isUserInRole(
                dto.getAuthToken(),
                dto.getRoleName());
    }

    /**
     * Check if user has at least one of the provided roles.
     * POST /sts/user/role/check-any
     * IsUserInARole extends TokenDto, adds roleNames[].
     */
    @POST
    @Path("/user/role/check-any")
    public boolean isUserInARole(IsUserInARole dto) throws Exception {
        return securityTokenService.isUserInARole(
                dto.getAuthToken(),
                dto.getRoleNames());
    }
}
