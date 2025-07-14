package com.example.DyD_Natures.Dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignora campos que no estén definidos aquí
public class ReniecDataDTO {

    @JsonProperty("nombres")
    private String nombres;
    @JsonProperty("apellidoPaterno")
    private String apellidoPaterno;
    @JsonProperty("apellidoMaterno")
    private String apellidoMaterno;
    @JsonProperty("numeroDocumento")
    private String numeroDocumento;
    // APIS.NET.PE generalmente NO devuelve la dirección detallada para DNI en el endpoint /reniec
    // Si tu plan de API o un endpoint específico lo ofrece, lo añadirías aquí.
    // @JsonProperty("direccion")
    // private String direccion;

    // Puedes añadir un campo para el nombre completo si la API lo devuelve así
    // @JsonProperty("nombreCompleto")
    // private String nombreCompleto;

    // Getters y Setters
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

    // Si tuvieras direccion para DNI:
    /*
    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    */

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
