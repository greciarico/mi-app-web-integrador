package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;
    /**
     * Muestra la vista principal de productos.
     * Carga los productos activos/inactivos (no eliminados) para que el JavaScript los filtre.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (productos.html).
     */
    @GetMapping
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.listarProductosActivos());
        return "productos";
    }

    /**
     * Endpoint para obtener todos los productos activos/inactivos (no eliminados) en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de productos activos/inactivos (no eliminados).
     */
    @GetMapping("/all")
    @ResponseBody
    public List<Producto> getAllProductsJson() {
        return productoService.listarProductosActivos();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Producto producto = new Producto();
        producto.setCategoria(new Categoria());
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", productoService.listarCategorias());
        return "fragments/productos_form_modal :: formContent";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Producto> productoOpt = productoService.obtenerProductoPorId(id);
        if (productoOpt.isPresent()) {
            model.addAttribute("producto", productoOpt.get());
            model.addAttribute("categorias", productoService.listarCategorias());
            return "fragments/productos_form_modal :: formContent";
        }
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", productoService.listarCategorias());
        return "fragments/productos_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarProducto(@ModelAttribute Producto producto) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones básicas, por ejemplo, que el nombre o código no estén vacíos
            if (producto.getNombre() == null || producto.getNombre().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El nombre del producto es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }
            if (producto.getDescripcion() == null || producto.getDescripcion().isEmpty()) {
                response.put("status", "error");
                response.put("message", "La descripción del producto es obligatoria.");
                return ResponseEntity.badRequest().body(response);
            }
            if (producto.getPrecio1() == null || producto.getPrecio1().compareTo(BigDecimal.ZERO) <= 0) {
                response.put("status", "error");
                response.put("message", "El Precio 1 es obligatorio y debe ser mayor que cero.");
                return ResponseEntity.badRequest().body(response);
            }
            if (producto.getPrecio2() == null || producto.getPrecio2().compareTo(BigDecimal.ZERO) <= 0) {
                response.put("status", "error");
                response.put("message", "El Precio 2 es obligatorio y debe ser mayor que cero.");
                return ResponseEntity.badRequest().body(response);
            }
            if (producto.getStock() == null || producto.getStock() < 0) {
                response.put("status", "error");
                response.put("message", "El Stock es obligatorio y no puede ser negativo.");
                return ResponseEntity.badRequest().body(response);
            }
            if (producto.getCategoria() == null || producto.getCategoria().getIdCategoria() == null) {
                response.put("status", "error");
                response.put("message", "La categoría es obligatoria.");
                return ResponseEntity.badRequest().body(response);
            }


            if (producto.getCategoria() != null && producto.getCategoria().getIdCategoria() != null) {
                Optional<Categoria> categoriaOpt = productoService.obtenerCategoriaPorId(producto.getCategoria().getIdCategoria());
                if (categoriaOpt.isPresent()) {
                    producto.setCategoria(categoriaOpt.get());
                } else {
                    response.put("status", "error");
                    response.put("message", "Categoría seleccionada inválida.");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Si es un nuevo producto, establece el estado por defecto a 1 (Activo) como Byte
            if (producto.getIdProducto() == null) {
                producto.setEstado((byte) 1);
            }

            productoService.guardarProducto(producto);
            response.put("status", "success");
            response.put("message", "Producto guardado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al guardar el producto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarProducto(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            productoService.eliminarProducto(id);
            response.put("status", "success");
            response.put("message", "Producto eliminado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al eliminar el producto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
