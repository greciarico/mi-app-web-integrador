package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/proveedores") // ¡CAMBIO AQUÍ! Ruta base ahora es /proveedores
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    /**
     * Muestra la vista principal de proveedores.
     * Carga los proveedores activos/inactivos (no eliminados) para que el JavaScript los filtre.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (proveedores.html).
     */
    @GetMapping
    public String listarProveedores(Model model) {
        model.addAttribute("proveedores", proveedorService.listarProveedoresActivos());
        return "proveedores"; // Asegúrate de que esto apunta a tu archivo proveedores.html
    }

    /**
     * Endpoint para obtener todos los proveedores activos/inactivos (no eliminados) en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de proveedores activos/inactivos (no eliminados).
     */
    @GetMapping("/all")
    @ResponseBody
    public List<Proveedor> getAllProveedoresJson() {
        return proveedorService.listarProveedoresActivos();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "fragments/proveedores_form_modal :: formContent"; // Asumiendo un fragmento para el formulario
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Proveedor> proveedorOpt = proveedorService.obtenerProveedorPorId(id);
        if (proveedorOpt.isPresent()) {
            model.addAttribute("proveedor", proveedorOpt.get());
            return "fragments/proveedores_form_modal :: formContent";
        }
        model.addAttribute("proveedor", new Proveedor()); // En caso de no encontrarlo
        return "fragments/proveedores_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarProveedor(@ModelAttribute Proveedor proveedor) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones básicas
            if (proveedor.getRuc() == null || proveedor.getRuc().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El RUC es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }
            if (proveedor.getNombreComercial() == null || proveedor.getNombreComercial().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El Nombre Comercial es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }
            if (proveedor.getRazonSocial() == null || proveedor.getRazonSocial().isEmpty()) {
                response.put("status", "error");
                response.put("message", "La Razón Social es obligatoria.");
                return ResponseEntity.badRequest().body(response);
            }
            if (proveedor.getDireccion() == null || proveedor.getDireccion().isEmpty()) {
                response.put("status", "error");
                response.put("message", "La Dirección es obligatoria.");
                return ResponseEntity.badRequest().body(response);
            }
            // Puedes añadir más validaciones para teléfono y correo si son obligatorios en tu negocio

            // Validación de RUC único
            Optional<Proveedor> existingProveedorByRuc = proveedorService.obtenerProveedorPorRuc(proveedor.getRuc());
            if (existingProveedorByRuc.isPresent() && (proveedor.getIdProveedor() == null || !existingProveedorByRuc.get().getIdProveedor().equals(proveedor.getIdProveedor()))) {
                response.put("status", "error");
                response.put("message", "El RUC ya está registrado.");
                return ResponseEntity.badRequest().body(response);
            }

            proveedorService.guardarProveedor(proveedor);
            response.put("status", "success");
            response.put("message", "Proveedor guardado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al guardar el proveedor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarProveedor(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            proveedorService.eliminarProveedor(id); // Llama al método de soft delete
            response.put("status", "success");
            response.put("message", "Proveedor eliminado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al eliminar el proveedor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Endpoint para verificar la unicidad del RUC
    @GetMapping("/checkRuc")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkRuc(@RequestParam String ruc,
                                                         @RequestParam(required = false) Integer idProveedor) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = proveedorService.existsByRucExcludingId(ruc, idProveedor);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}

