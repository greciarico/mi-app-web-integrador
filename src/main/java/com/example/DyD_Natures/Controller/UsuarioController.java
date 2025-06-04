package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.RolUsuario;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.RolUsuarioService;
import com.example.DyD_Natures.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolUsuarioService rolUsuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Muestra la lista de usuarios.
     * Esta es la vista principal que se carga en el content-area.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (usuarios.html).
     */
    @GetMapping
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        model.addAttribute("roles", rolUsuarioService.listarRoles()); // Necesario para el modal incluso si no se muestra inicialmente
        return "usuarios"; // Devuelve la vista principal usuarios.html
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Usuario> getAllUsersJson() {
        return usuarioService.listarUsuarios(); // Asegúrate de llamar a este método
    }


    /**
     * Muestra el formulario para crear un nuevo usuario.
     * Devuelve solo el fragmento del formulario para ser cargado vía AJAX en el modal.
     * @param model El modelo para pasar datos a la vista.
     * @return El fragmento Thymeleaf del formulario.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Usuario usuario = new Usuario();
        usuario.setRolUsuario(new RolUsuario()); // Inicializa rolUsuario para evitar NPE
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", rolUsuarioService.listarRoles());
        // Devuelve el fragmento del formulario dentro del modal
        return "fragments/usuarios_form_modal :: formContent";
    }

    /**
     * Muestra el formulario para editar un usuario existente.
     * Devuelve solo el fragmento del formulario para ser cargado vía AJAX en el modal.
     * @param id El ID del usuario a editar.
     * @param model El modelo para pasar datos a la vista.
     * @return El fragmento Thymeleaf del formulario si el usuario existe, o un error.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorId(id);
        if (usuarioOpt.isPresent()) {
            model.addAttribute("usuario", usuarioOpt.get());
            model.addAttribute("roles", rolUsuarioService.listarRoles());
            // Devuelve el fragmento del formulario dentro del modal
            return "fragments/usuarios_form_modal :: formContent";
        }
        // Si el usuario no se encuentra, puedes devolver un fragmento de error o manejarlo en el JS del cliente
        // Por ahora, devolvemos un formulario vacío con un mensaje de error (esto es una simplificación)
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolUsuarioService.listarRoles());
        model.addAttribute("mensajeError", "Usuario no encontrado.");
        return "fragments/usuarios_form_modal :: formContent";
    }

    /**
     * Guarda un usuario nuevo o actualiza uno existente.
     * Responde con un JSON indicando éxito o fracaso.
     * @param usuario El objeto Usuario a guardar.
     * @return ResponseEntity con un mensaje JSON.
     */
    @PostMapping("/guardar")
    @ResponseBody // Indica que el método devuelve directamente el cuerpo de la respuesta (JSON en este caso)
    public ResponseEntity<Map<String, String>> guardarUsuario(@ModelAttribute Usuario usuario) {
        Map<String, String> response = new HashMap<>();
        try {
            // =======================================================================
            // INICIO DE LAS VALIDACIONES DE DNI A NIVEL DE SERVIDOR (LO QUE DEBES AGREGAR)
            // =======================================================================

            // 1. Validar que el DNI no esté vacío
            if (usuario.getDni() == null || usuario.getDni().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El DNI no puede estar vacío.");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. Validar que el DNI no esté ya registrado por otro usuario
            // Obtener el usuario existente por DNI
            Optional<Usuario> existingUserByDni = usuarioService.obtenerUsuarioPorDni(usuario.getDni());

            // Si se encontró un usuario con ese DNI Y no es el mismo usuario que estamos editando
            if (existingUserByDni.isPresent() && (usuario.getIdUsuario() == null || !existingUserByDni.get().getIdUsuario().equals(usuario.getIdUsuario()))) {
                response.put("status", "error");
                response.put("message", "El DNI ya está registrado por otro usuario.");
                return ResponseEntity.badRequest().body(response);
            }

            // =======================================================================
            // FIN DE LAS VALIDACIONES DE DNI
            // =======================================================================


            // Establece la fecha de registro solo si es un nuevo usuario (idUsuario es null)
            if (usuario.getFechaRegistro() == null && usuario.getIdUsuario() == null) {
                usuario.setFechaRegistro(LocalDate.now());
            }

            // Solo codifica la contraseña si se ha proporcionado una nueva contraseña
            if (usuario.getContrasena() != null && !usuario.getContrasena().isEmpty()) {
                String passHasheada = passwordEncoder.encode(usuario.getContrasena());
                usuario.setContrasena(passHasheada);
            } else if (usuario.getIdUsuario() != null) {
                // Si es una edición y el campo de contraseña está vacío,
                // recupera la contraseña existente del usuario de la base de datos
                Optional<Usuario> existingUserOpt = usuarioService.obtenerUsuarioPorId(usuario.getIdUsuario());
                existingUserOpt.ifPresent(existingUser -> usuario.setContrasena(existingUser.getContrasena()));
            }

            usuarioService.guardarUsuario(usuario);
            response.put("status", "success");
            response.put("message", "Usuario guardado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al guardar el usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Elimina un usuario por su ID.
     * Responde con un JSON indicando éxito o fracaso.
     * @param id El ID del usuario a eliminar.
     * @return ResponseEntity con un mensaje JSON.
     */
    @GetMapping("/eliminar/{id}")
    @ResponseBody // Indica que el método devuelve directamente el cuerpo de la respuesta (JSON en este caso)
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            usuarioService.eliminarUsuario(id);
            response.put("status", "success");
            response.put("message", "Usuario eliminado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al eliminar el usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verifica si un DNI ya existe en la base de datos.
     * @param dni El DNI a verificar.
     * @param idUsuario ID del usuario actual (opcional, para exclusión en ediciones).
     * @return ResponseEntity con un JSON indicando si el DNI existe.
     */
    @GetMapping("/checkDni") // Asegúrate de que esta anotación y el nombre del método sean correctos
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkDni(@RequestParam String dni,
                                                         @RequestParam(required = false) Integer idUsuario) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = usuarioService.existsByDniExcludingId(dni, idUsuario);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}

