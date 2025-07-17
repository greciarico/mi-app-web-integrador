package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller // Este controlador sirve vistas (HTML)
public class DashboardController {
    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/") // Mapea la ruta raíz de tu aplicación
    public String home(HttpServletRequest request, Model model, Authentication authentication) {
        // Verifica si hay un usuario autenticado
        if (authentication != null && authentication.isAuthenticated()) {
            // El nombre de usuario en el Authentication object será el DNI,
            // ya que así lo configuraste en tu UsuarioDetailsService.
            String dniUsuarioLogueado = authentication.getName();

            // Busca el usuario completo en la base de datos usando el DNI
            usuarioService.obtenerUsuarioPorDni(dniUsuarioLogueado).ifPresent(usuario -> {
                model.addAttribute("currentUser", usuario);
            });
        }
        model.addAttribute("currentUri", request.getRequestURI());
        return "dashboard";  // Retorna el nombre de tu plantilla Thymeleaf (layout.html)
    }
}
