package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Extends TokenDto with a single roleName.
 * Used for isUserInRole.
 */
@XmlRootElement(name = "isUserInRole")
@XmlAccessorType(XmlAccessType.FIELD)
public class IsUserInRoleDto extends TokenDto {

    private String roleName;

    public IsUserInRoleDto() {}

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
