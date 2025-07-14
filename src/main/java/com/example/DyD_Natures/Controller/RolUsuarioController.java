package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Permiso;
import com.example.DyD_Natures.Model.RolUsuario;
import com.example.DyD_Natures.Service.PermisoService;
import com.example.DyD_Natures.Service.RolUsuarioService;
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

    /**
     * Vista principal: carga Thymeleaf para /roles
     */
    @GetMapping
    public String listarRoles(Model model) {
        model.addAttribute("roles", rolService.listarRoles());
        return "roles";
    }

    /**
     * Devuelve todos los roles en JSON
     */
    @GetMapping("/all")
    @ResponseBody
    public List<RolUsuario> getAllRoles() {
        return rolService.listarRoles();
    }

    /**
     * Fragmento de formulario para “Nuevo Rol”
     */
    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        // 1) objeto vacío para el form
        model.addAttribute("rolUsuario", new RolUsuario());
        // 2) cargamos **todos** los permisos existentes
        model.addAttribute("todosPermisos", permisoService.listarPermisos());
        // devolvemos solo el fragmento Thymeleaf
        return "fragments/roles_form_modal :: formContent";
    }

    /**
     * Fragmento de formulario para “Editar Rol”
     */
    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Integer id, Model model) {
        // 1) cargamos el rol si existe
        rolService.obtenerRolPorId(id)
                .ifPresent(r -> model.addAttribute("rolUsuario", r));
        // 2) cargamos la **misma** lista completa de permisos
        model.addAttribute("todosPermisos", permisoService.listarPermisos());
        // devolvemos el mismo fragmento
        return "fragments/roles_form_modal :: formContent";
    }

    /**
     * Guarda o actualiza un rol; responde con {status: "success"}
     */
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String,String>> guardar(
            @ModelAttribute RolUsuario rol,
            @RequestParam(value = "permisos", required = false) List<Integer> permisosIds
    ) {
        // 1) Construimos el set de Permiso a partir de los IDs recibidos
        Set<Permiso> permisos = Optional.ofNullable(permisosIds)
                .map(ids -> ids.stream()
                        .map(id -> permisoService.obtenerPermisoPorId(id)
                                .orElseThrow(() -> new IllegalArgumentException("Permiso inválido: " + id)))
                        .collect(Collectors.toSet()))
                .orElse(new HashSet<>());

        // 2) Asignamos al rol y guardamos
        rol.setPermisos(permisos);
        rolService.guardarRol(rol);

        // 3) Devolvemos OK
        return ResponseEntity.ok(Map.of("status","success"));
    }

    /**
     * Elimina un rol por su ID; responde con {status: "success"}
     */
    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String,String>> eliminar(@PathVariable Integer id) {
        rolService.eliminarRol(id);
        return ResponseEntity.ok(Map.of("status","success"));
    }
}
