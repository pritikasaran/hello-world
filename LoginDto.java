package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "login")
@XmlAccessorType(XmlAccessType.FIELD)
public class LoginDto {

    private String userId;
    private String pw;

    public LoginDto() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }
}
