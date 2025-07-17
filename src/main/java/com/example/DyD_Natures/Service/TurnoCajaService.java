package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.TurnoCaja;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Model.Venta;
import com.example.DyD_Natures.Repository.TurnoCajaRepository;
import com.example.DyD_Natures.Repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
public class TurnoCajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;

    @Autowired
    public TurnoCajaService(TurnoCajaRepository turnoCajaRepository, VentaRepository ventaRepository) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaRepository = ventaRepository;
    }

    @Transactional
    public TurnoCaja abrirTurnoCaja(Usuario vendedor, BigDecimal fondoInicialEfectivo) {
        Optional<TurnoCaja> existingTurno = turnoCajaRepository.findByVendedorAndEstadoCuadre(vendedor, "Abierto");
        if (existingTurno.isPresent()) {
            throw new IllegalStateException("El vendedor " + vendedor.getNombre() + " ya tiene un turno de caja abierto.");
        }

        TurnoCaja nuevoTurno = new TurnoCaja(vendedor, fondoInicialEfectivo);
        nuevoTurno.setTotalVentasEfectivoSistema(BigDecimal.ZERO);
        nuevoTurno.setTotalVentasMonederoElectronicoSistema(BigDecimal.ZERO);
        return turnoCajaRepository.save(nuevoTurno);
    }

    @Transactional(readOnly = true)
    public Optional<TurnoCaja> getTurnoCajaAbierto(Usuario vendedor) {
        Optional<TurnoCaja> turnoOpt = turnoCajaRepository.findByVendedorAndEstadoCuadre(vendedor, "Abierto");
        if (turnoOpt.isPresent()) {
            TurnoCaja turno = turnoOpt.get();
            List<Venta> ventasDelTurno = ventaRepository.findByTurnoCaja(turno);

            BigDecimal totalEfectivoVentas = BigDecimal.ZERO;
            BigDecimal totalMonederoElectronicoVentas = BigDecimal.ZERO;

            for (Venta venta : ventasDelTurno) {
                if ("EFECTIVO".equalsIgnoreCase(venta.getTipoPago()) && venta.getEstado() != null && venta.getEstado() == 1) {
                    totalEfectivoVentas = totalEfectivoVentas.add(venta.getTotal());
                } else if (("MONEDERO_ELECTRONICO".equalsIgnoreCase(venta.getTipoPago()) || "YAPE".equalsIgnoreCase(venta.getTipoPago())) && venta.getEstado() != null && venta.getEstado() == 1) {
                    totalMonederoElectronicoVentas = totalMonederoElectronicoVentas.add(venta.getTotal());
                }
            }
            turno.setTotalVentasEfectivoSistema(totalEfectivoVentas);
            turno.setTotalVentasMonederoElectronicoSistema(totalMonederoElectronicoVentas);
        }
        return turnoOpt;
    }

    @Transactional
    public TurnoCaja cerrarYCuadrarTurnoCaja(Integer idTurnoCaja, BigDecimal conteoFinalEfectivo) {
        TurnoCaja turno = turnoCajaRepository.findById(idTurnoCaja)
                .orElseThrow(() -> new RuntimeException("Turno de caja no encontrado con ID: " + idTurnoCaja));

        if (!"Abierto".equals(turno.getEstadoCuadre())) {
            throw new IllegalStateException("El turno de caja no está abierto para cuadrar.");
        }

        List<Venta> ventasDelTurno = ventaRepository.findByTurnoCaja(turno);

        BigDecimal totalEfectivoVentas = BigDecimal.ZERO;
        BigDecimal totalMonederoElectronicoVentas = BigDecimal.ZERO;

        for (Venta venta : ventasDelTurno) {
            if ("EFECTIVO".equalsIgnoreCase(venta.getTipoPago()) && venta.getEstado() != null && venta.getEstado() == 1) {
                totalEfectivoVentas = totalEfectivoVentas.add(venta.getTotal());
            } else if (("MONEDERO_ELECTRONICO".equalsIgnoreCase(venta.getTipoPago()) || "YAPE".equalsIgnoreCase(venta.getTipoPago())) && venta.getEstado() != null && venta.getEstado() == 1) {
                totalMonederoElectronicoVentas = totalMonederoElectronicoVentas.add(venta.getTotal());
            }
        }

        turno.setTotalVentasEfectivoSistema(totalEfectivoVentas);
        turno.setTotalVentasMonederoElectronicoSistema(totalMonederoElectronicoVentas);

        BigDecimal efectivoEsperado = turno.getFondoInicialEfectivo().add(totalEfectivoVentas).add(totalMonederoElectronicoVentas);

        turno.setConteoFinalEfectivo(conteoFinalEfectivo);
        BigDecimal diferencia = conteoFinalEfectivo.subtract(efectivoEsperado);
        turno.setDiferenciaEfectivo(diferencia);

        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            turno.setEstadoCuadre("Cuadrado");
        } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
            turno.setEstadoCuadre("Con Faltante");
        } else {
            turno.setEstadoCuadre("Con Sobrante");
        }

        turno.setFechaCierre(LocalDateTime.now());
        return turnoCajaRepository.save(turno);
    }

    @Transactional
    public void asociarVentaATurno(Venta venta, TurnoCaja turno) {
        venta.setTurnoCaja(turno);
        ventaRepository.save(venta);
    }

    @Transactional(readOnly = true)
    public List<TurnoCaja> findAllTurnosCaja() {
        // Usar el nuevo método del repositorio que carga el vendedor explícitamente
        return turnoCajaRepository.findAllWithVendedor(); // <--- CORRECCIÓN AQUÍ
    }
}
