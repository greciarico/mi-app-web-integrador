package com.example.DyD_Natures.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "igv")
public class Igv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_igv", nullable = false)
    private Integer idIgv;

    @Column(name = "tasa", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasa;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "estado", nullable = false)
    private Byte estado;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    public Igv() {
    }

    public Integer getIdIgv() {
        return idIgv;
    }

    public void setIdIgv(Integer idIgv) {
        this.idIgv = idIgv;
    }

    public BigDecimal getTasa() {
        return tasa;
    }

    public void setTasa(BigDecimal tasa) {
        this.tasa = tasa;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Byte getEstado() {
        return estado;
    }

    public void setEstado(Byte estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}


