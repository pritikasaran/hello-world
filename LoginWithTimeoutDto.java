package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Extends LoginDto with an optional session timeout.
 * Used for both /sts/login (sessionTimeoutSeconds ignored)
 * and /sts/login/timeout (sessionTimeoutSeconds required).
 */
@XmlRootElement(name = "loginWithTimeout")
@XmlAccessorType(XmlAccessType.FIELD)
public class LoginWithTimeoutDto extends LoginDto {

    private long sessionTimeoutSeconds;

    public LoginWithTimeoutDto() {}

    public long getSessionTimeoutSeconds() { return sessionTimeoutSeconds; }
    public void setSessionTimeoutSeconds(long sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }
}
