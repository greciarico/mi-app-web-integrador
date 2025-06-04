package com.example.DyD_Natures.Model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "merma")
public class Merma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_merma", nullable = false)
    private Integer idMerma;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "descripcion", length = 100)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false,
            foreignKey = @ForeignKey(name = "fk_merma_producto1"))
    private Producto producto;

    public Merma() {
    }

    public Integer getIdMerma() {
        return idMerma;
    }

    public void setIdMerma(Integer idMerma) {
        this.idMerma = idMerma;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}


