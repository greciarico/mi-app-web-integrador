package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.TurnoCaja;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.TurnoCajaService;
import com.example.DyD_Natures.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal; // Importante para obtener el usuario autenticado
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/caja")
public class TurnoCajaController {

    private final TurnoCajaService turnoCajaService;
    private final UsuarioService usuarioService;

    @Autowired
    public TurnoCajaController(TurnoCajaService turnoCajaService, UsuarioService usuarioService) {
        this.turnoCajaService = turnoCajaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/a-c-caja")
    public String mostrarFormularioAC_Caja(Model model, Principal principal) {
        String dniUsuarioAutenticado = principal.getName();
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorDni(dniUsuarioAutenticado);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Error: Usuario autenticado no encontrado en la base de datos.");
            return "errorPage";
        }

        Usuario usuarioActual = usuarioOpt.get();
        Optional<TurnoCaja> turnoAbierto = turnoCajaService.getTurnoCajaAbierto(usuarioActual);

        if (turnoAbierto.isPresent()) {
            model.addAttribute("currentTurno", turnoAbierto.get());
            return "caja/gestionCaja";
        } else {
            model.addAttribute("fondoInicial", BigDecimal.ZERO);
            return "caja/abrirCaja";
        }
    }

    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam BigDecimal fondoInicialEfectivo, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        String dniUsuarioAutenticado = principal.getName();
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorDni(dniUsuarioAutenticado);

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Usuario no encontrado para abrir el turno.");
            return "redirect:/errorPage";
        }

        try {
            turnoCajaService.abrirTurnoCaja(usuarioOpt.get(), fondoInicialEfectivo);
            redirectAttributes.addFlashAttribute("successMessage", "Turno de caja abierto exitosamente.");
            return "redirect:/caja/a-c-caja";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/caja/a-c-caja";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al abrir el turno de caja: " + e.getMessage());
            return "redirect:/caja/a-c-caja";
        }
    }

    @PostMapping("/cerrar")
    public String cerrarCaja(@RequestParam("idTurnoCaja") Integer idTurnoCaja,
                             @RequestParam("conteoFinalEfectivo") BigDecimal conteoFinalEfectivo,
                             RedirectAttributes redirectAttributes) {
        try {
            turnoCajaService.cerrarYCuadrarTurnoCaja(idTurnoCaja, conteoFinalEfectivo);
            redirectAttributes.addFlashAttribute("successMessage", "Turno de caja cerrado y cuadrado exitosamente.");
            return "redirect:/caja/historial-caja";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/caja/a-c-caja";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al cerrar el turno de caja: " + e.getMessage());
            return "redirect:/caja/a-c-caja";
        }
    }

    @GetMapping("/historial-caja")
    public String mostrarHistorialCaja(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login"; // Redirige si no hay autenticación.
        }

        // El DNI es el username en tu UserDetails
        String dniUsuarioAutenticado = userDetails.getUsername();
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorDni(dniUsuarioAutenticado);

        if (usuarioOpt.isEmpty()) {
            // Esto sería un error grave: usuario autenticado pero no encontrado en BD.
            model.addAttribute("errorMessage", "Error: No se pudo cargar la información completa del usuario autenticado.");
            return "errorPage"; // O una página de error más específica
        }

        Usuario usuarioActual = usuarioOpt.get();
        List<TurnoCaja> historial = turnoCajaService.getHistorialTurnosCaja(usuarioActual);
        model.addAttribute("historialTurnos", historial);
        return "caja/historialCaja"; // Vista para mostrar el historial
    }
}
