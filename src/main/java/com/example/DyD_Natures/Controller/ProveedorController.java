package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate; // Asegúrate de que esta importación esté presente si usas LocalDate
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/proveedores")
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
        // Asegúrate de que este fragmento existe y es correcto
        return "fragments/proveedores_form_modal :: formContent";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Proveedor> proveedorOpt = proveedorService.obtenerProveedorPorId(id);
        if (proveedorOpt.isPresent()) {
            model.addAttribute("proveedor", proveedorOpt.get());
            // Asegúrate de que este fragmento existe y es correcto
            return "fragments/proveedores_form_modal :: formContent";
        }
        // En caso de no encontrarlo, puedes redirigir a un error o crear uno nuevo vacío
        model.addAttribute("proveedor", new Proveedor());
        return "fragments/proveedores_form_modal :: formContent";
    }

    /**
     * Guarda un proveedor nuevo o actualiza uno existente.
     * Recibe los datos del proveedor como un objeto JSON en el cuerpo de la solicitud.
     * @param proveedor El objeto Proveedor recibido del frontend (JSON).
     * @return ResponseEntity con el estado de la operación y un mensaje.
     */
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarProveedor(@RequestBody Proveedor proveedor) { // <--- ¡AQUÍ ESTÁ EL CAMBIO CLAVE!
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones básicas (estas validaciones ahora deberían recibir los datos correctos del JSON)
            if (proveedor.getRuc() == null || proveedor.getRuc().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El RUC es obligatorio.");
                // Puedes añadir un HttpStatus.BAD_REQUEST para indicar un error de cliente
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
                return ResponseEntity.badRequest().body(response); // O HttpStatus.CONFLICT (409) para indicar duplicado
            }

            // Si las validaciones pasan, procede a guardar
            proveedorService.guardarProveedor(proveedor);
            response.put("status", "success");
            response.put("message", "Proveedor guardado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            response.put("status", "error");
            response.put("message", "Error al guardar el proveedor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Realiza una eliminación lógica (cambio de estado) de un proveedor.
     * @param id El ID del proveedor a inactivar.
     * @return ResponseEntity con el estado de la operación y un mensaje.
     */
    @PostMapping("/inactivar/{id}") // Cambiado a POST, como lo usas en el frontend para inactivar
    @ResponseBody
    public ResponseEntity<Map<String, String>> inactivarProveedor(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            proveedorService.eliminarProveedor(id); // Este método ya cambia el estado a 2 (inactivo/eliminado lógico)
            response.put("status", "success");
            response.put("message", "Proveedor inactivado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al inactivar el proveedor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para verificar la unicidad del RUC.
     * @param ruc El número de RUC a verificar.
     * @param idProveedor El ID del proveedor a excluir de la búsqueda (null para nuevas creaciones).
     * @return ResponseEntity con un mapa que indica si el RUC ya existe.
     */
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
