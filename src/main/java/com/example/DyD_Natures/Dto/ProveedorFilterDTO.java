package com.example.DyD_Natures.Dto;

import org.springframework.format.annotation.DateTimeFormat; 
import java.time.LocalDate; 
import java.util.List;

public class ProveedorFilterDTO {
    private String nombreRucRazonSocialCorreo;
    private List<Byte> estados;

    @DateTimeFormat(pattern = "yyyy-MM-dd") 
    private LocalDate fechaRegistroDesde;

    @DateTimeFormat(pattern = "yyyy-MM-dd") 
    private LocalDate fechaRegistroHasta;


    public ProveedorFilterDTO() {
    }


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
