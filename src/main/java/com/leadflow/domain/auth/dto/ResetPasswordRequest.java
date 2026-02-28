package com.leadflow.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * DTO responsável por receber a requisição de redefinição de senha.
 * Contém token de validação e nova senha.
 */
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 150, message = "Token must have at most 150 characters")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must contain between 8 and 100 characters")
    private String newPassword;

    public ResetPasswordRequest() {
        // Necessário para desserialização do Jackson
    }

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ResetPasswordRequest{" +
                "token='[PROTECTED]'" +
                ", newPassword='[PROTECTED]'" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResetPasswordRequest)) return false;
        ResetPasswordRequest that = (ResetPasswordRequest) o;
        return Objects.equals(token, that.token) &&
               Objects.equals(newPassword, that.newPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, newPassword);
    }
}