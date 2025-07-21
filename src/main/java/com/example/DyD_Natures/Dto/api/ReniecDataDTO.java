package com.example.DyD_Natures.Dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) 
public class ReniecDataDTO {

    @JsonProperty("nombres")
    private String nombres;
    @JsonProperty("apellidoPaterno")
    private String apellidoPaterno;
    @JsonProperty("apellidoMaterno")
    private String apellidoMaterno;
    @JsonProperty("numeroDocumento")
    private String numeroDocumento;

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }


    @Override
    public String toString() {
        return "ReniecDataDTO{" +
                "nombres='" + nombres + '\'' +
                ", apellidoPaterno='" + apellidoPaterno + '\'' +
                ", apellidoMaterno='" + apellidoMaterno + '\'' +
                ", numeroDocumento='" + numeroDocumento + '\'' +
                '}';
    }
}
