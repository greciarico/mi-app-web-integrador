package com.example.DyD_Natures.Dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SunatDataDTO {

    @JsonProperty("ruc")
    private String ruc;
    @JsonProperty("razonSocial")
    private String razonSocial;
    @JsonProperty("nombreComercial")
    private String nombreComercial; // Puede ser null
    @JsonProperty("direccion")
    private String direccion;
    @JsonProperty("estado") // ACTIVO, BAJA PROVISIONAL, BAJA DEFINITIVA, etc.
    private String estado;
    @JsonProperty("condicion") // HABIDO, NO HABIDO
    private String condicion;

    // Getters y Setters
    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String condicion) {
        this.condicion = condicion;
    }

    @Override
    public String toString() {
        return "SunatDataDTO{" +
                "ruc='" + ruc + '\'' +
                ", razonSocial='" + razonSocial + '\'' +
                ", nombreComercial='" + nombreComercial + '\'' +
                ", direccion='" + direccion + '\'' +
                ", estado='" + estado + '\'' +
                ", condicion='" + condicion + '\'' +
                '}';
    }
}
