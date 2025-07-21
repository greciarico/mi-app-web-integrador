package com.example.DyD_Natures.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ProductoFilterDTO {
    private String nombre;
    private String descripcion;
    private Integer idCategoria;
    private BigDecimal precio1Min;
    private BigDecimal precio1Max;
    private BigDecimal precio2Min;
    private BigDecimal precio2Max;
    private Integer stockMin;
    private Integer stockMax;
    private List<Byte> estados;
    private LocalDate fechaRegistroStart;
    private LocalDate fechaRegistroEnd;

    public ProductoFilterDTO() {
    }


    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public BigDecimal getPrecio1Min() {
        return precio1Min;
    }

    public void setPrecio1Min(BigDecimal precio1Min) {
        this.precio1Min = precio1Min;
    }

    public BigDecimal getPrecio1Max() {
        return precio1Max;
    }

    public void setPrecio1Max(BigDecimal precio1Max) {
        this.precio1Max = precio1Max;
    }

    public BigDecimal getPrecio2Min() {
        return precio2Min;
    }

    public void setPrecio2Min(BigDecimal precio2Min) {
        this.precio2Min = precio2Min;
    }

    public BigDecimal getPrecio2Max() {
        return precio2Max;
    }

    public void setPrecio2Max(BigDecimal precio2Max) {
        this.precio2Max = precio2Max;
    }

    public Integer getStockMin() {
        return stockMin;
    }

    public void setStockMin(Integer stockMin) {
        this.stockMin = stockMin;
    }

    public Integer getStockMax() {
        return stockMax;
    }

    public void setStockMax(Integer stockMax) {
        this.stockMax = stockMax;
    }

    public List<Byte> getEstados() {
        return estados;
    }

    public void setEstados(List<Byte> estados) {
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
        return "ProductoFilterDTO{" +
                "nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", idCategoria=" + idCategoria +
                ", precio1Min=" + precio1Min +
                ", precio1Max=" + precio1Max +
                ", precio2Min=" + precio2Min +
                ", precio2Max=" + precio2Max +
                ", stockMin=" + stockMin +
                ", stockMax=" + stockMax +
                ", estados=" + estados +
                ", fechaRegistroStart=" + fechaRegistroStart +
                ", fechaRegistroEnd=" + fechaRegistroEnd +
                '}';
    }
}
