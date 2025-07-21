package com.example.DyD_Natures.Model;

import com.fasterxml.jackson.annotation.JsonBackReference; 
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta", nullable = false)
    private Integer idDetalleVenta;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "id_venta", nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_venta_venta1"))
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "id_producto", nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_venta_producto1"))
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "estado", nullable = true)
    private Byte estado = 1;

    public Byte getEstado() {
        return estado;
    }

    public void setEstado(Byte estado) {
        this.estado = estado;
    }

    public DetalleVenta() {
    }

    public DetalleVenta(Integer idDetalleVenta, Venta venta, Producto producto, Integer cantidad, BigDecimal precioUnitario, BigDecimal total) {
        this.idDetalleVenta = idDetalleVenta;
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.total = total;
    }



    public Integer getIdDetalleVenta() { return idDetalleVenta; }
    public void setIdDetalleVenta(Integer idDetalleVenta) { this.idDetalleVenta = idDetalleVenta; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleVenta that = (DetalleVenta) o;
        return Objects.equals(idDetalleVenta, that.idDetalleVenta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDetalleVenta);
    }
}
