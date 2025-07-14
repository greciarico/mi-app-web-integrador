package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Dto.CategoriaFilterDTO; // Importar
import com.example.DyD_Natures.Service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.itextpdf.text.*; // Importar iText
import com.itextpdf.text.pdf.PdfPCell; // Importar iText
import com.itextpdf.text.pdf.PdfPTable; // Importar iText
import com.itextpdf.text.pdf.PdfWriter; // Importar iText

import jakarta.servlet.http.HttpServletResponse; // Importar
import java.io.IOException; // Importar
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
    // ===============================================
    // NUEVOS ENDPOINTS PARA EL REPORTE DE CATEGORÍAS
    // ===============================================

    /**
     * Muestra el fragmento del modal de filtros para el reporte de categorías.
     * @return La ruta al fragmento del modal.
     */
    @GetMapping("/reporte/modal")
    public String obtenerModalReporteCategorias() {
        return "fragments/reporte_categorias_modal :: reporteModalContent";
    }

    /**
     * Genera y devuelve un informe PDF de categorías basado en los filtros.
     * @param filterDTO DTO con los criterios de filtrado para el reporte.
     * @param response Objeto HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si ocurre un error al crear el documento PDF.
     * @throws IOException Si ocurre un error de E/S.
     */
    @GetMapping("/reporte/pdf")
    public void generarReportePdf(@ModelAttribute CategoriaFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=reporte_categorias.pdf";
        response.setHeader(headerKey, headerValue);

        List<Categoria> categorias = categoriaService.buscarCategoriasPorFiltros(filterDTO);

        Document document = new Document(PageSize.A4); // Tamaño A4, sin rotar
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Paragraph title = new Paragraph("Reporte de Categorías", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Mostrar filtros aplicados
        Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        StringBuilder filtrosAplicados = new StringBuilder("Filtros Aplicados:\n");

        if (filterDTO.getNombreCategoria() != null && !filterDTO.getNombreCategoria().isEmpty()) {
            filtrosAplicados.append("- Nombre de Categoría: ").append(filterDTO.getNombreCategoria()).append("\n");
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

        if (filtrosAplicados.toString().equals("Filtros Aplicados:\n")) {
            filtrosAplicados.append("- Ninguno (Reporte Completo)\n");
        }

        Paragraph pFiltros = new Paragraph(filtrosAplicados.toString(), fontFilters);
        pFiltros.setAlignment(Paragraph.ALIGN_LEFT);
        pFiltros.setSpacingAfter(10);
        document.add(pFiltros);

        PdfPTable table = new PdfPTable(3); // 3 columnas: ID, Nombre Categoría, Estado
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        float[] columnWidths = {0.5f, 3f, 1f}; // Ajusta anchos según necesidad
        table.setWidths(columnWidths);

        PdfPCell cell;
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
        Font fontContentActive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(0, 128, 0)); // Verde
        Font fontContentInactive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(255, 0, 0)); // Rojo

        String[] headers = {"ID", "Nombre Categoría", "Estado"};
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        if (categorias.isEmpty()) {
            cell = new PdfPCell(new Phrase("No se encontraron categorías con los filtros aplicados.", fontContent));
            cell.setColspan(3); // Abarca todas las columnas
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        } else {
            for (Categoria categoria : categorias) {
                table.addCell(new Phrase(String.valueOf(categoria.getIdCategoria()), fontContent));
                table.addCell(new Phrase(categoria.getNombreCategoria(), fontContent));
                String estadoText;
                Font estadoFont;

                if (categoria.getEstado() == 1) {
                    estadoText = "Activo";
                    estadoFont = fontContentActive;
                } else if (categoria.getEstado() == 0) {
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
