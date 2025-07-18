package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller 
public class DashboardController {
    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/") 
    public String home(HttpServletRequest request, Model model, Authentication authentication) {
        
        if (authentication != null && authentication.isAuthenticated()) {
            String dniUsuarioLogueado = authentication.getName();
            usuarioService.obtenerUsuarioPorDni(dniUsuarioLogueado).ifPresent(usuario -> {
                model.addAttribute("currentUser", usuario);
            });
        }
        model.addAttribute("currentUri", request.getRequestURI());
        return "dashboard"; 
    }
}
