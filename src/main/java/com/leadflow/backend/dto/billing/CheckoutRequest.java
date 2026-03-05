package com.leadflow.backend.dto.billing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 120) String nomeVendedor,
        @NotBlank @Size(min = 8, max = 20) String whatsappVendedor,
        @Size(max = 120) String nomeEmpresa,
        @Size(max = 80) String slug
) {
}
