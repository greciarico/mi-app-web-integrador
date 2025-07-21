package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.UsuarioFilterDTO;
import com.example.DyD_Natures.Model.RolUsuario;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.RolUsuarioService;
import com.example.DyD_Natures.Service.UsuarioService;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.itextpdf.text.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

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
    public String listarUsuarios(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("usuarios", usuarioService.listarUsuariosActivos());
        model.addAttribute("todosRoles", rolUsuarioService.listarRoles());
        return "usuarios";
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Usuario> getAllUsersJson() {
        return usuarioService.listarUsuariosActivos();
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
        usuario.setRolUsuario(new RolUsuario());
        model.addAttribute("usuario", usuario);
        model.addAttribute("todosRoles", rolUsuarioService.listarRoles());
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
            model.addAttribute("todosRoles", rolUsuarioService.listarRoles());
            // Devuelve el fragmento del formulario dentro del modal
            return "fragments/usuarios_form_modal :: formContent";
        }
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("todosRoles", rolUsuarioService.listarRoles());
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
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarUsuario(@ModelAttribute Usuario usuario) {
        Map<String, String> response = new HashMap<>();
        try {
            if (usuario.getDni() == null || usuario.getDni().isBlank()) {
                response.put("status", "error");
                response.put("message", "El DNI no puede estar vacío.");
                return ResponseEntity.badRequest().body(response);
            }
            Optional<Usuario> existente = usuarioService.obtenerUsuarioPorDni(usuario.getDni());
            if (existente.isPresent() &&
                    (usuario.getIdUsuario() == null || !existente.get().getIdUsuario().equals(usuario.getIdUsuario()))) {
                response.put("status", "error");
                response.put("message", "El DNI ya está registrado por otro usuario.");
                return ResponseEntity.badRequest().body(response);
            }
            if (usuario.getIdUsuario() == null && usuario.getFechaRegistro() == null) {
                usuario.setFechaRegistro(LocalDate.now());
            }
            if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {
                usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            } else if (usuario.getIdUsuario() != null) {
                usuarioService.obtenerUsuarioPorId(usuario.getIdUsuario())
                        .ifPresent(old -> usuario.setContrasena(old.getContrasena()));
            }
            Integer rolId = usuario.getRolUsuario() != null
                    ? usuario.getRolUsuario().getIdRol()
                    : null;
            if (rolId == null) {
                throw new IllegalArgumentException("Debe seleccionar un rol.");
            }
            RolUsuario rol = rolUsuarioService.obtenerRolPorId(rolId)
                    .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + rolId));
            usuario.setRolUsuario(rol);
            if (usuario.getEstado() == null) {
                usuario.setEstado((byte) 1);
            }
            usuarioService.guardarUsuario(usuario);

            response.put("status", "success");
            response.put("message", "Usuario guardado exitosamente!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Error al guardar el usuario: " + ex.getMessage());
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
    @ResponseBody
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
    @GetMapping("/checkDni")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkDni(@RequestParam String dni,
                                                         @RequestParam(required = false) Integer idUsuario) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = usuarioService.existsByDniExcludingId(dni, idUsuario);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
    /**
     * Busca usuarios aplicando filtros dinámicamente.
     * @param filterDTO DTO con los criterios de búsqueda.
     * @return Lista de usuarios que coinciden con los filtros.
     */
    @GetMapping("/buscar")
    @ResponseBody
    public List<Usuario> buscarUsuarios(@ModelAttribute UsuarioFilterDTO filterDTO) {
        System.out.println("Buscando usuarios con filtros: " + filterDTO);
        return usuarioService.buscarUsuariosPorFiltros(filterDTO);
    }

    @GetMapping("/reporte/pdf")
    public void generarReportePdf(UsuarioFilterDTO filterDTO, HttpServletResponse response) throws IOException, DocumentException {
        System.out.println("Filtros recibidos para el reporte: " + filterDTO);

        List<Usuario> usuarios = usuarioService.buscarUsuariosPorFiltros(filterDTO);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"reporte_usuarios.pdf\"");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitle = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
            Paragraph title = new Paragraph("Reporte de Usuarios - D&D Nature's", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC);
            Paragraph filterParagraph = new Paragraph("Filtros aplicados:\n", fontFilters);

            if (filterDTO.getNombreApellidoDniCorreo() != null && !filterDTO.getNombreApellidoDniCorreo().trim().isEmpty()) {
                filterParagraph.add(new Chunk("Búsqueda General: " + filterDTO.getNombreApellidoDniCorreo() + "\n", fontFilters));
            }

            if (filterDTO.getIdRoles() != null && !filterDTO.getIdRoles().isEmpty()) {
                List<String> nombresRolesSeleccionados = new ArrayList<>();
                List<RolUsuario> todosLosRoles = rolUsuarioService.listarRoles();
                for (Integer idRolSeleccionado : filterDTO.getIdRoles()) {
                    todosLosRoles.stream()
                            .filter(rol -> rol.getIdRol().equals(idRolSeleccionado))
                            .findFirst()
                            .ifPresent(rol -> nombresRolesSeleccionados.add(rol.getTipoRol()));
                }
                if (!nombresRolesSeleccionados.isEmpty()) {
                    filterParagraph.add(new Chunk("Tipo(s) de Usuario: " + String.join(", ", nombresRolesSeleccionados) + "\n", fontFilters));
                } else {
                    filterParagraph.add(new Chunk("Tipo(s) de Usuario: Ninguno seleccionado o no válido\n", fontFilters));
                }
            } else {
                filterParagraph.add(new Chunk("Tipo(s) de Usuario: Todos\n", fontFilters));
            }

            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                List<String> nombresEstadosSeleccionados = new ArrayList<>();
                if (filterDTO.getEstados().contains(1)) {
                    nombresEstadosSeleccionados.add("Activo");
                }
                if (filterDTO.getEstados().contains(0)) {
                    nombresEstadosSeleccionados.add("Inactivo");
                }
                if (!nombresEstadosSeleccionados.isEmpty()) {
                    filterParagraph.add(new Chunk("Estado(s): " + String.join(", ", nombresEstadosSeleccionados) + "\n", fontFilters));
                } else {
                    filterParagraph.add(new Chunk("Estado(s): Ninguno seleccionado o no válido\n", fontFilters));
                }
            } else {
                filterParagraph.add(new Chunk("Estado(s): Todos\n", fontFilters));
            }

            if (filterDTO.getFechaRegistroStart() != null) {
                filterParagraph.add(new Chunk("Fecha Registro Desde: " + filterDTO.getFechaRegistroStart().toString() + "\n", fontFilters));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                filterParagraph.add(new Chunk("Fecha Registro Hasta: " + filterDTO.getFechaRegistroEnd().toString() + "\n", fontFilters));
            }


            filterParagraph.setSpacingAfter(15);
            document.add(filterParagraph);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            float[] columnWidths = {0.5f, 1.5f, 1.5f, 1.5f, 1f, 1f, 2f, 1f, 0.8f};
            table.setWidths(columnWidths);
            Font fontHeader = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            BaseColor headerBgColor = new BaseColor(24, 61, 0);

            String[] headers = {"ID", "Nombre", "Ap. Paterno", "Ap. Materno", "DNI", "Rol", "Correo", "Celular", "Estado"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(headerBgColor);
                cell.setPadding(5);
                table.addCell(cell);
            }
            Font fontContent = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);

            if (usuarios.isEmpty()) {
                PdfPCell noDataCell = new PdfPCell(new Phrase("No se encontraron usuarios con los filtros aplicados.", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC)));
                noDataCell.setColspan(9);
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                noDataCell.setPadding(10);
                table.addCell(noDataCell);
            } else {
                for (Usuario usuario : usuarios) {
                    table.addCell(new Phrase(usuario.getIdUsuario() != null ? usuario.getIdUsuario().toString() : "", fontContent));
                    table.addCell(new Phrase(usuario.getNombre() != null ? usuario.getNombre() : "", fontContent));
                    table.addCell(new Phrase(usuario.getApPaterno() != null ? usuario.getApPaterno() : "", fontContent));
                    table.addCell(new Phrase(usuario.getApMaterno() != null ? usuario.getApMaterno() : "", fontContent));
                    table.addCell(new Phrase(usuario.getDni() != null ? usuario.getDni() : "", fontContent));
                    table.addCell(new Phrase(usuario.getRolUsuario() != null ? usuario.getRolUsuario().getTipoRol() : "N/A", fontContent));
                    table.addCell(new Phrase(usuario.getCorreo() != null ? usuario.getCorreo() : "", fontContent));
                    table.addCell(new Phrase(usuario.getTelefono() != null ? usuario.getTelefono() : "", fontContent));
                    table.addCell(new Phrase(usuario.getEstado() == 1 ? "Activo" : (usuario.getEstado() == 0 ? "Inactivo" : "Desconocido"), fontContent));
                }
            }

            document.add(table);
            document.close();
            response.getOutputStream().write(baos.toByteArray());
        } catch (DocumentException e) {
            throw new IOException("Error al generar el PDF", e);
        }
    }
    @GetMapping("/cambiarEstado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cambiarEstadoUsuario(@PathVariable("id") Integer idUsuario) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Usuario> optionalUsuario = usuarioService.obtenerUsuarioPorId(idUsuario);

            if (optionalUsuario.isPresent()) {
                Usuario usuario = optionalUsuario.get();

                byte nuevoEstado = (usuario.getEstado() == 1) ? (byte) 0 : (byte) 1;
                usuario.setEstado(nuevoEstado);

                usuarioService.guardarUsuario(usuario);

                response.put("status", "success");
                response.put("message", "Estado del usuario actualizado correctamente.");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Usuario no encontrado.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al cambiar el estado del usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
