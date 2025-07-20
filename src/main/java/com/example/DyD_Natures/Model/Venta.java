package com.example.DyD_Natures.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore; // Importar JsonIgnore
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime; // Importar LocalDateTime

@Data
@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta", nullable = false)
    private Integer idVenta;

    @Column(name = "igv", nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

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

    @Column(name = "monto_efectivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoEfectivo;

    @Column(name = "monto_monedero_electronico", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoMonederoElectronico;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_turno_caja",
            foreignKey = @ForeignKey(name = "fk_venta_turno_caja"))
    @JsonIgnore
    private TurnoCaja turnoCaja;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DetalleVenta> detalleVentas = new ArrayList<>();

    @Column(name = "estado", nullable = true)
    private Byte estado = 1;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    public Venta() {
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
