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

    /**
     * Elimina un DocumentoCompra y sus DetalleCompra.
     * @param id El ID del documento de compra.
     * @return ResponseEntity con el resultado.
     */
    @GetMapping("/eliminar/{id}")
    public ResponseEntity<Map<String, String>> eliminarDocumentoCompra(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            documentoCompraService.eliminarDocumentoCompra(id);
            response.put("status", "success");
            response.put("message", "Documento de Compra eliminado exitosamente y stock de productos revertido!");
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            // Este catch es para cuando el documento o un producto asociado no se encuentra
            response.put("status", "error");
            response.put("message", "Error al eliminar el Documento de Compra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (RuntimeException e) {
            // Este catch es para RuntimeException, como la que lanzarías por "stock insuficiente"
            response.put("status", "error");
            response.put("message", "Error al eliminar el Documento de Compra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            // Este catch es para cualquier otra excepción inesperada
            response.put("status", "error");
            response.put("message", "Error interno al eliminar el Documento de Compra: " + e.getMessage());
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
                        //
                    }

                    if (documentoCompra.getDetalleCompras() != null && !documentoCompra.getDetalleCompras().isEmpty()) {
                        documentoCompra.getDetalleCompras().forEach(detalle -> {
                            if (detalle.getProducto() != null) {
                            } else {
                                //
                            }
                        });
                    } else {
                        //
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
}
