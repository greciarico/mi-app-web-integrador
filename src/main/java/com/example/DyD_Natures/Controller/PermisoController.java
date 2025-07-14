package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Permiso;
import com.example.DyD_Natures.Service.PermisoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permiso")
public class PermisoController {

    private final PermisoService permisoService;

    @Autowired
    public PermisoController(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @GetMapping("/all")
    public List<Permiso> listar() {
        return permisoService.listarPermisos();
    }
}
