package de.deltalloyd.partnerdb.rest.restWrapper;

import de.deltalloyd.partnerdb.rest.DTO.sts.GetAttributeByUserIdDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.GetAttributeDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.IsAllowedDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.IsUserInARole;
import de.deltalloyd.partnerdb.rest.DTO.sts.IsUserInRoleDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.LoginDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.LoginWithTimeoutDto;
import de.deltalloyd.partnerdb.rest.DTO.sts.TokenDto;
import de.deltalloyd.sst.sts.base.SecurityTokenService;
import de.deltalloyd.sst.sts.base.SecurityTokenService.UserAttribute;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
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

    @Inject
    SecurityTokenService securityTokenService;

    /**
     * Login a user and return a session token.
     *
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
     * Login a user with a custom session timeout and return a session token.
     *
     * POST /sts/login/timeout
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
     * Logout a user and invalidate the session token.
     *
     * POST /sts/logout
     */
    @POST
    @Path("/logout")
    public void logout(TokenDto dto) throws Exception {
        securityTokenService.logout(dto.getAuthToken());
    }

    /**
     * Get remaining lifetime of a token in seconds.
     * Returns 0 if token is invalid. Does not extend token lifetime.
     *
     * POST /sts/token/lifetime
     */
    @POST
    @Path("/token/lifetime")
    public long getRestLifeTimeSeconds(TokenDto dto) {
        return securityTokenService.getRestLifeTimeSeconds(dto.getAuthToken());
    }

    /**
     * Check if a token is still valid.
     *
     * POST /sts/token/validate
     */
    @POST
    @Path("/token/validate")
    public boolean isValid(TokenDto dto) throws Exception {
        return securityTokenService.isValid(dto.getAuthToken());
    }

    /**
     * Get the user ID associated with a token.
     *
     * POST /sts/user/id
     */
    @POST
    @Path("/user/id")
    public String getUserID(TokenDto dto) throws Exception {
        return securityTokenService.getUserID(dto.getAuthToken());
    }

    /**
     * Get a specific attribute of the user associated with a token.
     *
     * POST /sts/user/attribute
     */
    @POST
    @Path("/user/attribute")
    public String getAttribute(GetAttributeDto dto) throws Exception {
        return securityTokenService.getAttribute(
                dto.getAuthToken(),
                UserAttribute.valueOf(dto.getAttribute()));
    }

    /**
     * Get a specific attribute of a user by userId (no token required).
     *
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
     * Check if a user has a specific role.
     *
     * POST /sts/user/role/check
     */
    @POST
    @Path("/user/role/check")
    public boolean isUserInRole(IsUserInRoleDto dto) throws Exception {
        return securityTokenService.isUserInRole(
                dto.getAuthToken(),
                dto.getRoleName());
    }

    /**
     * Check if a user has at least one of the provided roles.
     *
     * POST /sts/user/role/check-any
     */
    @POST
    @Path("/user/role/check-any")
    public boolean isUserInARole(IsUserInARole dto) throws Exception {
        return securityTokenService.isUserInARole(
                dto.getAuthToken(),
                dto.getRoleNames());
    }
}
