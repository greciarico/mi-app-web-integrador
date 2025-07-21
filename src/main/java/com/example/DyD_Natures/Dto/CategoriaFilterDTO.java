package com.example.DyD_Natures.Dto;

import java.util.List;

public class CategoriaFilterDTO {
    private String nombreCategoria; 
    private List<Byte> estados;    

   
    public CategoriaFilterDTO() {
    }

   
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
