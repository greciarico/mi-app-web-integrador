package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Usuario; // Importa la clase Usuario
import com.example.DyD_Natures.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Importa Authentication
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Importa Model
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class DashboardController {
    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
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
        return "layout";  // tu archivo layout.html
    }
}

