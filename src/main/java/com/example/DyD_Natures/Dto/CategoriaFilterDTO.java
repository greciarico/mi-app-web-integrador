package com.example.DyD_Natures.Dto;

import java.util.List;

public class CategoriaFilterDTO {
    private String nombreCategoria; // Para buscar por nombre
    private List<Byte> estados;    // Para filtrar por estados (Activo, Inactivo)

    // Constructor vacío
    public CategoriaFilterDTO() {
    }

    // Getters y Setters
    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public List<Byte> getEstados() {
        return estados;
    }

    public void setEstados(List<Byte> estados) {
        this.estados = estados;
    }

    @Override
    public String toString() {
        return "CategoriaFilterDTO{" +
                "nombreCategoria='" + nombreCategoria + '\'' +
                ", estados=" + estados +
                '}';
    }
}
