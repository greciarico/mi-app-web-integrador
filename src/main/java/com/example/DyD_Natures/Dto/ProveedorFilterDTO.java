package com.example.DyD_Natures.Dto;

import org.springframework.format.annotation.DateTimeFormat; // Importar
import java.time.LocalDate; // Importar
import java.util.List;

public class ProveedorFilterDTO {
    private String nombreRucRazonSocialCorreo;
    private List<Byte> estados;

    @DateTimeFormat(pattern = "yyyy-MM-dd") // Para que Spring pueda mapear la fecha del formulario
    private LocalDate fechaRegistroDesde;

    @DateTimeFormat(pattern = "yyyy-MM-dd") // Para que Spring pueda mapear la fecha del formulario
    private LocalDate fechaRegistroHasta;

    // Constructor vacío
    public ProveedorFilterDTO() {
    }

    // Getters y Setters existentes

    public String getNombreRucRazonSocialCorreo() {
        return nombreRucRazonSocialCorreo;
    }

    public void setNombreRucRazonSocialCorreo(String nombreRucRazonSocialCorreo) {
        this.nombreRucRazonSocialCorreo = nombreRucRazonSocialCorreo;
    }

    public List<Byte> getEstados() {
        return estados;
    }

    public void setEstados(List<Byte> estados) {
        this.estados = estados;
    }

    // NUEVOS GETTERS Y SETTERS
    public LocalDate getFechaRegistroDesde() {
        return fechaRegistroDesde;
    }

    public void setFechaRegistroDesde(LocalDate fechaRegistroDesde) {
        this.fechaRegistroDesde = fechaRegistroDesde;
    }

    public LocalDate getFechaRegistroHasta() {
        return fechaRegistroHasta;
    }

    public void setFechaRegistroHasta(LocalDate fechaRegistroHasta) {
        this.fechaRegistroHasta = fechaRegistroHasta;
    }

    @Override
    public String toString() {
        return "ProveedorFilterDTO{" +
                "nombreRucRazonSocialCorreo='" + nombreRucRazonSocialCorreo + '\'' +
                ", estados=" + estados +
                ", fechaRegistroDesde=" + fechaRegistroDesde +
                ", fechaRegistroHasta=" + fechaRegistroHasta +
                '}';
    }
}
