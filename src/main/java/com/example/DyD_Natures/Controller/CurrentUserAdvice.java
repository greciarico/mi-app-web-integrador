package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    @Autowired
    private UsuarioService usuarioService;

    @ModelAttribute("currentUser")
    public Usuario currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return usuarioService.obtenerUsuarioPorDni(auth.getName())
                .orElse(null);
    }
}
