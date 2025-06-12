package com.example.DyD_Natures.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vendedor") // Mapea todas las peticiones a /vendedor a este controlador
public class VendedorController {

    /**
     * Muestra la página principal del dashboard del vendedor.
     * Accessible solo para usuarios con el rol VENDEDOR, según la configuración de SecurityConfig.
     * @return El nombre de la vista (vendedor.html)
     */
    @GetMapping // Maneja las peticiones GET a /vendedor
    public String showVendedorDashboard() {
        return "vendedor"; // Devuelve el nombre de la plantilla HTML, que Spring Boot buscará como src/main/resources/templates/vendedor.html
    }
}
