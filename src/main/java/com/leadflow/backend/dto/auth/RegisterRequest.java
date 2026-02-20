package com.leadflow.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        @JsonProperty("name")
        String name,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @JsonProperty("email")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
        @JsonProperty("password")
        String password

) {

    @JsonCreator
    public RegisterRequest {

        if (name != null) {
            name = name.trim();
        }

        if (email != null) {
            email = email.trim().toLowerCase();
        }

        if (password != null) {
            password = password.trim();
        }
    }
}