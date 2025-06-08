package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.DocumentoCompra;
import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Service.DocumentoCompraService;
import com.example.DyD_Natures.Service.ProveedorService;
import com.example.DyD_Natures.Service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/documento-compra")
public class DocumentoCompraController {

    @Autowired
    private DocumentoCompraService documentoCompraService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private ProductoService productoService;

    /**
     * Muestra la vista principal de Documentos de Compra.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (documento_compra.html).
     */
    @GetMapping
    public String listarDocumentosCompra(Model model) {
        try {
            model.addAttribute("documentosCompra", documentoCompraService.listarDocumentosCompra());
            model.addAttribute("proveedores", proveedorService.listarProveedoresActivos());
            model.addAttribute("productos", productoService.listarProductosActivos());
            return "documento_compra";
        } catch (Exception e) {
            e.printStackTrace();
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
        model.addAttribute("proveedores", proveedorService.listarProveedoresActivos());
        model.addAttribute("productos", productoService.listarProductosActivos());
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
        model.addAttribute("proveedores", proveedorService.listarProveedoresActivos());
        model.addAttribute("productos", productoService.listarProductosActivos());
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
    @GetMapping("/ver/{id}")
    public String verDocumentoCompra(@PathVariable Integer id, Model model) {
        try {
            Optional<DocumentoCompra> documentoOpt = documentoCompraService.obtenerDocumentoCompraPorId(id);
            if (documentoOpt.isPresent()) {
                DocumentoCompra documentoCompra = documentoOpt.get();

                // --- PUNTOS CLAVE PARA IMPRIMIR EN CONSOLA Y DEPURAR ---
                System.out.println("--- DEBUG: INICIO DE verDocumentoCompra ---");
                System.out.println("ID recibido: " + id);
                System.out.println("¿Documento encontrado?: " + documentoOpt.isPresent());
                if (documentoOpt.isPresent()) {
                    System.out.println("Documento ID de la compra: " + documentoCompra.getIdCompra());
                    System.out.println("Fecha de Registro: " + documentoCompra.getFechaRegistro()); // Verifica si es null
                    System.out.println("Total de la compra: " + documentoCompra.getTotal()); // Verifica si es null o 0

                    // Verificar Proveedor
                    if (documentoCompra.getProveedor() != null) {
                        System.out.println("Proveedor cargado: " + documentoCompra.getProveedor().getRazonSocial());
                    } else {
                        System.out.println("¡ATENCIÓN! Proveedor es NULL para el documento ID: " + documentoCompra.getIdCompra());
                    }

                    // Verificar DetalleCompras
                    if (documentoCompra.getDetalleCompras() != null && !documentoCompra.getDetalleCompras().isEmpty()) {
                        System.out.println("Número de detalles de compra: " + documentoCompra.getDetalleCompras().size());
                        documentoCompra.getDetalleCompras().forEach(detalle -> {
                            System.out.println("  - Detalle ID: " + detalle.getIdDetalleCompra() +
                                    ", Cantidad: " + detalle.getCantidad() +
                                    ", Precio Unitario: " + detalle.getPrecioUnitario() +
                                    ", SubTotal (del detalle): " + detalle.getTotal()); // Si es total en detalle
                            if (detalle.getProducto() != null) {
                                System.out.println("    - Producto en detalle: " + detalle.getProducto().getNombre());
                            } else {
                                System.out.println("    - ¡ATENCIÓN! Producto es NULL en el detalle ID: " + detalle.getIdDetalleCompra());
                            }
                        });
                    } else {
                        System.out.println("¡ATENCIÓN! La lista de detalles de compra es NULL o está VACÍA.");
                    }
                }
                System.out.println("--- DEBUG: FIN DE verDocumentoCompra ---");
                // --- FIN PUNTOS CLAVE ---

                model.addAttribute("documentoCompra", documentoCompra);
                return "fragments/documento_compra_detalle_modal :: viewContent";
            } else {
                // Esto se ejecutaría si documentoOpt.isEmpty()
                model.addAttribute("mensajeError", "Documento de Compra no encontrado para el ID: " + id);
                System.err.println("ERROR: Documento de Compra con ID " + id + " no encontrado.");
                // Retornar el mismo fragmento para que muestre el mensaje de error dentro del modal
                return "fragments/documento_compra_detalle_modal :: viewContent";
            }
        } catch (Exception e) {
            e.printStackTrace(); // Imprime la excepción completa en la consola
            model.addAttribute("mensajeError", "Error interno al cargar los detalles del Documento de Compra: " + e.getMessage());
            System.err.println("EXCEPCIÓN al cargar detalles de Documento de Compra: " + e.getMessage());
            // Retornar el mismo fragmento para que muestre el mensaje de error dentro del modal
            return "fragments/documento_compra_detalle_modal :: viewContent";
        }
    }
}
