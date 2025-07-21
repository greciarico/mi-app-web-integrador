package com.example.DyD_Natures.Dto;

import java.time.LocalDate;

public class MermaFilterDTO {
    private String nombreProducto; 
    private String descripcionMerma;
    private Integer idProducto;     
    private LocalDate fechaRegistroStart;
    private LocalDate fechaRegistroEnd;
    private Integer cantidadMin;
    private Integer cantidadMax;

    public MermaFilterDTO() {
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getDescripcionMerma() {
        return descripcionMerma;
    }

    public void setDescripcionMerma(String descripcionMerma) {
        this.descripcionMerma = descripcionMerma;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
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

    public Integer getCantidadMin() {
        return cantidadMin;
    }

    public void setCantidadMin(Integer cantidadMin) {
        this.cantidadMin = cantidadMin;
    }

    public Integer getCantidadMax() {
        return cantidadMax;
    }

    public void setCantidadMax(Integer cantidadMax) {
        this.cantidadMax = cantidadMax;
    }

    @Override
    public String toString() {
        return "MermaFilterDTO{" +
                "nombreProducto='" + nombreProducto + '\'' +
                "descripcionMerma='" + descripcionMerma + '\'' +
                ", idProducto=" + idProducto +
                ", fechaRegistroStart=" + fechaRegistroStart +
                ", fechaRegistroEnd=" + fechaRegistroEnd +
                ", cantidadMin=" + cantidadMin +
                ", cantidadMax=" + cantidadMax +
                '}';
    }
}
