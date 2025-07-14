// src/main/java/com/example/DyD_Natures/Dto/VentaFilterDTO.java
package com.example.DyD_Natures.Dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class VentaFilterDTO {

    private String nombreCliente;
    private String tipoDocumento;
    private String numDocumento;
    private String tipoPago;
    private Integer idCliente; // Para filtrar por un cliente específico
    private Integer idUsuario; // Para filtrar por un usuario específico (vendedor)
    private List<Byte> estados; // 0=Cancelada, 1=Activa

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaRegistroStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaRegistroEnd;

    private BigDecimal totalMin;
    private BigDecimal totalMax;

    // Getters y Setters
    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
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

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
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

    public BigDecimal getTotalMin() {
        return totalMin;
    }

    public void setTotalMin(BigDecimal totalMin) {
        this.totalMin = totalMin;
    }

    public BigDecimal getTotalMax() {
        return totalMax;
    }

    public void setTotalMax(BigDecimal totalMax) {
        this.totalMax = totalMax;
    }

    // Método toString para depuración y para mostrar los filtros en el reporte
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (nombreCliente != null && !nombreCliente.isEmpty()) {
            sb.append("Cliente: '").append(nombreCliente).append("', ");
        }
        if (tipoDocumento != null && !tipoDocumento.isEmpty()) {
            sb.append("Tipo Doc.: '").append(tipoDocumento).append("', ");
        }
        if (numDocumento != null && !numDocumento.isEmpty()) {
            sb.append("N° Doc.: '").append(numDocumento).append("', ");
        }
        if (tipoPago != null && !tipoPago.isEmpty()) {
            sb.append("Tipo Pago: '").append(tipoPago).append("', ");
        }
        if (idCliente != null) {
            sb.append("ID Cliente: ").append(idCliente).append(", ");
        }
        if (idUsuario != null) {
            sb.append("ID Usuario: ").append(idUsuario).append(", ");
        }
        if (estados != null && !estados.isEmpty()) {
            String estadoStr = estados.stream()
                    .map(e -> e == 1 ? "Activa" : (e == 0 ? "Cancelada" : "Desconocido"))
                    .collect(Collectors.joining(", "));
            sb.append("Estado(s): ").append(estadoStr).append(", ");
        }
        if (fechaRegistroStart != null) {
            sb.append("Fecha Inicio: ").append(fechaRegistroStart).append(", ");
        }
        if (fechaRegistroEnd != null) {
            sb.append("Fecha Fin: ").append(fechaRegistroEnd).append(", ");
        }
        if (totalMin != null) {
            sb.append("Total Min: ").append(totalMin).append(", ");
        }
        if (totalMax != null) {
            sb.append("Total Max: ").append(totalMax).append(", ");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // Eliminar la última ', '
            return sb.toString();
        } else {
            return "Ninguno";
        }
    }
}
