package com.example.DyD_Natures.Dto;

import java.time.LocalDate;
import java.util.List;

public class UsuarioFilterDTO {
    private String nombreApellidoDniCorreo; 
    private List<Integer> idRoles; 
    private List<Integer> estados; 
    private LocalDate fechaRegistroStart; 
    private LocalDate fechaRegistroEnd;  


    public String getNombreApellidoDniCorreo() {
        return nombreApellidoDniCorreo;
    }

    public void setNombreApellidoDniCorreo(String nombreApellidoDniCorreo) {
        this.nombreApellidoDniCorreo = nombreApellidoDniCorreo;
    }

    public List<Integer> getIdRoles() { 
        return idRoles;
    }

    public void setIdRoles(List<Integer> idRoles) { 
        this.idRoles = idRoles;
    }

    public List<Integer> getEstados() { 
        return estados;
    }

    public void setEstados(List<Integer> estados) { 
        this.estados = estados;
    }

   
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
