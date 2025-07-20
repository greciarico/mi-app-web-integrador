package com.example.DyD_Natures.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "turno_caja")
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno_caja")
    private Integer idTurnoCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "fondo_inicial_efectivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal fondoInicialEfectivo;

    @Column(name = "total_ventas_efectivo_sistema", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVentasEfectivoSistema;

    @Column(name = "total_ventas_monedero_electronico_sistema", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVentasMonederoElectronicoSistema;

    @Column(name = "conteo_final_efectivo", precision = 10, scale = 2)
    private BigDecimal conteoFinalEfectivo;

    // --- ¡ASEGÚRATE DE QUE ESTOS DOS CAMPOS Y SUS GETTERS/SETTERS ESTÉN PRESENTES! ---
    @Column(name = "conteo_final_monedero", precision = 10, scale = 2)
    private BigDecimal conteoFinalMonedero;

    @Column(name = "diferencia_efectivo", precision = 10, scale = 2)
    private BigDecimal diferenciaEfectivo;

    @Column(name = "diferencia_monedero", precision = 10, scale = 2)
    private BigDecimal diferenciaMonedero;
    // ---------------------------------------------------------------------------------

    @Column(name = "estado_cuadre", nullable = false, length = 50)
    private String estadoCuadre;

    public TurnoCaja() {
        this.fondoInicialEfectivo = BigDecimal.ZERO;
        this.totalVentasEfectivoSistema = BigDecimal.ZERO;
        this.totalVentasMonederoElectronicoSistema = BigDecimal.ZERO;
        this.conteoFinalEfectivo = BigDecimal.ZERO;
        this.conteoFinalMonedero = BigDecimal.ZERO; // <-- Importante inicializar
        this.diferenciaEfectivo = BigDecimal.ZERO;
        this.diferenciaMonedero = BigDecimal.ZERO; // <-- Importante inicializar
        this.estadoCuadre = "Abierto";
    }

    public TurnoCaja(Usuario vendedor, BigDecimal fondoInicialEfectivo) {
        this();
        this.vendedor = vendedor;
        this.fechaApertura = LocalDateTime.now();
        this.fondoInicialEfectivo = fondoInicialEfectivo;
    }

    // --- Getters y Setters ---

    public Integer getIdTurnoCaja() {
        return idTurnoCaja;
    }

    public void setIdTurnoCaja(Integer idTurnoCaja) {
        this.idTurnoCaja = idTurnoCaja;
    }

    public Usuario getVendedor() {
        return vendedor;
    }

    public void setVendedor(Usuario vendedor) {
        this.vendedor = vendedor;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getFondoInicialEfectivo() {
        return fondoInicialEfectivo;
    }

    public void setFondoInicialEfectivo(BigDecimal fondoInicialEfectivo) {
        this.fondoInicialEfectivo = fondoInicialEfectivo;
    }

    public BigDecimal getTotalVentasEfectivoSistema() {
        return totalVentasEfectivoSistema;
    }

    public void setTotalVentasEfectivoSistema(BigDecimal totalVentasEfectivoSistema) {
        this.totalVentasEfectivoSistema = totalVentasEfectivoSistema;
    }

    public BigDecimal getTotalVentasMonederoElectronicoSistema() {
        return totalVentasMonederoElectronicoSistema;
    }

    public void setTotalVentasMonederoElectronicoSistema(BigDecimal totalVentasMonederoElectronicoSistema) {
        this.totalVentasMonederoElectronicoSistema = totalVentasMonederoElectronicoSistema;
    }

    public BigDecimal getConteoFinalEfectivo() {
        return conteoFinalEfectivo;
    }

    public void setConteoFinalEfectivo(BigDecimal conteoFinalEfectivo) {
        this.conteoFinalEfectivo = conteoFinalEfectivo;
    }

    // --- ¡ASEGÚRATE DE QUE ESTOS DOS MÉTODOS ESTÉN PRESENTES! ---
    public BigDecimal getConteoFinalMonedero() {
        return conteoFinalMonedero;
    }

    public void setConteoFinalMonedero(BigDecimal conteoFinalMonedero) {
        this.conteoFinalMonedero = conteoFinalMonedero;
    }
    // ---------------------------------------------------------

    public BigDecimal getDiferenciaEfectivo() {
        return diferenciaEfectivo;
    }

    public void setDiferenciaEfectivo(BigDecimal diferenciaEfectivo) {
        this.diferenciaEfectivo = diferenciaEfectivo;
    }

    // --- ¡ASEGÚRATE DE QUE ESTOS DOS MÉTODOS ESTÉN PRESENTES! ---
    public BigDecimal getDiferenciaMonedero() {
        return diferenciaMonedero;
    }

    public void setDiferenciaMonedero(BigDecimal diferenciaMonedero) {
        this.diferenciaMonedero = diferenciaMonedero;
    }
    // ---------------------------------------------------------

    public String getEstadoCuadre() {
        return estadoCuadre;
    }

    public void setEstadoCuadre(String estadoCuadre) {
        this.estadoCuadre = estadoCuadre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TurnoCaja turnoCaja = (TurnoCaja) o;
        return Objects.equals(idTurnoCaja, turnoCaja.idTurnoCaja);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTurnoCaja);
    }

    @Override
    public String toString() {
        return "TurnoCaja{" +
                "idTurnoCaja=" + idTurnoCaja +
                ", vendedor=" + (vendedor != null ? vendedor.getNombre() : "N/A") +
                ", fechaApertura=" + fechaApertura +
                ", fechaCierre=" + fechaCierre +
                ", fondoInicialEfectivo=" + fondoInicialEfectivo +
                ", totalVentasEfectivoSistema=" + totalVentasEfectivoSistema +
                ", totalVentasMonederoElectronicoSistema=" + totalVentasMonederoElectronicoSistema +
                ", conteoFinalEfectivo=" + conteoFinalEfectivo +
                ", conteoFinalMonedero=" + conteoFinalMonedero + // Incluir en toString
                ", diferenciaEfectivo=" + diferenciaEfectivo +
                ", diferenciaMonedero=" + diferenciaMonedero + // Incluir en toString
                ", estadoCuadre='" + estadoCuadre + '\'' +
                '}';
    }
}
