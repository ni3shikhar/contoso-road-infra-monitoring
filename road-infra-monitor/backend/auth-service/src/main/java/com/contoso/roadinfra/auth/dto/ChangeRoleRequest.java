package com.contoso.roadinfra.auth.dto;

import com.contoso.roadinfra.common.constants.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

    private String persona;
}
