package com.example.DyD_Natures.Dto;

import java.time.LocalDate;

public class ClienteFilterDTO {
    private String nombreCompletoODoc;
    private Integer idTipoCliente;
    private LocalDate fechaRegistroStart;
    private LocalDate fechaRegistroEnd;
    private String direccion;
    private String telefono;
    private Byte estado; // NUEVO: Campo para filtrar por estado (1=Activo, 2=Inactivo)

    // Constructor vacío
    public ClienteFilterDTO() {
    }

    // Getters y Setters (Asegúrate de tener los nuevos getters y setters para 'estado')
    public String getNombreCompletoODoc() {
        return nombreCompletoODoc;
    }

    public void setNombreCompletoODoc(String nombreCompletoODoc) {
        this.nombreCompletoODoc = nombreCompletoODoc;
    }

    public Integer getIdTipoCliente() {
        return idTipoCliente;
    }

    public void setIdTipoCliente(Integer idTipoCliente) {
        this.idTipoCliente = idTipoCliente;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    // NUEVO: Getter y Setter para 'estado'
    public Byte getEstado() {
        return estado;
    }

    public void setEstado(Byte estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "ClienteFilterDTO{" +
                "nombreCompletoODoc='" + nombreCompletoODoc + '\'' +
                ", idTipoCliente=" + idTipoCliente +
                ", fechaRegistroStart=" + fechaRegistroStart +
                ", fechaRegistroEnd=" + fechaRegistroEnd +
                ", direccion='" + direccion + '\'' +
                ", telefono='" + telefono + '\'' +
                ", estado=" + estado + // Incluir en toString
                '}';
    }
}
