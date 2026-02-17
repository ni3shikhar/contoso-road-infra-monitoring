package com.contoso.roadinfra.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 50, message = "Persona must not exceed 50 characters")
    private String persona;

    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
}
