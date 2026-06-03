package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Extends TokenDto with an attribute field.
 * Used for getAttribute — attribute must be one of: UID, GIVENNAME, SURNAME, MAIL
 */
@XmlRootElement(name = "getAttribute")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAttributeDto extends TokenDto {

    private String attribute;

    public GetAttributeDto() {}

    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
}
