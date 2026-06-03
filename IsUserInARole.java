package de.deltalloyd.partnerdb.rest.DTO.sts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Extends TokenDto with a roleNames array.
 * Used for isUserInARole — checks if user has at least one of the provided roles.
 */
@XmlRootElement(name = "isUserInARole")
@XmlAccessorType(XmlAccessType.FIELD)
public class IsUserInARole extends TokenDto {

    @XmlElement(name = "roleName")
    private String[] roleNames;

    public IsUserInARole() {}

    public String[] getRoleNames() { return roleNames; }
    public void setRoleNames(String[] roleNames) { this.roleNames = roleNames; }
}
