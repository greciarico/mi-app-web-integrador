package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.DocumentoCompraFilterDTO;
import com.example.DyD_Natures.Model.DocumentoCompra;
import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.*;
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
import org.springframework.security.crypto.password.PasswordEncoder; //

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
    // --- CAMBIOS PARA ANULAR COMPRA ---
    @Autowired
    private UsuarioService usuarioService; // Para obtener el usuario autenticado
    @Autowired
    private PasswordEncoder passwordEncoder; // Para comparar contraseñas

    @Autowired
    private CategoriaService categoriaService;
    // --- FIN CAMBIOS ANULAR COMPRA ---


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
            // e.printStackTrace(); // Eliminado
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
        // Siempre inicializa el proveedor y los detalles para evitar NullPointerExceptions en Thymeleaf
        documentoCompra.setProveedor(new Proveedor());
        documentoCompra.setDetalleCompras(new ArrayList<>());

        model.addAttribute("documentoCompra", documentoCompra);
        model.addAttribute("proveedores", proveedorService.listarSoloProveedoresActivos());
        model.addAttribute("productos", productoService.listarSoloProductosActivos());
        model.addAttribute("categorias", categoriaService.listarCategoriasActivas());
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
            // Asegúrate de que el proveedor y la lista de detalles no sean nulos para el formulario
            // Las inicializaciones LAZY se manejan en el servicio, aquí solo se asegura que no sean null
            if (documentoCompra.getProveedor() == null) {
                documentoCompra.setProveedor(new Proveedor());
            }
            if (documentoCompra.getDetalleCompras() == null) {
                documentoCompra.setDetalleCompras(new ArrayList<>());
            }
        } else {
            // Si no se encuentra, crea un objeto vacío pero inicializado
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
    @ResponseBody
    @PostMapping("/guardar")
    public ResponseEntity<Map<String, String>> guardarDocumentoCompra(
            @RequestBody DocumentoCompra documentoCompra) {

        Map<String, String> resp = new HashMap<>();
        try {
            DocumentoCompra creado = documentoCompraService.guardarDocumentoCompra(documentoCompra);
            resp.put("status",  "success");
            resp.put("message", "Compra registrada correctamente (ID=" + creado.getIdCompra() + ")");
            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException ex) {
            resp.put("status",  "error");
            resp.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(resp);

        } catch (Exception ex) {
            resp.put("status",  "error");
            resp.put("message", "Error interno: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    // --- CAMBIOS PARA ANULAR COMPRA (ANTES 'eliminar/{id}') ---
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
            // Obtener el DNI del usuario autenticado de Spring Security
            String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario currentUser = usuarioService.obtenerUsuarioPorDni(currentUserName)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario autenticado no encontrado."));

            // Verificar si el usuario autenticado tiene el rol de ADMINISTRADOR
            // Asumiendo que el rol ADMINISTRADOR es el que puede anular
            boolean isAdmin = currentUser.getRolUsuario() != null &&
                    "ADMINISTRADOR".equalsIgnoreCase(currentUser.getRolUsuario().getTipoRol());

            if (!isAdmin) {
                response.put("status", "error");
                response.put("message", "Solo los usuarios con rol de ADMINISTRADOR pueden anular compras.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Validar la contraseña del usuario autenticado contra la contraseña proporcionada
            if (!passwordEncoder.matches(adminPassword, currentUser.getContrasena())) {
                response.put("status", "error");
                response.put("message", "Contraseña de administrador incorrecta.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Si todas las validaciones pasan, llamar al servicio para anular
            documentoCompraService.anularDocumentoCompra(id, currentUser); // Pasar el usuario que anula

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
    // --- FIN CAMBIOS ANULAR COMPRA --

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

                // --- PUNTOS CLAVE PARA IMPRIMIR EN CONSOLA Y DEPURAR ---
                // System.out.println("--- DEBUG: INICIO DE verDocumentoCompra ---"); // Eliminado
                // System.out.println("ID recibido: " + id); // Eliminado
                // System.out.println("¿Documento encontrado?: " + documentoOpt.isPresent()); // Eliminado
                if (documentoOpt.isPresent()) { // Esta comprobación es redundante aquí, ya que el isPresent() es previo
                    // System.out.println("Documento ID de la compra: " + documentoCompra.getIdCompra()); // Eliminado
                    // System.out.println("Fecha de Registro: " + documentoCompra.getFechaRegistro()); // Eliminado
                    // System.out.println("Total de la compra: " + documentoCompra.getTotal()); // Eliminado

                    // Verificar Proveedor
                    if (documentoCompra.getProveedor() != null) {
                        // System.out.println("Proveedor cargado: " + documentoCompra.getProveedor().getRazonSocial()); // Eliminado
                    } else {
                        // System.out.println("¡ATENCIÓN! Proveedor es NULL para el documento ID: " + documentoCompra.getIdCompra()); // Eliminado
                    }

                    // Verificar DetalleCompras
                    if (documentoCompra.getDetalleCompras() != null && !documentoCompra.getDetalleCompras().isEmpty()) {
                        // System.out.println("Número de detalles de compra: " + documentoCompra.getDetalleCompras().size()); // Eliminado
                        documentoCompra.getDetalleCompras().forEach(detalle -> {
                            // System.out.println("  - Detalle ID: " + detalle.getIdDetalleCompra() + // Eliminado
                            //        ", Cantidad: " + detalle.getCantidad() + // Eliminado
                            //        ", Precio Unitario: " + detalle.getPrecioUnitario() + // Eliminado
                            //        ", SubTotal (del detalle): " + detalle.getTotal()); // Eliminado
                            if (detalle.getProducto() != null) {
                                // System.out.println("    - Producto en detalle: " + detalle.getProducto().getNombre()); // Eliminado
                            } else {
                                // System.out.println("    - ¡ATENCIÓN! Producto es NULL en el detalle ID: " + detalle.getIdDetalleCompra()); // Eliminado
                            }
                        });
                    } else {
                        // System.out.println("¡ATENCIÓN! La lista de detalles de compra es NULL o está VACÍA."); // Eliminado
                    }
                }
                // System.out.println("--- DEBUG: FIN DE verDocumentoCompra ---"); // Eliminado
                // --- FIN PUNTOS CLAVE ---

                model.addAttribute("documentoCompra", documentoCompra);
                return "fragments/documento_compra_detalle_modal :: viewContent";
            } else {
                // Esto se ejecutaría si documentoOpt.isEmpty()
                model.addAttribute("mensajeError", "Documento de Compra no encontrado para el ID: " + id);
                // System.err.println("ERROR: Documento de Compra con ID " + id + " no encontrado."); // Eliminado
                // Retornar el mismo fragmento para que muestre el mensaje de error dentro del modal
                return "fragments/documento_compra_detalle_modal :: viewContent";
            }
        } catch (Exception e) {
            // e.printStackTrace(); // Eliminado
            model.addAttribute("mensajeError", "Error interno al cargar los detalles del Documento de Compra: " + e.getMessage());
            // System.err.println("EXCEPCIÓN al cargar detalles de Documento de Compra: " + e.getMessage()); // Eliminado
            // Retornar el mismo fragmento para que muestre el mensaje de error dentro del modal
            return "fragments/documento_compra_detalle_modal :: viewContent";
        }
    }
    /**
     * Carga el fragmento HTML del modal para generar reportes de documentos de compra.
     * @param model El modelo para pasar datos a la vista (ej. lista de proveedores).
     * @return La ruta al fragmento HTML del modal de reporte.
     */
    @GetMapping("/reporte/modal")
    public String openReporteDocumentosCompraModal(Model model) {
        model.addAttribute("proveedores", proveedorService.listarSoloProveedoresActivos());
        // CAMBIO AQUÍ: Usar el nombre de archivo existente
        return "fragments/reporte_documentos_compra_modal :: reporteModalContent";
    }

    @GetMapping("/reporte/pdf")
    public void generateDocumentoCompraReportePdf(@ModelAttribute DocumentoCompraFilterDTO filterDTO, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=reporte_documentos_compra.pdf");

        try {
            List<DocumentoCompra> documentosCompra = documentoCompraService.buscarDocumentosCompraPorFiltros(filterDTO);

            Document document = new Document(PageSize.A4.rotate()); // Página apaisada
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Fuentes y colores
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLDITALIC, BaseColor.GRAY);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
            Font activeStatusFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(0, 100, 0)); // Verde oscuro
            Font cancelledStatusFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(178, 34, 34)); // Rojo ladrillo
            Font unknownStatusFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.ORANGE); // Nuevo: Para estados nulos o desconocidos

            // Título
            Paragraph title = new Paragraph("Reporte de Documentos de Compra", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Mostrar filtros aplicados
            Paragraph filtersApplied = new Paragraph("Filtros Aplicados:", subtitleFont);
            filtersApplied.setSpacingAfter(5);
            document.add(filtersApplied);

            // Formato para fechas
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
                        // Envuelve la checked exception en una unchecked exception
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
                // Si ambos están seleccionados, no se añade este filtro (o si no hay ninguno)
                if (filterDTO.getEstados().size() == 1 || (filterDTO.getEstados().size() == 2 && !estadosText.toString().isEmpty())) {
                    document.add(new Paragraph(estadosText.toString(), contentFont));
                }
            } else {
                document.add(new Paragraph(" - Estados: Todos", contentFont)); // Si no se seleccionó ningún estado
            }


            document.add(Chunk.NEWLINE); // Espacio después de los filtros

            if (documentosCompra.isEmpty()) {
                Paragraph noResults = new Paragraph("No se encontraron documentos de compra con los filtros seleccionados.", contentFont);
                noResults.setAlignment(Element.ALIGN_CENTER);
                document.add(noResults);
            } else {
                // Tabla de documentos de compra (columnas principales)
                PdfPTable table = new PdfPTable(7); // ID, Fecha, Proveedor, Tipo Doc, N° Doc, Total, Estado
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(10f);
                float[] columnWidths = {0.8f, 1.2f, 2.5f, 1f, 1.5f, 1f, 0.8f}; // Ajusta los anchos si es necesario
                table.setWidths(columnWidths);

                // Cabeceras de la tabla principal
                String[] headers = {"ID Compra", "Fecha Reg.", "Proveedor", "Tipo Doc.", "N° Documento", "Total", "Estado"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setBackgroundColor(new BaseColor(24, 61, 0)); // Color verde oscuro similar a tu botón
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                // Datos de la tabla principal
                DecimalFormat df = new DecimalFormat("0.00");
                for (DocumentoCompra doc : documentosCompra) {
                    table.addCell(createCell(String.valueOf(doc.getIdCompra()), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(doc.getFechaRegistro().format(dateFormatter), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(doc.getProveedor() != null ? doc.getProveedor().getRazonSocial() : "N/A", contentFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(doc.getTipoDocumento(), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(doc.getNumDocumento(), contentFont, Element.ALIGN_CENTER));
                    table.addCell(createCell("S/ " + df.format(doc.getTotal()), contentFont, Element.ALIGN_RIGHT));

                    // **INICIO DEL CAMBIO CRÍTICO: MANEJO DEL ESTADO NULL**
                    String estadoText;
                    Font estadoCellFont;

                    if (doc.getEstado() == null) {
                        estadoText = "Desconocido"; // Texto para estado nulo
                        estadoCellFont = unknownStatusFont; // Fuente/color para estado desconocido
                    } else if (doc.getEstado() == 1) { // Compara directamente el objeto Byte con el literal int
                        estadoText = "Activo";
                        estadoCellFont = activeStatusFont;
                    } else if (doc.getEstado() == 0) {
                        estadoText = "Cancelado";
                        estadoCellFont = cancelledStatusFont;
                    } else {
                        estadoText = "Inválido (" + doc.getEstado() + ")"; // Para otros valores que no sean 0, 1 o null
                        estadoCellFont = unknownStatusFont;
                    }
                    table.addCell(createCell(estadoText, estadoCellFont, Element.ALIGN_CENTER));
                    // **FIN DEL CAMBIO CRÍTICO**
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

    // Método auxiliar para crear celdas de tabla PDF
    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        return cell;
    }

    /**
     * Endpoint para obtener el siguiente número de documento predicho (sin guardar).
     * @param tipoDocumento El tipo de documento para el cual predecir el número.
     * @return ResponseEntity con el número de documento predicho o un mensaje de error.
     */
    @GetMapping("/next-num-documento")
    @ResponseBody // Para devolver JSON
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
