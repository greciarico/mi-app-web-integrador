package com.example.DyD_Natures.Model;

import jakarta.persistence.*; // Usa jakarta.persistence para Spring Boot 3+
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turno_caja") // Asegúrate que el nombre de la tabla coincida exactamente con la BD (es 'turno_caja', no 'TurnosCaja')
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno_caja")
    private Integer idTurnoCaja;

    @ManyToOne // Un usuario puede tener muchos turnos de caja
    @JoinColumn(name = "id_vendedor", nullable = false) // Columna FK en TurnosCaja, que apunta a Usuario
    private Usuario vendedor; // Asume que tienes una entidad Usuario

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre; // Puede ser null

    @Column(name = "fondo_inicial_efectivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal fondoInicialEfectivo;

    @Column(name = "total_ventas_efectivo_sistema", precision = 10, scale = 2)
    private BigDecimal totalVentasEfectivoSistema; // Puede ser null inicialmente

    @Column(name = "total_ventas_monederoElectronico_sistema", precision = 10, scale = 2)
    private BigDecimal totalVentasMonederoElectronicoSistema; // Puede ser null inicialmente

    @Column(name = "conteo_final_efectivo", precision = 10, scale = 2)
    private BigDecimal conteoFinalEfectivo; // Puede ser null

    @Column(name = "diferencia_efectivo", precision = 10, scale = 2)
    private BigDecimal diferenciaEfectivo; // Puede ser null

    @Column(name = "estado_cuadre", nullable = false, length = 20)
    private String estadoCuadre; // Ej. 'Abierto', 'Cuadrado', 'Con Faltante', 'Con Sobrante'

    // *** ELIMINADA LA PROPIEDAD 'turnoCaja' QUE CAUSABA LA DUPLICACIÓN ***

    // Constructor vacío (necesario para JPA)
    public TurnoCaja() {
    }

    // Constructor para facilidad de creación
    public TurnoCaja(Usuario vendedor, BigDecimal fondoInicialEfectivo) {
        this.vendedor = vendedor;
        this.fechaApertura = LocalDateTime.now(); // Se establece al crear
        this.fondoInicialEfectivo = fondoInicialEfectivo;
        this.estadoCuadre = "Abierto"; // Estado inicial
    }

    // --- Getters y Setters ---
    // (Asegúrate de tener todos los getters y setters para los campos restantes)

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

    public BigDecimal getDiferenciaEfectivo() {
        return diferenciaEfectivo;
    }

    public void setDiferenciaEfectivo(BigDecimal diferenciaEfectivo) {
        this.diferenciaEfectivo = diferenciaEfectivo;
    }

    public String getEstadoCuadre() {
        return estadoCuadre;
    }

    public void setEstadoCuadre(String estadoCuadre) {
        this.estadoCuadre = estadoCuadre;
    }

    @Override
    public String toString() {
        return "TurnoCaja{" +
                "idTurnoCaja=" + idTurnoCaja +
                ", vendedorId=" + (vendedor != null ? vendedor.getIdUsuario() : "null") + // O vendedor.getNombre()
                ", fechaApertura=" + fechaApertura +
                ", fechaCierre=" + fechaCierre +
                ", fondoInicialEfectivo=" + fondoInicialEfectivo +
                ", totalVentasEfectivoSistema=" + totalVentasEfectivoSistema +
                ", totalVentasMonederoElectronicoSistema=" + totalVentasMonederoElectronicoSistema +
                ", conteoFinalEfectivo=" + conteoFinalEfectivo +
                ", diferenciaEfectivo=" + diferenciaEfectivo +
                ", estadoCuadre='" + estadoCuadre + '\'' +
                '}';
    }
}
