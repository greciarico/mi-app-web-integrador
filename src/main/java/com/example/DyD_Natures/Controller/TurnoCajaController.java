package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.TurnoCaja;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.TurnoCajaService;
import com.example.DyD_Natures.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal; // Importante para obtener el usuario autenticado
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/caja") // Ruta base del controlador: /caja
public class TurnoCajaController {

    private final TurnoCajaService turnoCajaService;
    private final UsuarioService usuarioService;

    @Autowired
    public TurnoCajaController(TurnoCajaService turnoCajaService, UsuarioService usuarioService) {
        this.turnoCajaService = turnoCajaService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra el formulario de Apertura/Cierre de Caja.
     * Si el usuario tiene un turno abierto, redirige a la página de gestión de caja.
     * Si no, redirige a la página para abrir caja.
     * Mapea a /caja/a-c-caja
     * @param model El modelo para pasar datos a la vista.
     * @param principal Objeto Principal para obtener el usuario autenticado.
     * @return Nombre de la vista a mostrar.
     */
    @GetMapping("/a-c-caja")
    public String mostrarFormularioAC_Caja(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login"; // Redirige si no hay autenticación.
        }

        // CAMBIO CLAVE AQUÍ: Asumiendo que principal.getName() devuelve el DNI del usuario
        // y que UsuarioService tiene un método obtenerUsuarioPorDni(String dni)
        String dniUsuarioAutenticado = principal.getName();
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorDni(dniUsuarioAutenticado);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Error: Usuario autenticado no encontrado en la base de datos.");
            return "errorPage"; // O una página de error más específica
        }

        Usuario usuarioActual = usuarioOpt.get();
        Optional<TurnoCaja> turnoAbierto = turnoCajaService.getTurnoCajaAbierto(usuarioActual);

        if (turnoAbierto.isPresent()) {
            model.addAttribute("currentTurno", turnoAbierto.get());
            return "caja/gestionCaja"; // Vista para gestionar un turno ya abierto
        } else {
            model.addAttribute("fondoInicial", BigDecimal.ZERO); // Valor por defecto para el fondo inicial
            return "caja/abrirCaja"; // Vista para abrir un nuevo turno
        }
    }

    /**
     * Procesa la apertura de un nuevo turno de caja.
     * Mapea a /caja/abrir
     * @param fondoInicialEfectivo El monto inicial de efectivo.
     * @param principal Objeto Principal para obtener el usuario autenticado.
     * @param redirectAttributes Para añadir mensajes flash en la redirección.
     * @return Redirección a la página de Apertura/Cierre de Caja.
     */
    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam BigDecimal fondoInicialEfectivo, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        // CAMBIO CLAVE AQUÍ: Asumiendo que principal.getName() devuelve el DNI del usuario
        String dniUsuarioAutenticado = principal.getName();
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorDni(dniUsuarioAutenticado);

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Usuario no encontrado para abrir el turno.");
            return "redirect:/errorPage";
        }

        try {
            turnoCajaService.abrirTurnoCaja(usuarioOpt.get(), fondoInicialEfectivo);
            redirectAttributes.addFlashAttribute("successMessage", "Turno de caja abierto exitosamente.");
            return "redirect:/caja/a-c-caja"; // Redirige a la página de gestión de caja
        } catch (IllegalStateException e) {
            // Si el usuario ya tiene un turno abierto
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/caja/a-c-caja";
        } catch (Exception e) {
            // Otros errores inesperados
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al abrir el turno de caja: " + e.getMessage());
            return "redirect:/caja/a-c-caja";
        }
    }

    /**
     * Procesa el cierre y cuadre de un turno de caja existente.
     * Mapea a /caja/cerrar
     * @param idTurnoCaja El ID del turno de caja a cerrar.
     * @param conteoFinalEfectivo El monto de efectivo contado físicamente.
     * @param redirectAttributes Para añadir mensajes flash en la redirección.
     * @return Redirección al historial de caja.
     */
    @PostMapping("/cerrar")
    public String cerrarCaja(@RequestParam("idTurnoCaja") Integer idTurnoCaja,
                             @RequestParam("conteoFinalEfectivo") BigDecimal conteoFinalEfectivo,
                             RedirectAttributes redirectAttributes) {
        try {
            turnoCajaService.cerrarYCuadrarTurnoCaja(idTurnoCaja, conteoFinalEfectivo);
            redirectAttributes.addFlashAttribute("successMessage", "Turno de caja cerrado y cuadrado exitosamente.");
            return "redirect:/caja/historial-caja";
        } catch (RuntimeException e) {
            // Captura excepciones como "Turno no encontrado" o "Turno no abierto"
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/caja/a-c-caja"; // Vuelve a la página de apertura/cierre si hay un error
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al cerrar el turno de caja: " + e.getMessage());
            return "redirect:/caja/a-c-caja";
        }
    }

    /**
     * Muestra el historial de todos los turnos de caja.
     * Mapea a /caja/historial-caja
     * @param model El modelo para pasar datos a la vista.
     * @return Nombre de la vista a mostrar.
     */
    @GetMapping("/historial-caja")
    public String mostrarHistorialCaja(Model model) {
        List<TurnoCaja> historial = turnoCajaService.findAllTurnosCaja();
        model.addAttribute("historialTurnos", historial);
        return "caja/historialCaja"; // Vista para mostrar el historial
    }
}
