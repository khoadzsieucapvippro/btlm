package com.elearning.dto.response;

import com.elearning.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Public response DTO representing a system role.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleResponse {

    private Integer roleId;
    private String roleName;

    public RoleResponse() {
    }

    public RoleResponse(Integer roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    public static RoleResponse fromEntity(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleResponse(role.getRoleId(), role.getRoleName());
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
