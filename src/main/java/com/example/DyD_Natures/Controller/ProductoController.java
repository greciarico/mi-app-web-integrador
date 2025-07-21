package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.ProductoFilterDTO;
import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Service.ProductoService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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
    public String listarProductos(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
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
    
    @GetMapping("/activos")
    @ResponseBody
    public List<Producto> getActiveProductsJson() {
        return productoService.listarSoloProductosActivos();
    }

    /**
     * Nuevo endpoint para buscar/filtrar productos en la tabla principal vía AJAX.
     * Recibe los filtros y devuelve una lista de productos en formato JSON.
     * @param filterDTO DTO con los criterios de filtrado.
     * @return Lista de productos que coinciden con los filtros.
     */
    @GetMapping("/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(@ModelAttribute ProductoFilterDTO filterDTO) {
        return productoService.buscarProductosPorFiltros(filterDTO);
    }

    /**
     * Genera y devuelve un informe PDF de productos basado en los filtros.
     * Este endpoint ya existía y está bien. Ahora se llamará directamente desde el JS.
     * @param filterDTO DTO con los criterios de filtrado para el reporte.
     * @param response Objeto HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si ocurre un error al crear el documento PDF.
     * @throws IOException Si ocurre un error de E/S.
     */
    @GetMapping("/reporte/pdf")
    public void generarReportePdf(@ModelAttribute ProductoFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=reporte_productos.pdf";
        response.setHeader(headerKey, headerValue);

        List<Producto> productos = productoService.buscarProductosPorFiltros(filterDTO);

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Paragraph title = new Paragraph("Reporte de Productos", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        StringBuilder filtrosAplicados = new StringBuilder("Filtros Aplicados:\n");

        if (filterDTO.getNombre() != null && !filterDTO.getNombre().isEmpty()) {
            filtrosAplicados.append("- Nombre: ").append(filterDTO.getNombre()).append("\n");
        }
        if (filterDTO.getDescripcion() != null && !filterDTO.getDescripcion().isEmpty()) {
            filtrosAplicados.append("- Presentación: ").append(filterDTO.getDescripcion()).append("\n");
        }
        if (filterDTO.getIdCategoria() != null) {
            Optional<Categoria> categoriaOpt = productoService.obtenerCategoriaPorId(filterDTO.getIdCategoria());
            categoriaOpt.ifPresent(categoria -> filtrosAplicados.append("- Categoría: ").append(categoria.getNombreCategoria()).append("\n"));
        }
        if (filterDTO.getPrecio1Min() != null || filterDTO.getPrecio1Max() != null) {
            filtrosAplicados.append("- Precio 1: ");
            if (filterDTO.getPrecio1Min() != null) filtrosAplicados.append("Desde ").append(filterDTO.getPrecio1Min()).append(" ");
            if (filterDTO.getPrecio1Max() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getPrecio1Max());
            filtrosAplicados.append("\n");
        }
        if (filterDTO.getPrecio2Min() != null || filterDTO.getPrecio2Max() != null) {
            filtrosAplicados.append("- Precio 2: ");
            if (filterDTO.getPrecio2Min() != null) filtrosAplicados.append("Desde ").append(filterDTO.getPrecio2Min()).append(" ");
            if (filterDTO.getPrecio2Max() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getPrecio2Max());
            filtrosAplicados.append("\n");
        }
        if (filterDTO.getStockMin() != null || filterDTO.getStockMax() != null) {
            filtrosAplicados.append("- Stock: ");
            if (filterDTO.getStockMin() != null) filtrosAplicados.append("Desde ").append(filterDTO.getStockMin()).append(" ");
            if (filterDTO.getStockMax() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getStockMax());
            filtrosAplicados.append("\n");
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
        if (filterDTO.getFechaRegistroStart() != null || filterDTO.getFechaRegistroEnd() != null) {
            filtrosAplicados.append("- Fecha de Registro: ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            if (filterDTO.getFechaRegistroStart() != null) filtrosAplicados.append("Desde ").append(filterDTO.getFechaRegistroStart().format(formatter)).append(" ");
            if (filterDTO.getFechaRegistroEnd() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getFechaRegistroEnd().format(formatter));
            filtrosAplicados.append("\n");
        }

        if (filtrosAplicados.toString().equals("Filtros Aplicados:\n")) {
            filtrosAplicados.append("- Ninguno (Reporte Completo)\n");
        }

        Paragraph pFiltros = new Paragraph(filtrosAplicados.toString(), fontFilters);
        pFiltros.setAlignment(Paragraph.ALIGN_LEFT);
        pFiltros.setSpacingAfter(10);
        document.add(pFiltros);

        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        float[] columnWidths = {0.5f, 1.5f, 2f, 1f, 0.8f, 0.8f, 0.6f, 0.7f, 1.2f};
        table.setWidths(columnWidths);

        PdfPCell cell;
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.BLACK);
        Font fontContentActive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(0, 128, 0));
        Font fontContentInactive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(255, 0, 0));

        String[] headers = {"ID", "Nombre", "Presentación", "Categoría", "Precio 1", "Precio 2", "Stock", "Estado", "F. Registro"};
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        if (productos.isEmpty()) {
            cell = new PdfPCell(new Phrase("No se encontraron productos con los filtros aplicados.", fontContent));
            cell.setColspan(9);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Producto producto : productos) {
                table.addCell(new Phrase(String.valueOf(producto.getIdProducto()), fontContent));
                table.addCell(new Phrase(producto.getNombre(), fontContent));
                table.addCell(new Phrase(producto.getDescripcion(), fontContent));
                table.addCell(new Phrase(producto.getCategoria() != null ? producto.getCategoria().getNombreCategoria() : "N/A", fontContent));
                table.addCell(new Phrase(producto.getPrecio1() != null ? producto.getPrecio1().setScale(2, BigDecimal.ROUND_HALF_UP).toString() : "0.00", fontContent));
                table.addCell(new Phrase(producto.getPrecio2() != null ? producto.getPrecio2().setScale(2, BigDecimal.ROUND_HALF_UP).toString() : "0.00", fontContent));
                table.addCell(new Phrase(String.valueOf(producto.getStock()), fontContent));
                String estadoText;
                Font estadoFont;

                if (producto.getEstado() == 1) {
                    estadoText = "Activo";
                    estadoFont = fontContentActive;
                } else if (producto.getEstado() == 0) {
                    estadoText = "Inactivo";
                    estadoFont = fontContentInactive;
                } else {
                    estadoText = "Desconocido";
                    estadoFont = fontContent;
                }
                table.addCell(new Phrase(estadoText, estadoFont));
                table.addCell(new Phrase(producto.getFechaRegistro() != null ? producto.getFechaRegistro().format(formatter) : "N/A", fontContent));
            }
        }

        document.add(table);
        document.close();
    }
}
