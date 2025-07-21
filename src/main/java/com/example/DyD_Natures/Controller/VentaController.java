package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.VentaFilterDTO;
import com.example.DyD_Natures.Model.*;
import com.example.DyD_Natures.Service.*;
import com.itextpdf.text.DocumentException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

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
    private ClienteService clienteService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private IgvService igvService;
    @Autowired
    private TipoClienteService tipoClienteService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String listarVentas(HttpServletRequest request, Model model) {
        try {
            model.addAttribute("currentUri", request.getRequestURI());
            model.addAttribute("ventas", ventaService.listarVentas());
            // Añadir clientes y usuarios al modelo para el Select2 del filtro
            model.addAttribute("clientes", clienteService.listarTodosLosClientesActivos());
            model.addAttribute("usuarios", usuarioService.listarUsuariosActivos()); // Asegúrate de tener este método en UsuarioService

            return "venta";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar la página de Ventas: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Venta> getAllVentasJson() {
        return ventaService.listarVentas();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Venta venta = new Venta();
        venta.setCliente(new Cliente());
        venta.setIgvEntity(new Igv());
        venta.setDetalleVentas(new ArrayList<>());

        model.addAttribute("venta", venta);
        model.addAttribute("clientes", clienteService.listarTodosLosClientesActivos());
        model.addAttribute("productos", productoService.listarSoloProductosActivos());
        model.addAttribute("igvs", igvService.listarSoloIgvActivos());
        return "fragments/venta_form_modal :: formContent";
    }

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

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarVenta(@RequestBody Venta venta) {
        Map<String,String> response = new HashMap<>();

        String dni = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Usuario realUser = usuarioService
                .obtenerUsuarioPorDni(dni)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado con DNI: " + dni));

        venta.setUsuario(realUser);

        try {
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

    @GetMapping("/cliente/nuevo")
    public String mostrarFormularioCrearCliente(Model model) {
        Cliente cliente = new Cliente();
        cliente.setTipoCliente(new TipoCliente());
        model.addAttribute("cliente", cliente);
        model.addAttribute("tiposCliente", tipoClienteService.listarTiposCliente());
        return "fragments/cliente_form_modal :: formContent";
    }

    @PostMapping("/cliente/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarCliente(@RequestBody Cliente cliente) {
        Map<String, String> response = new HashMap<>();
        try {
            clienteService.guardarCliente(cliente);
            response.put("status", "success");
            response.put("message", "Cliente guardado exitosamente!");
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

    @GetMapping("/checkDni")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkDni(@RequestParam String dni,
                                                         @RequestParam(required = false) Integer idCliente) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = clienteService.existsByDniExcludingCurrent(dni, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkRuc")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkRuc(@RequestParam String ruc,
                                                         @RequestParam(required = false) Integer idCliente) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = clienteService.existsByRucExcludingCurrent(ruc, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

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

            List<Usuario> adminUsers = usuarioService.obtenerUsuariosPorTipoRol("ADMINISTRADOR");

            if (adminUsers.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No se encontró ningún usuario con rol de ADMINISTRADOR para validar la contraseña.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            boolean passwordMatches = false;
            for (Usuario admin : adminUsers) {
                if (passwordEncoder.matches(adminPassword, admin.getContrasena())) {
                    passwordMatches = true;
                    break;
                }
            }

            if (!passwordMatches) {
                response.put("status", "error");
                response.put("message", "Contraseña de administrador incorrecta.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Venta venta = ventaService.obtenerVentaPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("La venta con ID " + id + " no existe."));

            if (venta.getTurnoCaja() == null || Boolean.FALSE.equals(venta.getTurnoCaja().getEstado())) {
                response.put("status", "error");
                response.put("message", "No se puede anular la venta porque la caja ya ha sido cerrada o no existe.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }


            ventaService.cancelarVenta(id, currentUser);

            response.put("status", "success");
            response.put("message", "Venta cancelada exitosamente y stock de productos revertido.");
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

    @GetMapping("/reporte/pdf")
    public void generarReportePdf(@ModelAttribute VentaFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        ventaService.generarReportePdf(filterDTO, response);
    }

    @GetMapping("/comprobante/{id}")
    public void generarComprobanteVentaPdf(@PathVariable Integer id, HttpServletResponse response) throws DocumentException, IOException {
        ventaService.generarComprobanteVentaPdf(id, response);
    }
    // Nuevo endpoint para buscar ventas con filtros para la tabla
    @GetMapping("/buscar")
    @ResponseBody
    public List<Venta> buscarVentas(@ModelAttribute VentaFilterDTO filterDTO) {
        // La lógica para determinar si es admin o usuario ya está en VentaService
        // y se aplicará automáticamente a la Specification si es necesario filtrar por usuario.
        return ventaService.buscarVentasPorFiltros(filterDTO);
    }
}
