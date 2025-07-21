package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Permiso;
import com.example.DyD_Natures.Model.RolUsuario;
import com.example.DyD_Natures.Service.PermisoService;
import com.example.DyD_Natures.Service.RolUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/roles")
public class RolUsuarioController {

    private final RolUsuarioService rolService;
    private final PermisoService permisoService;

    @Autowired
    public RolUsuarioController(RolUsuarioService rolService,
                                PermisoService permisoService) {
        this.rolService = rolService;
        this.permisoService = permisoService;
    }

    @GetMapping
    public String listarRoles(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("roles", rolService.listarRoles());
        return "roles";
    }

    @GetMapping("/all")
    @ResponseBody
    public List<RolUsuario> getAllRoles() {
        return rolService.listarRoles();
    }

    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        model.addAttribute("rolUsuario", new RolUsuario());
        model.addAttribute("todosPermisos", permisoService.listarPermisos());
        return "fragments/roles_form_modal :: formContent";
    }

    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Integer id, Model model) {
        rolService.obtenerRolPorId(id)
                .ifPresent(r -> model.addAttribute("rolUsuario", r));
        model.addAttribute("todosPermisos", permisoService.listarPermisos());
        return "fragments/roles_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardar(
            @ModelAttribute RolUsuario rol,
            @RequestParam(value = "permisos", required = false) List<Integer> permisosIds
    ) {
        try {
            Set<Permiso> permisos = Optional.ofNullable(permisosIds)
                    .map(ids -> ids.stream()
                            .map(id -> permisoService.obtenerPermisoPorId(id)
                                    .orElseThrow(() -> new IllegalArgumentException("Permiso inválido: " + id)))
                            .collect(Collectors.toSet()))
                    .orElse(new HashSet<>());

            rol.setPermisos(permisos);
            rolService.guardarRol(rol);

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String,String>> eliminar(@PathVariable Integer id) {
        rolService.eliminarRol(id);
        return ResponseEntity.ok(Map.of("status","success"));
    }
}
