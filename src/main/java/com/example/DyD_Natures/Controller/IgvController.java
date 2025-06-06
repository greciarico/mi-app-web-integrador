package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Igv;
import com.example.DyD_Natures.Service.IgvService;
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
@RequestMapping("/igv")
public class IgvController {

    @Autowired
    private IgvService igvService;

    @GetMapping
    public String listarIgv(Model model) {
        model.addAttribute("igvs", igvService.listarIgvActivos());
        return "igv";
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Igv> getAllIgvJson() {
        return igvService.listarIgvActivos();
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
    public ResponseEntity<Map<String, String>> guardarIgv(@RequestBody Igv igv) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones de negocio en el backend (se recomienda hacerlas aquí también)
            if (igv.getIgv() == null || igv.getIgv().compareTo(BigDecimal.ZERO) <= 0) {
                response.put("status", "error");
                response.put("message", "El valor del IGV es obligatorio y debe ser mayor que cero.");
                return ResponseEntity.badRequest().body(response);
            }
            // Asegurarse de que el BigDecimal tenga la escala correcta (2 decimales)
            // Esto es importante si el frontend envía un float que no es exactamente .XX
            igv.setIgv(igv.getIgv().setScale(2, BigDecimal.ROUND_HALF_UP));

            // Si es nuevo, establecer estado y fecha de registro
            if (igv.getIdIgv() == null) {
                igv.setEstado((byte) 1); // Por defecto Activo (Byte)
                igv.setFechaRegistro(LocalDate.now());
            } else { // Si es edición, mantener la fecha de registro existente
                Optional<Igv> existingIgvOpt = igvService.obtenerIgvPorId(igv.getIdIgv());
                existingIgvOpt.ifPresent(existingIgv -> igv.setFechaRegistro(existingIgv.getFechaRegistro()));
            }

            igvService.guardarIgv(igv);
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
