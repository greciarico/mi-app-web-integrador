package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.DocumentoCompraFilterDTO;
import com.example.DyD_Natures.Model.DocumentoCompra;
import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.DocumentoCompraService;
import com.example.DyD_Natures.Service.ProductoService;
import com.example.DyD_Natures.Service.ProveedorService;
import com.example.DyD_Natures.Service.UsuarioService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder; 

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Controller
@RequestMapping("/documento-compra")
public class DocumentoCompraController {

    @Autowired
    private DocumentoCompraService documentoCompraService;
    @Autowired
    private ProveedorService proveedorService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private UsuarioService usuarioService; 
    @Autowired
    private PasswordEncoder passwordEncoder; 
    
    /**
     * Muestra la vista principal de Documentos de Compra.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (documento_compra.html).
     */
    @GetMapping
    public String listarDocumentosCompra(HttpServletRequest request, Model model) {
        try {
            model.addAttribute("currentUri", request.getRequestURI());
            model.addAttribute("documentosCompra", documentoCompraService.listarDocumentosCompra());
            model.addAttribute("proveedores", proveedorService.listarSoloProveedoresActivos());
            model.addAttribute("productos", productoService.listarSoloProductosActivos());
            return "documento_compra";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar la página de Documentos de Compra: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Endpoint para obtener todos los documentos de compra en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de documentos de compra.
     */
    @GetMapping("/all")
    @ResponseBody
    public List<DocumentoCompra> getAllDocumentosCompraJson() {
        return documentoCompraService.listarDocumentosCompra();
    }

    /**
     * Muestra el formulario para crear un nuevo documento de compra.
     * @param model El modelo.
     * @return Fragmento del formulario.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        DocumentoCompra documentoCompra = new DocumentoCompra();
        documentoCompra.setProveedor(new Proveedor());
        documentoCompra.setDetalleCompras(new ArrayList<>());

        model.addAttribute("documentoCompra", documentoCompra);
        model.addAttribute("proveedores", proveedorService.listarSoloProveedoresActivos());
        model.addAttribute("productos", productoService.listarSoloProductosActivos());
        return "fragments/documento_compra_form_modal :: formContent";
    }

    /**
     * Muestra el formulario para editar un documento de compra existente.
     * @param id El ID del documento de compra.
     * @param model El modelo.
     * @return Fragmento del formulario o error.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<DocumentoCompra> documentoOpt = documentoCompraService.obtenerDocumentoCompraPorId(id);
        DocumentoCompra documentoCompra;

        if (documentoOpt.isPresent()) {
            documentoCompra = documentoOpt.get();
            if (documentoCompra.getProveedor() == null) {
                documentoCompra.setProveedor(new Proveedor());
            }
            if (documentoCompra.getDetalleCompras() == null) {
                documentoCompra.setDetalleCompras(new ArrayList<>());
            }
        } else {
            documentoCompra = new DocumentoCompra();
            documentoCompra.setProveedor(new Proveedor());
            documentoCompra.setDetalleCompras(new ArrayList<>());
            model.addAttribute("mensajeError", "Documento de Compra no encontrado para editar.");
        }

        model.addAttribute("documentoCompra", documentoCompra);
        model.addAttribute("proveedores", proveedorService.listarSoloProveedoresActivos());
        model.addAttribute("productos", productoService.listarSoloProductosActivos());
        return "fragments/documento_compra_form_modal :: formContent";
    }

    /**
     * Guarda un DocumentoCompra y sus DetalleCompra.
     * @param documentoCompra El objeto DocumentoCompra.
     * @return ResponseEntity con el resultado.
     */
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarDocumentoCompra(@RequestBody DocumentoCompra documentoCompra) {
        Map<String, String> response = new HashMap<>();
        try {
            documentoCompraService.guardarDocumentoCompra(documentoCompra);
            response.put("status", "success");
            response.put("message", "Documento de Compra guardado exitosamente y stock de productos actualizado!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", "Error de negocio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al guardar el Documento de Compra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    /**
     * Anula lógicamente un Documento de Compra (cambia de estado a 0) y revierte el stock de los productos.
     * Requiere la contraseña del administrador para confirmación y valida que la compra sea del mismo día.
     * @param id El ID del documento de compra.
     * @param requestBody Un mapa que contiene la contraseña del administrador {"password": "adminPassword"}.
     * @return ResponseEntity con el resultado de la operación.
     */
    @PostMapping("/anular/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> anularDocumentoCompra(@PathVariable("id") Integer id,
                                                                     @RequestBody Map<String, String> requestBody) {
        Map<String, String> response = new HashMap<>();
        String adminPassword = requestBody.get("password");

        if (adminPassword == null || adminPassword.isEmpty()) {
            response.put("status", "error");
            response.put("message", "La contraseña del administrador es obligatoria para anular la compra.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario currentUser = usuarioService.obtenerUsuarioPorDni(currentUserName)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario autenticado no encontrado."));
            boolean isAdmin = currentUser.getRolUsuario() != null &&
                    "ADMINISTRADOR".equalsIgnoreCase(currentUser.getRolUsuario().getTipoRol());

            if (!isAdmin) {
                response.put("status", "error");
                response.put("message", "Solo los usuarios con rol de ADMINISTRADOR pueden anular compras.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            if (!passwordEncoder.matches(adminPassword, currentUser.getContrasena())) {
                response.put("status", "error");
                response.put("message", "Contraseña de administrador incorrecta.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            documentoCompraService.anularDocumentoCompra(id, currentUser); 

            response.put("status", "success");
            response.put("message", "Documento de Compra anulado exitosamente y stock de productos revertido!");
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            response.put("status", "error");
            response.put("message", "Error al anular la Compra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Error de negocio al anular la Compra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (UsernameNotFoundException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al anular la Compra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Muestra los detalles de un documento de compra específico.
     * @param id El ID del documento de compra a visualizar.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista o fragmento para mostrar los detalles.
     */
    @GetMapping("/ver/{id}")
    public String verDocumentoCompra(@PathVariable Integer id, Model model) {
        try {
            Optional<DocumentoCompra> documentoOpt = documentoCompraService.obtenerDocumentoCompraPorId(id);
            if (documentoOpt.isPresent()) {
                DocumentoCompra documentoCompra = documentoOpt.get();

                    if (documentoOpt.isPresent()) { 
                    if (documentoCompra.getProveedor() != null) {
                    } else {
                    }
                    if (documentoCompra.getDetalleCompras() != null && !documentoCompra.getDetalleCompras().isEmpty()) {
                        documentoCompra.getDetalleCompras().forEach(detalle -> {
                            
                            if (detalle.getProducto() != null) {
                            } else {
                            }
                        });
                    } else {
                    }
                }

                model.addAttribute("documentoCompra", documentoCompra);
                return "fragments/documento_compra_detalle_modal :: viewContent";
            } else {
                model.addAttribute("mensajeError", "Documento de Compra no encontrado para el ID: " + id);
                return "fragments/documento_compra_detalle_modal :: viewContent";
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error interno al cargar los detalles del Documento de Compra: " + e.getMessage());
            return "fragments/documento_compra_detalle_modal :: viewContent";
        }
    }

    @GetMapping("/reporte/pdf")
    public void generateDocumentoCompraReportePdf(@ModelAttribute DocumentoCompraFilterDTO filterDTO, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=reporte_documentos_compra.pdf");

        try {
            List<DocumentoCompra> documentosCompra = documentoCompraService.buscarDocumentosCompraPorFiltros(filterDTO);

            Document document = new Document(PageSize.A4.rotate()); 
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLDITALIC, BaseColor.GRAY);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
            Font activeStatusFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(0, 100, 0)); 
            Font cancelledStatusFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(178, 34, 34)); 
            Font unknownStatusFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.ORANGE); 

            Paragraph title = new Paragraph("Reporte de Documentos de Compra", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph filtersApplied = new Paragraph("Filtros Aplicados:", subtitleFont);
            filtersApplied.setSpacingAfter(5);
            document.add(filtersApplied);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            if (filterDTO.getTipoDocumento() != null && !filterDTO.getTipoDocumento().isEmpty()) {
                document.add(new Paragraph(" - Tipo Documento: " + filterDTO.getTipoDocumento(), contentFont));
            }
            if (filterDTO.getNumDocumento() != null && !filterDTO.getNumDocumento().isEmpty()) {
                document.add(new Paragraph(" - N° Documento: " + filterDTO.getNumDocumento(), contentFont));
            }
            if (filterDTO.getIdProveedor() != null) {
                Optional<Proveedor> prov = proveedorService.obtenerProveedorPorId(filterDTO.getIdProveedor());
                prov.ifPresent(p -> {
                    try {
                        document.add(new Paragraph(" - Proveedor: " + p.getRazonSocial(), contentFont));
                    } catch (DocumentException e) {
                        throw new RuntimeException("Error al añadir párrafo de proveedor al PDF", e);
                    }
                });
            }
            if (filterDTO.getFechaRegistroStart() != null) {
                document.add(new Paragraph(" - Fecha Registro Desde: " + filterDTO.getFechaRegistroStart().format(dateFormatter), contentFont));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                document.add(new Paragraph(" - Fecha Registro Hasta: " + filterDTO.getFechaRegistroEnd().format(dateFormatter), contentFont));
            }
            if (filterDTO.getTotalMin() != null) {
                document.add(new Paragraph(" - Total Mínimo: S/ " + filterDTO.getTotalMin().setScale(2, BigDecimal.ROUND_HALF_UP), contentFont));
            }
            if (filterDTO.getTotalMax() != null) {
                document.add(new Paragraph(" - Total Máximo: S/ " + filterDTO.getTotalMax().setScale(2, BigDecimal.ROUND_HALF_UP), contentFont));
            }
            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                StringBuilder estadosText = new StringBuilder(" - Estados: ");
                if (filterDTO.getEstados().contains((byte) 1)) estadosText.append("Activo");
                if (filterDTO.getEstados().contains((byte) 0)) {
                    if (filterDTO.getEstados().contains((byte) 1)) estadosText.append(", ");
                    estadosText.append("Cancelado");
                }
                if (filterDTO.getEstados().size() == 1 || (filterDTO.getEstados().size() == 2 && !estadosText.toString().isEmpty())) {
                    document.add(new Paragraph(estadosText.toString(), contentFont));
                }
            } else {
                document.add(new Paragraph(" - Estados: Todos", contentFont)); 
            }

            document.add(Chunk.NEWLINE); 

            if (documentosCompra.isEmpty()) {
                Paragraph noResults = new Paragraph("No se encontraron documentos de compra con los filtros seleccionados.", contentFont);
                noResults.setAlignment(Element.ALIGN_CENTER);
                document.add(noResults);
            } else {
                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(10f);
                float[] columnWidths = {0.8f, 1.2f, 2.5f, 1f, 1.5f, 1f, 0.8f}; 
                table.setWidths(columnWidths);

                String[] headers = {"ID Compra", "Fecha Reg.", "Proveedor", "Tipo Doc.", "N° Documento", "Total", "Estado"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setBackgroundColor(new BaseColor(24, 61, 0)); 
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                DecimalFormat df = new DecimalFormat("0.00");
                for (DocumentoCompra doc : documentosCompra) {
                    table.addCell(createCell(String.valueOf(doc.getIdCompra()), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(doc.getFechaRegistro().format(dateFormatter), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(doc.getProveedor() != null ? doc.getProveedor().getRazonSocial() : "N/A", contentFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(doc.getTipoDocumento(), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(doc.getNumDocumento(), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell("S/ " + df.format(doc.getTotal()), contentFont, Element.ALIGN_RIGHT));

                    String estadoText;
                    Font estadoCellFont;

                    if (doc.getEstado() == null) {
                        estadoText = "Desconocido"; 
                        estadoCellFont = unknownStatusFont; 
                    } else if (doc.getEstado() == 1) { 
                        estadoText = "Activo";
                        estadoCellFont = activeStatusFont;
                    } else if (doc.getEstado() == 0) {
                        estadoText = "Cancelado";
                        estadoCellFont = cancelledStatusFont;
                    } else {
                        estadoText = "Inválido (" + doc.getEstado() + ")";
                        estadoCellFont = unknownStatusFont;
                    }
                    table.addCell(createCell(estadoText, estadoCellFont, Element.ALIGN_CENTER));
                }
                document.add(table);
            }
            document.close();
        } catch (DocumentException e) {
            System.err.println("Error al generar el PDF del reporte de documentos de compra: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("Error inesperado al generar el reporte de documentos de compra: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        return cell;
    }
    /**
     * Endpoint para buscar documentos de compra aplicando filtros dinámicos.
     * Utilizado por AJAX para recargar la tabla principal.
     * @param filterDTO DTO con los criterios de filtro (incluyendo estados, proveedor, etc.).
     * @return Una lista de documentos de compra que coinciden con los filtros en formato JSON.
     */
    @GetMapping("/buscar") // NUEVO ENDPOINT
    @ResponseBody // Indica que el retorno es el cuerpo de la respuesta, no una vista
    public ResponseEntity<List<DocumentoCompra>> buscarDocumentosCompra(DocumentoCompraFilterDTO filterDTO) {
        try {
            // Opcional: Log para depuración, puedes eliminarlo después
            System.out.println("DEBUG - DocumentoCompraController - Filtros recibidos:");
            System.out.println("  Tipo Documento: " + filterDTO.getTipoDocumento());
            System.out.println("  Num Documento: " + filterDTO.getNumDocumento());
            System.out.println("  ID Proveedor: " + filterDTO.getIdProveedor());
            System.out.println("  Estados: " + filterDTO.getEstados()); // MUY IMPORTANTE PARA VER SI LLEGA NULL/VACIO

            List<DocumentoCompra> documentos = documentoCompraService.buscarDocumentosCompraPorFiltros(filterDTO);
            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            System.err.println("Error al buscar documentos de compra: " + e.getMessage());
            // Retorna un error 500 con una lista vacía
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
    /**
     * Endpoint para obtener el siguiente número de documento predicho (sin guardar).
     * @param tipoDocumento El tipo de documento para el cual predecir el número.
     * @return ResponseEntity con el número de documento predicho o un mensaje de error.
     */
    @GetMapping("/next-num-documento")
    @ResponseBody 
    public ResponseEntity<Map<String, String>> getNextDocumentNumber(@RequestParam("tipoDocumento") String tipoDocumento) {
        Map<String, String> response = new HashMap<>();
        try {
            String nextNum = documentoCompraService.predictNextDocumentNumber(tipoDocumento);
            response.put("status", "success");
            response.put("nextNumDocumento", nextNum);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al predecir el número de documento: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
