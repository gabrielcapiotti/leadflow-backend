package com.leadflow.backend.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateLeadRequest {

    @NotBlank
    @Size(max = 100)
    private String nomeCompleto;

    @NotBlank
    @Pattern(regexp = "^[0-9+\\-() ]{8,20}$")
    private String whatsapp;

    @Size(max = 50)
    private String tipoConsorcio;

    @Size(max = 50)
    private String valorCredito;

    @Pattern(regexp = "quero_fechar|analisando|pesquisando")
    private String urgencia;

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
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