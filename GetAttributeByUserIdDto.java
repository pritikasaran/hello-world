package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * DTO for getAttributeByUserId.
 * attribute must be one of: UID, GIVENNAME, SURNAME, MAIL
 */
@XmlRootElement(name = "getAttributeByUserId")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAttributeByUserIdDto {

    private String userId;
    private String attribute;

    public GetAttributeByUserIdDto() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
}
