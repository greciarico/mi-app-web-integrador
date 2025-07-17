package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Empresa;
import com.example.DyD_Natures.Service.EmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/informacion-empresa")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;

    @GetMapping
    public String mostrarInformacionEmpresa(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        Optional<Empresa> empresaOpt = empresaService.obtenerInformacionEmpresa();
        Empresa empresa = empresaOpt.orElse(new Empresa()); // Si no existe, crea una nueva instancia
        // CAMBIO CLAVE: Si es una nueva empresa, inicializa fechaRegistro aquí para el frontend
        if (empresa.getIdEmpresa() == null) {
            empresa.setFechaRegistro(LocalDate.now()); // Establece la fecha actual por defecto
        }
        model.addAttribute("empresa", empresa);
        return "informacion_empresa";
    }

    @GetMapping("/data")
    @ResponseBody
    public Empresa getEmpresaDataJson() {
        return empresaService.obtenerInformacionEmpresa().orElse(new Empresa());
    }

    @GetMapping("/form")
    public String mostrarFormularioEmpresa(Model model) {
        Optional<Empresa> empresaOpt = empresaService.obtenerInformacionEmpresa();
        Empresa empresa = empresaOpt.orElse(new Empresa());
        // CAMBIO CLAVE: Si es una nueva empresa, inicializa fechaRegistro aquí para el fragmento del formulario
        if (empresa.getIdEmpresa() == null) {
            empresa.setFechaRegistro(LocalDate.now()); // Asegura que siempre haya una fecha para el th:value
        }
        model.addAttribute("empresa", empresa);
        return "fragments/informacion_empresa_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarEmpresa(@RequestBody Empresa empresa) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validaciones básicas en el backend
            if (empresa.getRuc() == null || empresa.getRuc().isEmpty() || !empresa.getRuc().matches("^\\d{11}$")) {
                response.put("status", "error");
                response.put("message", "El RUC es obligatorio y debe tener 11 dígitos numéricos.");
                return ResponseEntity.badRequest().body(response);
            }
            if (empresa.getNombreComercial() == null || empresa.getNombreComercial().isEmpty()) {
                response.put("status", "error");
                response.put("message", "El Nombre Comercial es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }
            if (empresa.getRazonSocial() == null || empresa.getRazonSocial().isEmpty()) {
                response.put("status", "error");
                response.put("message", "La Razón Social es obligatoria.");
                return ResponseEntity.badRequest().body(response);
            }
            if (empresa.getDireccion() == null || empresa.getDireccion().isEmpty()) {
                response.put("status", "error");
                response.put("message", "La Dirección es obligatoria.");
                return ResponseEntity.badRequest().body(response);
            }
            if (empresa.getTelefono() != null && !empresa.getTelefono().isEmpty() && !empresa.getTelefono().matches("^\\d{9}$")) {
                response.put("status", "error");
                response.put("message", "El teléfono debe tener 9 dígitos numéricos.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validación de RUC único
            if (empresaService.existsByRucExcludingCurrent(empresa.getRuc())) {
                response.put("status", "error");
                response.put("message", "El RUC ya está registrado para otra empresa.");
                return ResponseEntity.badRequest().body(response);
            }

            empresaService.guardarEmpresa(empresa);
            response.put("status", "success");
            response.put("message", "Información de la empresa guardada exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al guardar la información de la empresa: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/checkRuc")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkRuc(@RequestParam String ruc) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = empresaService.existsByRucExcludingCurrent(ruc);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}
