package com.example.DyD_Natures.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta", nullable = false)
    private Integer idVenta;

    // --- CAMPO PARA LA BASE DE DATOS ---
    // Este es el campo que SÍ se guarda en la base de datos, en la columna "igv".
    // Almacenará el MONTO del IGV (ej: 18.00).
    @Column(name = "igv", nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    // --- CAMPO TRANSITORIO PARA EL FORMULARIO/JSON ---
    // Este campo NO se guarda en la BD. Solo existe para recibir el dato 'tasa' del JSON que envía el frontend.
    @Transient
    private BigDecimal tasa;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_cliente1"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_usuario"))
    private Usuario usuario;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    private String tipoDocumento;

    @Column(name = "num_documento", nullable = false, length = 45)
    private String numDocumento;

    @Column(name = "tipo_pago", nullable = false, length = 100)
    private String tipoPago;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_igv", nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_igv"))
    private Igv igvEntity;

    // --- RELACIÓN CON DETALLES ---
    // @JsonManagedReference evita problemas de bucles infinitos al convertir a JSON.
    // FetchType.LAZY es una buena práctica para mejorar el rendimiento.
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DetalleVenta> detalleVentas = new ArrayList<>();

    @Column(name = "estado", nullable = true)
    private Byte estado = 1;

    public Venta() {
    }

    // --- Getters y Setters ---

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
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

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumDocumento() {
        return numDocumento;
    }

    public void setNumDocumento(String numDocumento) {
        this.numDocumento = numDocumento;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public Igv getIgvEntity() {
        return igvEntity;
    }

    public void setIgvEntity(Igv igvEntity) {
        this.igvEntity = igvEntity;
    }

    public List<DetalleVenta> getDetalleVentas() {
        return detalleVentas;
    }

    public void setDetalleVentas(List<DetalleVenta> detalleVentas) {
        if (this.detalleVentas == null) {
            this.detalleVentas = new ArrayList<>();
        } else {
            this.detalleVentas.clear();
        }
        if (detalleVentas != null) {
            for (DetalleVenta detalle : detalleVentas) {
                detalle.setVenta(this);
                this.detalleVentas.add(detalle);
            }
        }
    }

    public Byte getEstado() {
        return estado;
    }

    public void setEstado(Byte estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venta venta = (Venta) o;
        return Objects.equals(idVenta, venta.idVenta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVenta);
    }
}
