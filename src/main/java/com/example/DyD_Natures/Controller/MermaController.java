package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.MermaFilterDTO; // Importar
import com.example.DyD_Natures.Model.Merma;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Service.MermaService;
import com.example.DyD_Natures.Service.ProductoService;
import com.itextpdf.text.*; // Importar iText
import com.itextpdf.text.pdf.PdfPCell; // Importar iText
import com.itextpdf.text.pdf.PdfPTable; // Importar iText
import com.itextpdf.text.pdf.PdfWriter; // Importar iText
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse; // Importar
import java.io.IOException; // Importar
import java.time.LocalDate;
import java.time.format.DateTimeFormatter; // Importar
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
    public String listarMermas(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
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
        return productoService.listarSoloProductosActivos();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("merma", new Merma());
        model.addAttribute("productos", productoService.listarSoloProductosActivos()); // Productos para el select
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
        model.addAttribute("productos", productoService.listarSoloProductosActivos()); // Productos para el select
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
    // ===============================================
    // NUEVOS ENDPOINTS PARA EL REPORTE DE MERMAS
    // ===============================================

    /**
     * Muestra el fragmento del modal de filtros para el reporte de mermas.
     * También carga los productos activos para el dropdown de filtro.
     * @param model El modelo para pasar datos a la vista.
     * @return La ruta al fragmento del modal.
     */
    @GetMapping("/reporte/modal")
    public String obtenerModalReporteMermas(Model model) {
        model.addAttribute("productos", productoService.listarSoloProductosActivos());
        return "fragments/reporte_mermas_modal :: reporteModalContent";
    }

    /**
     * Genera y devuelve un informe PDF de registros de Merma basado en los filtros.
     * @param filterDTO DTO con los criterios de filtrado para el reporte.
     * @param response Objeto HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si ocurre un error al crear el documento PDF.
     * @throws IOException Si ocurre un error de E/S.
     */
    @GetMapping("/reporte/pdf")
    public void generarReporteMermaPdf(@ModelAttribute MermaFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=reporte_mermas.pdf";
        response.setHeader(headerKey, headerValue);

        List<Merma> mermas = mermaService.buscarMermasPorFiltros(filterDTO);

        Document document = new Document(PageSize.A4.rotate()); // A4 horizontal
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Paragraph title = new Paragraph("Reporte de Registros de Merma", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Mostrar filtros aplicados
        Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        StringBuilder filtrosAplicados = new StringBuilder("Filtros Aplicados:\n");

        if (filterDTO.getNombreProducto() != null && !filterDTO.getNombreProducto().isEmpty()) {
            filtrosAplicados.append("- Nombre de Producto: ").append(filterDTO.getNombreProducto()).append("\n");
        }
        if (filterDTO.getDescripcionMerma() != null && !filterDTO.getDescripcionMerma().isEmpty()) {
            filtrosAplicados.append("- Descripción de Merma: ").append(filterDTO.getDescripcionMerma()).append("\n");
        }
        if (filterDTO.getIdProducto() != null) {
            Optional<Producto> productoOpt = productoService.obtenerProductoPorId(filterDTO.getIdProducto());
            productoOpt.ifPresent(producto -> filtrosAplicados.append("- Producto Específico: ").append(producto.getNombre()).append("\n"));
        }
        if (filterDTO.getFechaRegistroStart() != null || filterDTO.getFechaRegistroEnd() != null) {
            filtrosAplicados.append("- Fecha de Registro: ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            if (filterDTO.getFechaRegistroStart() != null) filtrosAplicados.append("Desde ").append(filterDTO.getFechaRegistroStart().format(formatter)).append(" ");
            if (filterDTO.getFechaRegistroEnd() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getFechaRegistroEnd().format(formatter));
            filtrosAplicados.append("\n");
        }
        if (filterDTO.getCantidadMin() != null || filterDTO.getCantidadMax() != null) {
            filtrosAplicados.append("- Cantidad: ");
            if (filterDTO.getCantidadMin() != null) filtrosAplicados.append("Desde ").append(filterDTO.getCantidadMin()).append(" ");
            if (filterDTO.getCantidadMax() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getCantidadMax());
            filtrosAplicados.append("\n");
        }

        if (filtrosAplicados.toString().equals("Filtros Aplicados:\n")) {
            filtrosAplicados.append("- Ninguno (Reporte Completo)\n");
        }

        Paragraph pFiltros = new Paragraph(filtrosAplicados.toString(), fontFilters);
        pFiltros.setAlignment(Paragraph.ALIGN_LEFT);
        pFiltros.setSpacingAfter(10);
        document.add(pFiltros);

        // Crear la tabla PDF
        PdfPTable table = new PdfPTable(5); // 5 columnas: ID, Producto, Cantidad, Fecha Registro, Descripción
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        float[] columnWidths = {0.5f, 1.5f, 0.8f, 1f, 2.5f}; // Ajusta anchos
        table.setWidths(columnWidths);

        PdfPCell cell;
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.BLACK);

        String[] headers = {"ID", "Producto", "Cantidad", "F. Registro", "Descripción"};
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        if (mermas.isEmpty()) {
            cell = new PdfPCell(new Phrase("No se encontraron registros de Merma con los filtros aplicados.", fontContent));
            cell.setColspan(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Merma merma : mermas) {
                table.addCell(new Phrase(String.valueOf(merma.getIdMerma()), fontContent));
                table.addCell(new Phrase(merma.getProducto() != null ? merma.getProducto().getNombre() : "N/A", fontContent));
                table.addCell(new Phrase(String.valueOf(merma.getCantidad()), fontContent));
                table.addCell(new Phrase(merma.getFechaRegistro() != null ? merma.getFechaRegistro().format(formatter) : "N/A", fontContent));
                table.addCell(new Phrase(merma.getDescripcion() != null && !merma.getDescripcion().isEmpty() ? merma.getDescripcion() : "Sin descripción", fontContent));
            }
        }

        document.add(table);
        document.close();
    }
}
