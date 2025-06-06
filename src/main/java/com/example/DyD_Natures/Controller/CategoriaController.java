package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    /**
     * Muestra la vista principal de categorías.
     * Carga las categorías activas/inactivas (no eliminadas) para que el JavaScript las filtre.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (categorias.html).
     */
    @GetMapping
    public String listarCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.listarCategoriasActivas());
        return "categorias";
    }

    /**
     * Endpoint para obtener todas las categorías activas/inactivas (no eliminadas) en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de categorías activas/inactivas (no eliminadas).
     */
    @GetMapping("/all")
    @ResponseBody
    public List<Categoria> getAllCategoriasJson() {
        return categoriaService.listarCategoriasActivas();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("categoria", new Categoria());
        return "fragments/categorias_form_modal :: formContent";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Categoria> categoriaOpt = categoriaService.obtenerCategoriaPorId(id);
        if (categoriaOpt.isPresent()) {
            model.addAttribute("categoria", categoriaOpt.get());
            return "fragments/categorias_form_modal :: formContent";
        }
        model.addAttribute("categoria", new Categoria());
        return "fragments/categorias_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarCategoria(@ModelAttribute Categoria categoria) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones básicas
            if (categoria.getNombreCategoria() == null || categoria.getNombreCategoria().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El nombre de la categoría es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validación de nombre único
            if (categoriaService.existsByNombreCategoriaExcludingId(categoria.getNombreCategoria(), categoria.getIdCategoria())) {
                response.put("status", "error");
                response.put("message", "Ya existe una categoría con este nombre.");
                return ResponseEntity.badRequest().body(response);
            }

            // Si es nueva, establece el estado por defecto a 1 (Activo)
            if (categoria.getIdCategoria() == null) {
                categoria.setEstado((byte) 1);
            }

            categoriaService.guardarCategoria(categoria);
            response.put("status", "success");
            response.put("message", "Categoría guardada exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al guardar la categoría: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarCategoria(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            categoriaService.eliminarCategoria(id);
            response.put("status", "success");
            response.put("message", "Categoría eliminada exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al eliminar la categoría: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Endpoint para verificar la unicidad del nombre de categoría
    @GetMapping("/checkNombreCategoria")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkNombreCategoria(@RequestParam String nombreCategoria,
                                                                     @RequestParam(required = false) Integer idCategoria) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = categoriaService.existsByNombreCategoriaExcludingId(nombreCategoria, idCategoria);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}