package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Service.DocumentoSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/documento-venta") // Ruta base para los documentos de venta
public class DocumentoVentaController {

    @Autowired
    private DocumentoSequenceService documentoSequenceService;

    // Endpoint para predecir el siguiente número de documento de venta
    @GetMapping("/next-num-documento")
    public ResponseEntity<Map<String, String>> getNextDocumentoVentaNumber(@RequestParam("tipoDocumento") String tipoDocumento) {
        Map<String, String> response = new HashMap<>();
        try {
            String nextNumDocumento = documentoSequenceService.getNextDocumentNumber(tipoDocumento);
            response.put("status", "success");
            response.put("nextNumDocumento", nextNumDocumento);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error al predecir el número de documento: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Aquí puedes añadir otros endpoints para gestionar documentos de venta (guardar, ver, etc.)
}
