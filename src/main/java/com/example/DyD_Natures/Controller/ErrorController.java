package com.example.DyD_Natures.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorController {
    @GetMapping("/access-denegado")
    public String accesoDenegado() {
        return "error/403";  // Apunta a src/main/resources/templates/error/403.html
    }
}
