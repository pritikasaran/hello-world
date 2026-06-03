package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Shared DTO for operations that only need an auth token:
 * logout, getRestLifeTimeSeconds, isValid, getUserID
 */
@XmlRootElement(name = "token")
@XmlAccessorType(XmlAccessType.FIELD)
public class TokenDto {

    private String authToken;

    public TokenDto() {}

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }
}
