package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.VentaFilterDTO; // Importar DTO
import com.example.DyD_Natures.Model.*;
import com.example.DyD_Natures.Service.*;
import com.itextpdf.text.DocumentException; // Importar
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse; // Importar
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder; // Importar

import java.io.IOException; // Importar

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ClienteService clienteService; // Para cargar clientes en formularios
    @Autowired
    private ProductoService productoService; // Para cargar productos en formularios
    @Autowired
    private IgvService igvService; // Para cargar IGV en formularios
    @Autowired
    private TipoClienteService tipoClienteService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private PasswordEncoder passwordEncoder; // Inyectar PasswordEncoder
    /**
     * Muestra la vista principal de Ventas.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (venta.html).
     */
    @GetMapping
    public String listarVentas(Model model) {
        try {
            model.addAttribute("ventas", ventaService.listarVentas());
            // No cargamos productos, clientes, IGV aquí directamente, se harán por AJAX en el modal
            return "venta";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar la página de Ventas: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Endpoint para obtener todas las ventas en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de ventas.
     */
    @GetMapping("/all")
    @ResponseBody
    public List<Venta> getAllVentasJson() {
        return ventaService.listarVentas();
    }

    /**
     * Muestra el formulario para crear una nueva venta.
     * @param model El modelo.
     * @return Fragmento del formulario.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Venta venta = new Venta();
        // Inicializar las relaciones para evitar NPE en Thymeleaf en el formulario
        venta.setCliente(new Cliente());
        venta.setIgvEntity(new Igv());
        venta.setDetalleVentas(new ArrayList<>());
        // En una app real, el usuario debería obtenerse del contexto de seguridad
        // venta.setUsuario(new Usuario()); // No es necesario inicializar aquí para el formulario

        model.addAttribute("venta", venta);
        model.addAttribute("clientes", clienteService.listarTodosLosClientesActivos());
        model.addAttribute("productos", productoService.listarSoloProductosActivos());
        model.addAttribute("igvs", igvService.listarSoloIgvActivos());
        return "fragments/venta_form_modal :: formContent";
    }


    /**
     * Muestra el modal de visualización para una venta existente.
     * @param id El ID de la venta.
     * @param model El modelo.
     * @return Fragmento de la vista de la venta o error.
     */
    @GetMapping("/visualizar/{id}")
    public String mostrarVenta(@PathVariable Integer id, Model model) {
        Optional<Venta> ventaOpt = ventaService.obtenerVentaPorId(id);
        if (ventaOpt.isPresent()) {
            model.addAttribute("venta", ventaOpt.get());
            return "fragments/venta_view_modal :: viewContent";
        } else {
            model.addAttribute("mensajeError", "Venta no encontrada.");
            return "error";
        }
    }

    /**
     * Guarda una Venta y sus DetalleVenta, actualizando el stock.
     * @param venta El objeto Venta.
     * @return ResponseEntity con el resultado.
     */
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarVenta(@RequestBody Venta venta) {
        Map<String,String> response = new HashMap<>();

        // 1) Recupera el DNI del usuario autenticado
        String dni = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        // 2) Busca la entidad Usuario por ese DNI
        Usuario realUser = usuarioService
                .obtenerUsuarioPorDni(dni)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado con DNI: " + dni));

        // 3) Asócialo a la venta *antes* de guardar
        venta.setUsuario(realUser);

        try {
            // 4) Guarda la venta y actualiza stock
            ventaService.guardarVenta(venta);

            response.put("status",  "success");
            response.put("message", "Venta guardada exitosamente y stock de productos actualizado!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("status",  "error");
            response.put("message", "Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (RuntimeException e) {
            response.put("status",  "error");
            response.put("message", "Error de negocio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("status",  "error");
            response.put("message", "Error interno al guardar la Venta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Muestra el formulario para crear un nuevo cliente (desde el contexto de Ventas).
     * Reutiliza el fragmento del formulario de cliente.
     * @param model El modelo.
     * @return Fragmento del formulario de cliente.
     */
    @GetMapping("/cliente/nuevo")
    public String mostrarFormularioCrearCliente(Model model) {
        Cliente cliente = new Cliente();
        cliente.setTipoCliente(new TipoCliente()); // Inicializar para evitar NPE en Thymeleaf
        model.addAttribute("cliente", cliente);
        model.addAttribute("tiposCliente", tipoClienteService.listarTiposCliente());
        return "fragments/cliente_form_modal :: formContent";
    }

    /**
     * Guarda un nuevo cliente (desde el contexto de Ventas).
     * Reutiliza la lógica de guardar cliente del ClienteService.
     * @param cliente El objeto Cliente a guardar.
     * @return ResponseEntity con el resultado.
     */
    @PostMapping("/cliente/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarCliente(@RequestBody Cliente cliente) {
        Map<String, String> response = new HashMap<>(); // Declarado una vez aquí
        try {
            clienteService.guardarCliente(cliente);
            response.put("status", "success");
            response.put("message", "Cliente guardado exitosamente!");
            // Opcional: devolver el ID del nuevo cliente si necesitas actualizar el select en el frontend
            if (cliente.getIdCliente() != null) {
                response.put("idCliente", cliente.getIdCliente().toString());
                response.put("nombreCliente", cliente.getRazonSocial() != null && !cliente.getRazonSocial().trim().isEmpty() ? cliente.getRazonSocial() : (cliente.getNombre() + " " + cliente.getApPaterno() + " " + cliente.getApMaterno()));
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al guardar el cliente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verifica si un DNI ya existe.
     * @param dni DNI a verificar.
     * @param idCliente ID del cliente actual (para exclusión).
     * @return JSON con "exists": true/false.
     */
    @GetMapping("/checkDni")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkDni(@RequestParam String dni,
                                                         @RequestParam(required = false) Integer idCliente) {
        Map<String, Boolean> response = new HashMap<>(); // Declarado una vez aquí
        boolean exists = clienteService.existsByDniExcludingCurrent(dni, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si un RUC ya existe.
     * @param ruc RUC a verificar.
     * @param idCliente ID del cliente actual (para exclusión).
     * @return JSON con "exists": true/false.
     */
    @GetMapping("/checkRuc")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkRuc(@RequestParam String ruc,
                                                         @RequestParam(required = false) Integer idCliente) {
        Map<String, Boolean> response = new HashMap<>(); // Declarado una vez aquí
        boolean exists = clienteService.existsByRucExcludingCurrent(ruc, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela (cambia de estado a inactivo) una Venta y revierte el stock de los productos.
     * @param id El ID de la venta.
     * @return ResponseEntity con el resultado.
     */
    // METODO ACTUALIZADO PARA CANCELAR VENTA
    @PostMapping("/cancelar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cancelarVenta(@PathVariable("id") Integer id,
                                                             @RequestBody Map<String, String> requestBody) {
        Map<String, String> response = new HashMap<>();
        String adminPassword = requestBody.get("password");

        if (adminPassword == null || adminPassword.isEmpty()) {
            response.put("status", "error");
            response.put("message", "La contraseña del administrador es obligatoria para anular la venta.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario currentUser = usuarioService.obtenerUsuarioPorDni(currentUserName)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario autenticado no encontrado."));

            // *** CAMBIO AQUÍ: Usar getRolUsuario().getTipoRol() ***
            // Verificar si el usuario tiene el rol de ADMINISTRADOR
            boolean isAdmin = currentUser.getRolUsuario() != null &&
                    "ADMINISTRADOR".equalsIgnoreCase(currentUser.getRolUsuario().getTipoRol());

            if (!isAdmin) {
                response.put("status", "error");
                response.put("message", "Solo los usuarios con rol de ADMINISTRADOR pueden anular ventas.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // *** CAMBIO AQUÍ: Usar getContrasena() ***
            // Validar la contraseña del usuario administrador
            if (!passwordEncoder.matches(adminPassword, currentUser.getContrasena())) {
                response.put("status", "error");
                response.put("message", "Contraseña de administrador incorrecta.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            ventaService.cancelarVenta(id, currentUser);

            response.put("status", "success");
            response.put("message", "Venta cancelada exitosamente y stock de productos revertido!");
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            response.put("status", "error");
            response.put("message", "Error al cancelar la Venta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Error de negocio al cancelar la Venta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (UsernameNotFoundException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al cancelar la Venta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

// --- Nuevos Endpoints para Reportes de Ventas ---

    /**
     * Muestra el modal con los filtros para generar el reporte de ventas.
     * @param model El modelo.
     * @return Fragmento del formulario de reporte de ventas.
     */
    @GetMapping("/reporte/modal")
    public String mostrarReporteVentasModal(Model model) {
        model.addAttribute("clientes", clienteService.listarTodosLosClientesActivos()); // Para el select de cliente
        model.addAttribute("usuarios", usuarioService.listarUsuariosActivos()); // Corrected method call
        return "fragments/venta_reporte_modal :: reporteModalContent";
    }

    /**
     * Genera el reporte PDF de ventas con los filtros aplicados.
     * @param filterDTO Objeto DTO con los criterios de filtro.
     * @param response HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si hay un error al generar el documento PDF.
     * @throws IOException Si hay un error de E/S.
     */
    @GetMapping("/reporte/pdf")
    public void generarReportePdf(@ModelAttribute VentaFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        ventaService.generarReportePdf(filterDTO, response);
    }
    /**
     * Genera un comprobante de venta (boleta/factura) en formato PDF para una venta específica.
     * @param id El ID de la venta para la cual se generará el comprobante.
     * @param response HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si hay un error al generar el documento PDF.
     * @throws IOException Si hay un error de E/S.
     */
    @GetMapping("/comprobante/{id}")
    public void generarComprobanteVentaPdf(@PathVariable Integer id, HttpServletResponse response) throws DocumentException, IOException {
        ventaService.generarComprobanteVentaPdf(id, response);
    }

}
