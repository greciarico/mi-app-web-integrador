package com.example.DyD_Natures.Dto;

import java.time.LocalDate;
import java.util.List; // Importar List

public class UsuarioFilterDTO {
    private String nombreApellidoDniCorreo; // Para búsqueda general en esos campos
    private List<Integer> idRoles; // Cambiado a List<Integer> para múltiples selecciones de rol
    private List<Integer> estados; // Cambiado a List<Integer> para múltiples selecciones de estado
    private LocalDate fechaRegistroStart; // Nuevo campo
    private LocalDate fechaRegistroEnd;   // Nuevo campo

    // Getters y Setters
    public String getNombreApellidoDniCorreo() {
        return nombreApellidoDniCorreo;
    }

    public void setNombreApellidoDniCorreo(String nombreApellidoDniCorreo) {
        this.nombreApellidoDniCorreo = nombreApellidoDniCorreo;
    }

    public List<Integer> getIdRoles() { // Cambiado el tipo de retorno y nombre
        return idRoles;
    }

    public void setIdRoles(List<Integer> idRoles) { // Cambiado el tipo de parámetro y nombre
        this.idRoles = idRoles;
    }

    public List<Integer> getEstados() { // Cambiado el tipo de retorno y nombre
        return estados;
    }

    public void setEstados(List<Integer> estados) { // Cambiado el tipo de parámetro y nombre
        this.estados = estados;
    }

    // Nuevos Getters y Setters para fechas
    public LocalDate getFechaRegistroStart() {
        return fechaRegistroStart;
    }

    public void setFechaRegistroStart(LocalDate fechaRegistroStart) {
        this.fechaRegistroStart = fechaRegistroStart;
    }

    public LocalDate getFechaRegistroEnd() {
        return fechaRegistroEnd;
    }

    public void setFechaRegistroEnd(LocalDate fechaRegistroEnd) {
        this.fechaRegistroEnd = fechaRegistroEnd;
    }

    @Override
    public String toString() {
        return "UsuarioFilterDTO{" +
                "nombreApellidoDniCorreo='" + nombreApellidoDniCorreo + '\'' +
                ", idRoles=" + idRoles +
                ", estados=" + estados +
                ", fechaRegistroStart=" + fechaRegistroStart +
                ", fechaRegistroEnd=" + fechaRegistroEnd +
                '}';
    }
}
