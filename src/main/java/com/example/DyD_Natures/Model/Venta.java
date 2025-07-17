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

@Data // Lombok genera getters, setters, equals, hashCode y toString
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

    // --- NUEVO CAMPO: Relación con TurnoCaja ---
    @ManyToOne(fetch = FetchType.LAZY) // Usar LAZY para evitar cargar el turno si no es necesario
    @JoinColumn(name = "id_turno_caja", // Nombre de la columna FK en la tabla 'venta'
            foreignKey = @ForeignKey(name = "fk_venta_turno_caja")) // Nombre de la clave foránea
    @JsonIgnore // <--- ¡IMPORTANTE! AÑADIR ESTA ANOTACIÓN PARA EVITAR ERRORES DE SERIALIZACIÓN
    private TurnoCaja turnoCaja; // Objeto TurnoCaja asociado a esta venta

    // --- RELACIÓN CON DETALLES ---
    // @JsonManagedReference evita problemas de bucles infinitos al convertir a JSON.
    // FetchType.LAZY es una buena práctica para mejorar el rendimiento.
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DetalleVenta> detalleVentas = new ArrayList<>();

    @Column(name = "estado", nullable = true)
    private Byte estado = 1;

    // Nuevos campos para auditoría de anulación
    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion; // Fecha y hora exacta de la anulación

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion; // Usuario que realizó la anulación
    // --- Fin nuevos campos ---

    public Venta() {
    }

    // El resto de getters y setters son generados por Lombok (@Data).
    // Solo se mantiene el setter de detalleVentas si tiene lógica personalizada.
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
