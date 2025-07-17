package com.example.DyD_Natures.Controller; // Puedes ajustar el paquete si tienes uno específico para 'advice' o 'config'

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Esta clase es un ControllerAdvice global que inyecta atributos comunes
 * en el modelo de todas las vistas.
 * Es esencial para que la variable 'currentUri' esté disponible en Thymeleaf
 * y se puedan resaltar los elementos del menú en el layout.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * Inyecta la URI de la solicitud actual en el modelo bajo el nombre "currentUri".
     * Esto es útil para resaltar elementos del menú en el layout.
     *
     * @param request La solicitud HTTP actual.
     * @return La URI de la solicitud.
     */
    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
