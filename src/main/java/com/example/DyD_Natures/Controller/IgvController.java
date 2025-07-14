package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Igv;
import com.example.DyD_Natures.Service.IgvService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/tasa")
public class IgvController {

    @Autowired
    private IgvService igvService;

    @GetMapping
    public String listarIgv(Model model) {
        model.addAttribute("igvs", igvService.listarSoloIgvActivos());
        return "igv";
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Igv> getAllTasasJson() {
        return igvService.listarIgvExcludingDeleted();
    }

    // —> JSON solo activos, para tu modal de venta
    @GetMapping("/activos")
    @ResponseBody
    public List<Igv> getActiveTasasJson() {
        return igvService.listarSoloIgvActivos();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("igv", new Igv());
        return "fragments/igv_form_modal :: formContent";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Igv> igvOpt = igvService.obtenerIgvPorId(id);
        if (igvOpt.isPresent()) {
            model.addAttribute("igv", igvOpt.get());
            return "fragments/igv_form_modal :: formContent";
        }
        model.addAttribute("igv", new Igv());
        return "fragments/igv_form_modal :: formContent";
    }

    // CAMBIO CLAVE: Ahora acepta @RequestBody para el JSON del frontend
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarIgv(@RequestBody Igv igvDesdeFrontend) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validación de la tasa (esto ya lo tienes y está bien)
            if (igvDesdeFrontend.getTasa() == null || igvDesdeFrontend.getTasa().compareTo(BigDecimal.ZERO) <= 0) {
                response.put("status", "error");
                response.put("message", "El valor del IGV es obligatorio y debe ser un número positivo.");
                return ResponseEntity.badRequest().body(response);
            }

            // --- LÓGICA DE FECHA CORREGIDA ---

            if (igvDesdeFrontend.getIdIgv() == null) { // Es un registro NUEVO
                igvDesdeFrontend.setEstado((byte) 1); // Asignar estado activo por defecto
                igvDesdeFrontend.setFechaRegistro(LocalDate.now()); // El backend asigna la fecha actual. Ignora lo que vino del frontend.

            } else { // Es una EDICIÓN
                // Para evitar que la fecha de registro original se modifique, la recuperamos de la base de datos.
                Igv igvExistente = igvService.obtenerIgvPorId(igvDesdeFrontend.getIdIgv())
                        .orElseThrow(() -> new EntityNotFoundException("No se encontró el IGV para editar con ID: " + igvDesdeFrontend.getIdIgv()));

                // Usamos la fecha que ya existía en la base de datos, ignorando la que pudo venir del frontend.
                igvDesdeFrontend.setFechaRegistro(igvExistente.getFechaRegistro());
            }

            // El resto de la lógica de guardado
            igvService.guardarIgv(igvDesdeFrontend);

            response.put("status", "success");
            response.put("message", "Registro de IGV guardado exitosamente!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al guardar el registro de IGV: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarIgv(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            igvService.eliminarIgv(id);
            response.put("status", "success");
            response.put("message", "Registro de IGV eliminado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al eliminar el registro de IGV: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
