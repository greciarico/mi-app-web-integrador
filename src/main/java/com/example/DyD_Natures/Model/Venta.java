package com.example.DyD_Natures.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference; // Importar JsonManagedReference
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

    @Column(name = "igv", nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_igv", nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_igv"))
    private Igv igvEntity;

    // AHORA SÍ: @JsonManagedReference para la colección de detalles en Venta.
    // También cambiamos a FetchType.LAZY para mejor rendimiento y evitar StackOverflowError.
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DetalleVenta> detalleVentas = new ArrayList<>();

    public Venta() {
    }

    // Getters y setters

    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNumDocumento() { return numDocumento; }
    public void setNumDocumento(String numDocumento) { this.numDocumento = numDocumento; }
    public String getTipoPago() { return tipoPago; }
    public void setTipoPago(String tipoPago) { this.tipoPago = tipoPago; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }
    public Igv getIgvEntity() { return igvEntity; }
    public void setIgvEntity(Igv igvEntity) { this.igvEntity = igvEntity; }

    // Importante: Jackson necesita un setter para deserializar la lista.
    // También es buena práctica manejar nulos y asegurar que los detalles tengan la venta padre.
    public List<DetalleVenta> getDetalleVentas() {
        return detalleVentas;
    }

    public void setDetalleVentas(List<DetalleVenta> detalleVentas) {
        // Limpiar la lista existente antes de añadir los nuevos,
        // para que JPA maneje correctamente los orphanRemoval (si aplica)
        if (this.detalleVentas == null) {
            this.detalleVentas = new ArrayList<>();
        } else {
            this.detalleVentas.clear(); // Limpiar solo si ya existe
        }
        if (detalleVentas != null) {
            for (DetalleVenta detalle : detalleVentas) {
                detalle.setVenta(this); // Asegurar que cada detalle tenga la referencia a esta venta
                this.detalleVentas.add(detalle);
            }
        }
    }


    // Métodos para agregar/eliminar detalles individualmente (buenas prácticas)
    public void addDetalleVenta(DetalleVenta detalle) {
        if (this.detalleVentas == null) {
            this.detalleVentas = new ArrayList<>();
        }
        if (!this.detalleVentas.contains(detalle)) { // Evitar duplicados si equals/hashCode están bien
            this.detalleVentas.add(detalle);
            detalle.setVenta(this);
        }
    }

    public void removeDetalleVenta(DetalleVenta detalle) {
        if (this.detalleVentas != null && this.detalleVentas.remove(detalle)) {
            detalle.setVenta(null); // Desvincular para evitar problemas de persistencia
        }
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
