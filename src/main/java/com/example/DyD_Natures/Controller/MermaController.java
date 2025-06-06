package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Merma;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Service.MermaService;
import com.example.DyD_Natures.Service.ProductoService; // Para obtener productos para el dropdown
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
@RequestMapping("/merma") // CAMBIADO: Ruta base para Merma ahora es /merma
public class MermaController {

    @Autowired
    private MermaService mermaService;

    @Autowired
    private ProductoService productoService; // Para poblar el select de productos

    /**
     * Muestra la vista principal de Merma.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (merma.html).
     */
    @GetMapping
    public String listarMermas(Model model) {
        model.addAttribute("mermas", mermaService.listarMermas());
        return "merma"; // Asegúrate de que esto apunta a tu archivo merma.html
    }

    /**
     * Endpoint para obtener todos los registros de Merma en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de registros de Merma.
     */
    @GetMapping("/all")
    @ResponseBody
    public List<Merma> getAllMermasJson() {
        return mermaService.listarMermas();
    }

    /**
     * Endpoint para obtener todos los productos activos en formato JSON.
     * Usado por el JavaScript para poblar el dropdown de productos en el formulario de Merma.
     * @return Una lista de productos activos.
     */
    @GetMapping("/productos")
    @ResponseBody
    public List<Producto> getProductosForDropdown() {
        return productoService.listarProductosActivos();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("merma", new Merma());
        model.addAttribute("productos", productoService.listarProductosActivos()); // Productos para el select
        return "fragments/merma_form_modal :: formContent"; // Asumiendo un fragmento para el formulario
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Merma> mermaOpt = mermaService.obtenerMermaPorId(id);
        if (mermaOpt.isPresent()) {
            model.addAttribute("merma", mermaOpt.get());
        } else {
            model.addAttribute("merma", new Merma()); // En caso de no encontrarlo
        }
        model.addAttribute("productos", productoService.listarProductosActivos()); // Productos para el select
        return "fragments/merma_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarMerma(@RequestBody Merma merma) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones básicas de Merma en el backend
            if (merma.getCantidad() == null || merma.getCantidad() <= 0) {
                response.put("status", "error");
                response.put("message", "La cantidad de merma debe ser un número entero positivo.");
                return ResponseEntity.badRequest().body(response);
            }
            if (merma.getProducto() == null || merma.getProducto().getIdProducto() == null) {
                response.put("status", "error");
                response.put("message", "Debe seleccionar un producto para la merma.");
                return ResponseEntity.badRequest().body(response);
            }
            // La fecha de registro se establecerá automáticamente en el servicio para nuevos registros.
            // La descripción es opcional.

            mermaService.guardarMerma(merma);
            response.put("status", "success");
            response.put("message", "Registro de Merma guardado exitosamente y stock del producto actualizado!");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) { // Captura las excepciones de stock insuficiente, etc.
            response.put("status", "error");
            response.put("message", "Error al guardar el registro de Merma: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // BAD_REQUEST para errores de negocio
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al guardar el registro de Merma: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarMerma(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            mermaService.eliminarMerma(id); // Llama al método de eliminación física y reposición de stock
            response.put("status", "success");
            response.put("message", "Registro de Merma eliminado exitosamente y stock del producto repuesto!");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", "Error al eliminar el registro de Merma: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al eliminar el registro de Merma: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}