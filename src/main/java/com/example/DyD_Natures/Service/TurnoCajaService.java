package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.TurnoCaja;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Model.Venta;
import com.example.DyD_Natures.Repository.TurnoCajaRepository;
import com.example.DyD_Natures.Repository.VentaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        Optional<TurnoCaja> existingTurnoActivo = turnoCajaRepository.findByVendedorAndEstadoCuadre(vendedor, "Abierto");
        if (existingTurnoActivo.isPresent()) {
            throw new IllegalStateException("El vendedor " + vendedor.getNombre() + " ya tiene un turno de caja abierto.");
        }
        LocalDate today = LocalDate.now();

        Optional<TurnoCaja> existingTurnoToday = turnoCajaRepository.findByVendedorAndFechaAperturaDia(vendedor, today);

        if (existingTurnoToday.isPresent()) {
            throw new IllegalStateException("El vendedor " + vendedor.getNombre() + " ya aperturó y/o cerró un turno de caja hoy. No se permite más de un turno por día.");
        }

        TurnoCaja nuevoTurno = new TurnoCaja(vendedor, fondoInicialEfectivo);
        nuevoTurno.setTotalVentasEfectivoSistema(BigDecimal.ZERO);
        nuevoTurno.setTotalVentasMonederoElectronicoSistema(BigDecimal.ZERO);
        nuevoTurno.setFechaApertura(LocalDateTime.now());
        nuevoTurno.setEstadoCuadre("Abierto");
        return turnoCajaRepository.save(nuevoTurno);
    }

    @Transactional(readOnly = true)
    public Optional<TurnoCaja> getTurnoCajaAbierto(Usuario vendedor) {
        Optional<TurnoCaja> turnoOpt = turnoCajaRepository.findByVendedorAndEstadoCuadre(vendedor, "Abierto");
        if (turnoOpt.isPresent()) {
            TurnoCaja turno = turnoOpt.get();
            recalcularMontosVentas(turno);
        }
        return turnoOpt;
    }

    @Transactional
    public void registrarMontoVentaEnCaja(TurnoCaja turnoCaja, BigDecimal montoEfectivo, BigDecimal montoMonederoElectronico) {
        if (turnoCaja == null) {
            throw new IllegalArgumentException("El turno de caja no puede ser nulo al registrar montos.");
        }
        if (!"Abierto".equalsIgnoreCase(turnoCaja.getEstadoCuadre())) {
            throw new IllegalStateException("Solo se pueden registrar ventas en un turno de caja abierto. Estado actual: " + turnoCaja.getEstadoCuadre());
        }

        BigDecimal efectivoToAdd = (montoEfectivo != null && montoEfectivo.compareTo(BigDecimal.ZERO) > 0) ? montoEfectivo : BigDecimal.ZERO;
        BigDecimal monederoToAdd = (montoMonederoElectronico != null && montoMonederoElectronico.compareTo(BigDecimal.ZERO) > 0) ? montoMonederoElectronico : BigDecimal.ZERO;

        BigDecimal currentEfectivoSistema = Optional.ofNullable(turnoCaja.getTotalVentasEfectivoSistema()).orElse(BigDecimal.ZERO);
        BigDecimal currentMonederoSistema = Optional.ofNullable(turnoCaja.getTotalVentasMonederoElectronicoSistema()).orElse(BigDecimal.ZERO);

        turnoCaja.setTotalVentasEfectivoSistema(currentEfectivoSistema.add(efectivoToAdd));
        turnoCaja.setTotalVentasMonederoElectronicoSistema(currentMonederoSistema.add(monederoToAdd));

        turnoCajaRepository.save(turnoCaja);
    }

    @Transactional
    public void revertirMontoVentaEnCaja(TurnoCaja turnoCaja, BigDecimal montoEfectivo, BigDecimal montoMonederoElectronico) {
        if (turnoCaja == null) {
            throw new IllegalArgumentException("El turno de caja no puede ser nulo al revertir montos.");
        }

        BigDecimal efectivoToSubtract = (montoEfectivo != null) ? montoEfectivo : BigDecimal.ZERO;
        BigDecimal monederoToSubtract = (montoMonederoElectronico != null) ? montoMonederoElectronico : BigDecimal.ZERO;

        BigDecimal currentEfectivoSistema = Optional.ofNullable(turnoCaja.getTotalVentasEfectivoSistema()).orElse(BigDecimal.ZERO);
        BigDecimal currentMonederoSistema = Optional.ofNullable(turnoCaja.getTotalVentasMonederoElectronicoSistema()).orElse(BigDecimal.ZERO);

        turnoCaja.setTotalVentasEfectivoSistema(currentEfectivoSistema.subtract(efectivoToSubtract));
        turnoCaja.setTotalVentasMonederoElectronicoSistema(currentMonederoSistema.subtract(monederoToSubtract));

        if (turnoCaja.getTotalVentasEfectivoSistema().compareTo(BigDecimal.ZERO) < 0) {
            turnoCaja.setTotalVentasEfectivoSistema(BigDecimal.ZERO);
        }
        if (turnoCaja.getTotalVentasMonederoElectronicoSistema().compareTo(BigDecimal.ZERO) < 0) {
            turnoCaja.setTotalVentasMonederoElectronicoSistema(BigDecimal.ZERO);
        }

        turnoCajaRepository.save(turnoCaja);
    }

    private void recalcularMontosVentas(TurnoCaja turno) {
        List<Venta> ventasDelTurno = ventaRepository.findByTurnoCaja(turno);

        BigDecimal totalEfectivoVentas = BigDecimal.ZERO;
        BigDecimal totalMonederoElectronicoVentas = BigDecimal.ZERO;

        for (Venta venta : ventasDelTurno) {
            if (venta.getEstado() != null && venta.getEstado() == 1) {
                totalEfectivoVentas = totalEfectivoVentas.add(Optional.ofNullable(venta.getMontoEfectivo()).orElse(BigDecimal.ZERO));
                totalMonederoElectronicoVentas = totalMonederoElectronicoVentas.add(Optional.ofNullable(venta.getMontoMonederoElectronico()).orElse(BigDecimal.ZERO));
            }
        }
        turno.setTotalVentasEfectivoSistema(totalEfectivoVentas);
        turno.setTotalVentasMonederoElectronicoSistema(totalMonederoElectronicoVentas);
    }

    @Transactional
    // Método modificado para recibir ambos conteos directamente
    public TurnoCaja cerrarYCuadrarTurnoCaja(Integer idTurnoCaja, BigDecimal conteoFinalEfectivo, BigDecimal conteoFinalMonedero) {
        TurnoCaja turno = turnoCajaRepository.findById(idTurnoCaja)
                .orElseThrow(() -> new EntityNotFoundException("Turno de caja no encontrado con ID: " + idTurnoCaja));

        if (!"Abierto".equals(turno.getEstadoCuadre())) {
            throw new IllegalStateException("El turno de caja no está abierto para cuadrar.");
        }

        recalcularMontosVentas(turno);

        BigDecimal totalEfectivoVentasSistema = turno.getTotalVentasEfectivoSistema();
        BigDecimal totalMonederoElectronicoVentasSistema = turno.getTotalVentasMonederoElectronicoSistema();

        // LÓGICA DE CUADRE PARA EFECTIVO
        BigDecimal efectivoEsperadoSistema = turno.getFondoInicialEfectivo().add(totalEfectivoVentasSistema);

        turno.setConteoFinalEfectivo(conteoFinalEfectivo);
        BigDecimal diferenciaEfectivo = conteoFinalEfectivo.subtract(efectivoEsperadoSistema);
        turno.setDiferenciaEfectivo(diferenciaEfectivo);

        // LÓGICA DE CUADRE PARA MONEDERO ELECTRÓNICO (YAPE)
        turno.setConteoFinalMonedero(conteoFinalMonedero);
        BigDecimal diferenciaMonedero = conteoFinalMonedero.subtract(totalMonederoElectronicoVentasSistema);
        turno.setDiferenciaMonedero(diferenciaMonedero);

        // Determinar el estado general del cuadre
        if (diferenciaEfectivo.compareTo(BigDecimal.ZERO) == 0 && diferenciaMonedero.compareTo(BigDecimal.ZERO) == 0) {
            turno.setEstadoCuadre("Cuadrado");
        } else if (diferenciaEfectivo.compareTo(BigDecimal.ZERO) < 0 || diferenciaMonedero.compareTo(BigDecimal.ZERO) < 0) {
            turno.setEstadoCuadre("Con Faltante");
        } else {
            turno.setEstadoCuadre("Con Sobrante");
        }

        turno.setFechaCierre(LocalDateTime.now());
        return turnoCajaRepository.save(turno);
    }

    @Transactional(readOnly = true)
    public List<TurnoCaja> getHistorialTurnosCaja(Usuario usuario) {
        if (usuario.getRolUsuario() != null && "ADMINISTRADOR".equalsIgnoreCase(usuario.getRolUsuario().getTipoRol())) {
            return turnoCajaRepository.findAllWithVendedor();
        } else {
            return turnoCajaRepository.findByVendedorOrderByFechaAperturaDesc(usuario);
        }
    }

    @Transactional
    public void cerrarTurnosOlvidadosAutomaticamente() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<TurnoCaja> turnosOlvidados = turnoCajaRepository.findByEstadoCuadreAndFechaAperturaBefore("Abierto", startOfToday);

        if (turnosOlvidados.isEmpty()) {
            System.out.println("No se encontraron turnos de caja olvidados para cerrar.");
            return;
        }

        System.out.println("Cerrando " + turnosOlvidados.size() + " turnos de caja olvidados...");

        for (TurnoCaja turno : turnosOlvidados) {
            try {
                turno.setFechaCierre(LocalDateTime.now());
                recalcularMontosVentas(turno);

                BigDecimal fondoInicial = Optional.ofNullable(turno.getFondoInicialEfectivo()).orElse(BigDecimal.ZERO);
                BigDecimal totalEfectivoVentas = Optional.ofNullable(turno.getTotalVentasEfectivoSistema()).orElse(BigDecimal.ZERO);
                BigDecimal totalMonederoElectronicoVentas = Optional.ofNullable(turno.getTotalVentasMonederoElectronicoSistema()).orElse(BigDecimal.ZERO); // También para cierre automático

                BigDecimal efectivoEsperado = fondoInicial.add(totalEfectivoVentas);

                turno.setConteoFinalEfectivo(BigDecimal.ZERO);
                turno.setDiferenciaEfectivo(BigDecimal.ZERO.subtract(efectivoEsperado));

                // Para cierre automático de monedero, asumimos conteo 0 y el faltante es lo que el sistema esperaba
                turno.setConteoFinalMonedero(BigDecimal.ZERO);
                turno.setDiferenciaMonedero(BigDecimal.ZERO.subtract(totalMonederoElectronicoVentas));

                // El estado general reflejará si hay faltantes en efectivo o monedero
                if (turno.getDiferenciaEfectivo().compareTo(BigDecimal.ZERO) < 0 || turno.getDiferenciaMonedero().compareTo(BigDecimal.ZERO) < 0) {
                    turno.setEstadoCuadre("Cerrado Automatico - Faltante");
                } else {
                    turno.setEstadoCuadre("Cerrado Automatico - Cuadrado"); // Menos probable, pero posible si no hubo ventas
                }


                turnoCajaRepository.save(turno);
                System.out.println("Turno de caja ID " + turno.getIdTurnoCaja() + " cerrado automáticamente. Faltante Efectivo: " + turno.getDiferenciaEfectivo() + ", Faltante Monedero: " + turno.getDiferenciaMonedero());
            } catch (Exception e) {
                System.err.println("Error al cerrar automáticamente el turno ID " + turno.getIdTurnoCaja() + ": " + e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TurnoCaja> listarTurnosCaja() {
        return turnoCajaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TurnoCaja> obtenerTurnoCajaPorId(Integer id) {
        return turnoCajaRepository.findById(id);
    }
}
