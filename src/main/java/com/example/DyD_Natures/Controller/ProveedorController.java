package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.ProveedorFilterDTO;
import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Service.ProveedorService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
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
    public List<Proveedor> getAllProveedoresJson(@RequestParam(required = false) String searchTerm) { // AÑADIR @RequestParam
        // Crear un DTO de filtro para pasar al servicio
        ProveedorFilterDTO filterDTO = new ProveedorFilterDTO();

        // Si se proporciona un término de búsqueda, establecerlo en el DTO
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            filterDTO.setNombreRucRazonSocialCorreo(searchTerm);
        }
        // Por defecto, buscarProveedoresPorFiltros ya excluye los proveedores con estado = 2 (eliminado lógicamente).
        // Si no se proporciona searchTerm, buscarProveedoresPorFiltros devolverá todos los no eliminados.
        return proveedorService.buscarProveedoresPorFiltros(filterDTO);
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
    // --- NUEVOS ENDPOINTS PARA REPORTES ---

    /**
     * Muestra el fragmento del modal con opciones para generar el reporte de proveedores.
     * @param model El modelo para pasar datos a la vista.
     * @return El fragmento HTML del modal de reporte.
     */
    @GetMapping("/reporte/modal")
    public String mostrarReporteModal(Model model) {
        // No hay roles ni otros datos complejos para proveedores como en usuarios,
        // pero si tuvieras otros filtros (ej. por categoría de proveedor), los añadirías aquí.
        return "fragments/reporte_proveedores_modal :: reporteModalContent";
    }

    /**
     * Genera y devuelve un informe PDF de proveedores basado en los filtros.
     * @param filterDTO DTO con los criterios de filtrado para el reporte.
     * @param response Objeto HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si ocurre un error al crear el documento PDF.
     * @throws IOException Si ocurre un error de E/S.
     */
    @GetMapping("/reporte/pdf")
    public void generarReportePdf(@ModelAttribute ProveedorFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=reporte_proveedores.pdf";
        response.setHeader(headerKey, headerValue);

        List<Proveedor> proveedores = proveedorService.buscarProveedoresPorFiltros(filterDTO);

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Paragraph title = new Paragraph("Reporte de Proveedores", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Mostrar filtros aplicados (similar a usuarios)
        Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        StringBuilder filtrosAplicados = new StringBuilder("Filtros Aplicados:\n");

        if (filterDTO.getNombreRucRazonSocialCorreo() != null && !filterDTO.getNombreRucRazonSocialCorreo().isEmpty()) {
            filtrosAplicados.append("- Búsqueda General: ").append(filterDTO.getNombreRucRazonSocialCorreo()).append("\n");
        }
        if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
            filtrosAplicados.append("- Estados: ");
            for (int i = 0; i < filterDTO.getEstados().size(); i++) {
                Byte estado = filterDTO.getEstados().get(i);
                filtrosAplicados.append(estado == 1 ? "Activo" : "Inactivo");
                if (i < filterDTO.getEstados().size() - 1) {
                    filtrosAplicados.append(", ");
                }
            }
            filtrosAplicados.append("\n");
        }
        // NUEVO: Mostrar filtros de fecha en el PDF
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filterDTO.getFechaRegistroDesde() != null) {
            filtrosAplicados.append("- Fecha Registro Desde: ").append(filterDTO.getFechaRegistroDesde().format(formatter)).append("\n");
        }
        if (filterDTO.getFechaRegistroHasta() != null) {
            filtrosAplicados.append("- Fecha Registro Hasta: ").append(filterDTO.getFechaRegistroHasta().format(formatter)).append("\n");
        }


        if (filtrosAplicados.toString().equals("Filtros Aplicados:\n")) {
            filtrosAplicados.append("- Ninguno (Reporte Completo)\n");
        }

        Paragraph pFiltros = new Paragraph(filtrosAplicados.toString(), fontFilters);
        pFiltros.setAlignment(Paragraph.ALIGN_LEFT);
        pFiltros.setSpacingAfter(10);
        document.add(pFiltros);


        PdfPTable table = new PdfPTable(9); // 9 columnas: ID, Fecha Reg, RUC, Nom Comercial, Razon Social, Telefono, Correo, Direccion, Estado
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        float[] columnWidths = {0.5f, 1f, 1f, 1.5f, 1.5f, 0.8f, 1.5f, 2f, 0.7f};
        table.setWidths(columnWidths);

        PdfPCell cell;
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
        Font fontContentActive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(0, 128, 0)); // Verde
        Font fontContentInactive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(255, 0, 0)); // Rojo

        String[] headers = {"ID", "Fecha Reg.", "RUC", "Nombre Comercial", "Razón Social", "Teléfono", "Correo", "Dirección", "Estado"};
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        if (proveedores.isEmpty()) {
            cell = new PdfPCell(new Phrase("No se encontraron proveedores con los filtros aplicados.", fontContent));
            cell.setColspan(9);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        } else {
            // El formatter ya está definido arriba, asegúrate que se el mismo que usas aquí
            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // Esto debería estar solo una vez
            for (Proveedor proveedor : proveedores) {
                table.addCell(new Phrase(String.valueOf(proveedor.getIdProveedor()), fontContent));
                table.addCell(new Phrase(proveedor.getFechaRegistro() != null ? proveedor.getFechaRegistro().format(formatter) : "N/A", fontContent));
                table.addCell(new Phrase(proveedor.getRuc(), fontContent));
                table.addCell(new Phrase(proveedor.getNombreComercial(), fontContent));
                table.addCell(new Phrase(proveedor.getRazonSocial(), fontContent));
                table.addCell(new Phrase(proveedor.getTelefono() != null ? proveedor.getTelefono() : "N/A", fontContent));
                table.addCell(new Phrase(proveedor.getCorreo() != null ? proveedor.getCorreo() : "N/A", fontContent));
                table.addCell(new Phrase(proveedor.getDireccion(), fontContent));
                String estadoText;
                Font estadoFont;

                if (proveedor.getEstado() == 1) {
                    estadoText = "Activo";
                    estadoFont = fontContentActive;
                } else if (proveedor.getEstado() == 0) {
                    estadoText = "Inactivo";
                    estadoFont = fontContentInactive;
                } else {
                    estadoText = "Desconocido";
                    estadoFont = fontContent; // Default to black if unknown
                }
                table.addCell(new Phrase(estadoText, estadoFont));              }
        }

        document.add(table);
        document.close();
    }
}
