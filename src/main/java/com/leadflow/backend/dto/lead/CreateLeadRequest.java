package com.leadflow.backend.dto.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateLeadRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    private String email;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Pattern(
        regexp = "^[0-9+()\\-\\s]*$",
        message = "Telefone contém caracteres inválidos"
    )
    private String phone;

    @Size(max = 50, message = "Tipo de consórcio deve ter no máximo 50 caracteres")
    private String tipoConsorcio;

    @Size(max = 50, message = "Valor de crédito deve ter no máximo 50 caracteres")
    private String valorCredito;

    @Pattern(regexp = "quero_fechar|analisando|pesquisando", message = "Urgência inválida")
    private String urgencia;

    /* ======================================================
       CONSTRUTORES
       ====================================================== */

    // ✅ Necessário para Jackson
    public CreateLeadRequest() {
    }

    public CreateLeadRequest(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /* ======================================================
       GETTERS & SETTERS
       ====================================================== */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTipoConsorcio() {
        return tipoConsorcio;
    }

    public void setTipoConsorcio(String tipoConsorcio) {
        this.tipoConsorcio = tipoConsorcio;
    }

    public String getValorCredito() {
        return valorCredito;
    }

    public void setValorCredito(String valorCredito) {
        this.valorCredito = valorCredito;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }
}
