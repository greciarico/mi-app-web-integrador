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
import java.time.LocalDate;
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
        Optional<TurnoCaja> existingTurnoActivo = turnoCajaRepository.findByVendedorAndEstadoCuadre(vendedor, "Abierto");
        if (existingTurnoActivo.isPresent()) {
            throw new IllegalStateException("El vendedor " + vendedor.getNombre() + " ya tiene un turno de caja abierto.");
        }
        LocalDate today = LocalDate.now(); // Asume la zona horaria de la JVM o la configurada en Spring JPA

        Optional<TurnoCaja> existingTurnoToday = turnoCajaRepository.findByVendedorAndFechaAperturaDia(vendedor, today);

        if (existingTurnoToday.isPresent()) {
            throw new IllegalStateException("El vendedor " + vendedor.getNombre() + " ya aperturó y/o cerró un turno de caja hoy. No se permite más de un turno por día.");
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
    public List<TurnoCaja> getHistorialTurnosCaja(Usuario usuario) {
        // Asumimos que el nombre del rol de administrador en tu BD es "ADMIN"
        // y el de vendedor es "VENDEDOR" o similar.
        if (usuario.getRolUsuario() != null && "ADMINISTRADOR".equalsIgnoreCase(usuario.getRolUsuario().getTipoRol())) {
            // Si es administrador, devuelve todos los turnos
            return turnoCajaRepository.findAllWithVendedor();
        } else {
            // Si no es administrador (es un vendedor o cualquier otro rol), devuelve solo sus propios turnos
            // Necesitarás crear este nuevo método en tu TurnoCajaRepository
            return turnoCajaRepository.findByVendedorOrderByFechaAperturaDesc(usuario);
        }
    }

    @Transactional
    public void cerrarTurnosOlvidadosAutomaticamente() {
        LocalDateTime endOfYesterday = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999).minusDays(1);
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

        List<TurnoCaja> turnosOlvidados = turnoCajaRepository.findByEstadoCuadreAndFechaAperturaBefore("Abierto", startOfToday);

        if (turnosOlvidados.isEmpty()) {
            System.out.println("No se encontraron turnos de caja olvidados para cerrar.");
            return;
        }

        System.out.println("Cerrando " + turnosOlvidados.size() + " turnos de caja olvidados...");

        for (TurnoCaja turno : turnosOlvidados) {
            try {
                turno.setFechaCierre(LocalDateTime.now());

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

                BigDecimal fondoInicial = turno.getFondoInicialEfectivo() != null ? turno.getFondoInicialEfectivo() : BigDecimal.ZERO;
                BigDecimal efectivoEsperado = fondoInicial.add(totalEfectivoVentas).add(totalMonederoElectronicoVentas);

                turno.setConteoFinalEfectivo(BigDecimal.ZERO);
                turno.setDiferenciaEfectivo(BigDecimal.ZERO.subtract(efectivoEsperado));
                turno.setEstadoCuadre("Cerrado Automatico - Faltante");

                turnoCajaRepository.save(turno);
                System.out.println("Turno de caja ID " + turno.getIdTurnoCaja() + " cerrado automáticamente. Faltante: " + turno.getDiferenciaEfectivo());
            } catch (Exception e) {
                System.err.println("Error al cerrar automáticamente el turno ID " + turno.getIdTurnoCaja() + ": " + e.getMessage());
            }
        }
    }
}
