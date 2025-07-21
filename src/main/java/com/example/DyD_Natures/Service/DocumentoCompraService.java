package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.DocumentoCompraFilterDTO;
import com.example.DyD_Natures.Model.*;
import com.example.DyD_Natures.Repository.DocumentoCompraRepository;
import com.example.DyD_Natures.Repository.DocumentoSequenceRepository;
import com.example.DyD_Natures.Repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentoCompraService {

    @Autowired
    private DocumentoCompraRepository documentoCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DocumentoSequenceRepository documentoSequenceRepository; 

    @Autowired
    private UsuarioService usuarioService;


    private static final String BOLETA_PREFIX = "B001-"; 
    private static final String FACTURA_PREFIX = "F001-"; 
    private static final int NUM_LENGTH = 8; 


    /**
     * Lista todos los documentos de compra, asegurando que las relaciones LAZY se carguen.
     * @return Lista de documentos de compra.
     */
    @Transactional 
    public List<DocumentoCompra> listarDocumentosCompra() {
        List<DocumentoCompra> documentos = documentoCompraRepository.findAll();
        
        for (DocumentoCompra doc : documentos) {
            
            if (doc.getProveedor() != null) {
                doc.getProveedor().getRazonSocial(); 
            }
            
            if (doc.getDetalleCompras() != null) {
                doc.getDetalleCompras().size(); 
                for (DetalleCompra detalle : doc.getDetalleCompras()) {
                    
                    if (detalle.getProducto() != null) {
                        detalle.getProducto().getNombre(); 
                    }
                }
            }
        }
        return documentos;
    }

    /**
     * Obtiene un documento de compra por su ID.
     * @param id El ID del documento de compra.
     * @return Un Optional que contiene el DocumentoCompra si se encuentra, o vacío si no.
     */
    @Transactional(readOnly = true)
    
    public Optional<DocumentoCompra> obtenerDocumentoCompraPorId(Integer id) {
        Optional<DocumentoCompra> documentoOpt = documentoCompraRepository.findById(id);
        documentoOpt.ifPresent(doc -> {
            
            if (doc.getProveedor() != null) {
                doc.getProveedor().getRazonSocial();
            }
            if (doc.getDetalleCompras() != null) {
                doc.getDetalleCompras().size(); 
                for (DetalleCompra detalle : doc.getDetalleCompras()) {
                    if (detalle.getProducto() != null) {
                        detalle.getProducto().getNombre(); 
                    }
                }
            }
        });
        return documentoOpt;
    }

    /**
     * Guarda un documento de compra nuevo o actualiza uno existente,
     * manejando la persistencia de sus detalles y la actualización del stock de productos.
     * @param documentoCompra El objeto DocumentoCompra a guardar.
     * @return El DocumentoCompra guardado.
     * @throws RuntimeException si ocurre un error de negocio (ej. producto no encontrado).
     */
    @Transactional 
    public DocumentoCompra guardarDocumentoCompra(DocumentoCompra documentoCompra) {
        if (documentoCompra.getIdCompra() == null) {
            documentoCompra.setFechaRegistro(LocalDate.now());
            if (documentoCompra.getTotal() == null) {
                documentoCompra.setTotal(BigDecimal.ZERO);
            }
        } else {
            Optional<DocumentoCompra> existingDocOpt = documentoCompraRepository.findById(documentoCompra.getIdCompra());
            if (existingDocOpt.isPresent()) {
                DocumentoCompra oldDocumento = existingDocOpt.get();
                documentoCompra.setFechaRegistro(oldDocumento.getFechaRegistro()); 

                if (oldDocumento.getDetalleCompras() != null) {
                    for (DetalleCompra oldDetalle : oldDocumento.getDetalleCompras()) {
                        boolean foundInNew = false;
                        if (documentoCompra.getDetalleCompras() != null) {
                            for (DetalleCompra newDetalle : documentoCompra.getDetalleCompras()) {
                                if (oldDetalle.getIdDetalleCompra() != null && oldDetalle.getIdDetalleCompra().equals(newDetalle.getIdDetalleCompra())) {
                                    foundInNew = true;
                                    // Si la cantidad disminuyó, revertir la diferencia
                                    if (newDetalle.getCantidad() < oldDetalle.getCantidad()) {
                                        updateProductStock(oldDetalle.getProducto().getIdProducto(), oldDetalle.getCantidad() - newDetalle.getCantidad());
                                    }
                                    break;
                                }
                            }
                        }
                        if (!foundInNew) {
                        
                            updateProductStock(oldDetalle.getProducto().getIdProducto(), oldDetalle.getCantidad());
                        }
                    }
                }
            }
        }

        BigDecimal currentTotal = BigDecimal.ZERO;
        if (documentoCompra.getDetalleCompras() != null) {
            for (DetalleCompra detalle : documentoCompra.getDetalleCompras()) {
                if (detalle.getProducto() == null || detalle.getProducto().getIdProducto() == null) {
                    throw new IllegalArgumentException("Detalle de compra inválido: Producto es obligatorio.");
                }
                if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                    throw new IllegalArgumentException("Detalle de compra inválido: Cantidad debe ser mayor a 0.");
                }
                if (detalle.getPrecioUnitario() == null || detalle.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Detalle de compra inválido: Precio unitario no puede ser negativo.");
                }


                if (documentoCompra.getIdCompra() == null) {
                    String tipoDocumento = documentoCompra.getTipoDocumento();
                    if (tipoDocumento == null || (!tipoDocumento.equalsIgnoreCase("BOLETA") && !tipoDocumento.equalsIgnoreCase("FACTURA"))) {
                        throw new IllegalArgumentException("Tipo de documento inválido. Debe ser 'BOLETA' o 'FACTURA'.");
                    }

                    String nuevoNumDocumento = generateNextDocumentNumber(tipoDocumento);
                    documentoCompra.setNumDocumento(nuevoNumDocumento);
                } else {

                    Optional<DocumentoCompra> existingDoc = documentoCompraRepository.findById(documentoCompra.getIdCompra());
                    if (existingDoc.isPresent() && !existingDoc.get().getNumDocumento().equals(documentoCompra.getNumDocumento())) {

                        throw new IllegalArgumentException("No se permite cambiar el número de documento de una compra existente.");
                    }

                    if (existingDoc.isPresent() && existingDoc.get().getEstado() == 0 && documentoCompra.getEstado() == 1) {
                        throw new IllegalArgumentException("No se puede reactivar un documento de compra cancelado directamente desde la edición.");
                    }
                }


                Producto productoExistente = productoRepository.findById(detalle.getProducto().getIdProducto())
                        .orElseThrow(() -> new RuntimeException("Producto con ID " + detalle.getProducto().getIdProducto() + " no encontrado."));

                detalle.setProducto(productoExistente);

                detalle.setDocumentoCompra(documentoCompra);


                BigDecimal detalleTotal = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
                detalle.setTotal(detalleTotal);

                currentTotal = currentTotal.add(detalleTotal);

                int oldCantidad = 0;
                if (detalle.getIdDetalleCompra() != null && documentoCompra.getIdCompra() != null) { 
                    Optional<DocumentoCompra> existingDocOpt = documentoCompraRepository.findById(documentoCompra.getIdCompra());
                    if (existingDocOpt.isPresent()) {
                        oldCantidad = existingDocOpt.get().getDetalleCompras().stream()
                                .filter(od -> od.getIdDetalleCompra() != null && od.getIdDetalleCompra().equals(detalle.getIdDetalleCompra()))
                                .map(DetalleCompra::getCantidad).findFirst().orElse(0);
                    }
                }
                updateProductStock(detalle.getProducto().getIdProducto(), detalle.getCantidad() - oldCantidad);
            }
        }

        documentoCompra.setTotal(currentTotal); 
        return documentoCompraRepository.save(documentoCompra);
    }

    /**
     * Genera el siguiente número de documento consecutivo y único para el tipo de documento especificado.
     * Utiliza un bloqueo pesimista para asegurar la unicidad en entornos concurrentes.
     * @param tipoDocumento El tipo de documento ("BOLETA" o "FACTURA").
     * @return El número de documento generado (ej: "B001-00000001", "F001-00000001").
     * @throws RuntimeException si no se puede generar el número de documento.
     */
    @Transactional 
    public String generateNextDocumentNumber(String tipoDocumento) {
        DocumentoSequence sequence = documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .orElseThrow(() -> new RuntimeException("Configuración de secuencia no encontrada para el tipo de documento: " + tipoDocumento));

        Integer nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        documentoSequenceRepository.save(sequence); 

        String prefix = sequence.getPrefix();
        String formattedNumber = String.format("%0" + NUM_LENGTH + "d", nextNumber); 

        return prefix + formattedNumber;
    }

    /**
     * Valida si el número de documento ya existe.
     * Esta validación es crucial para evitar duplicados en caso de que la generación falle o se modifique manualmente.
     * @param numDocumento El número de documento a validar.
     * @return true si el número de documento ya existe, false en caso contrario.
     */
    @Transactional(readOnly = true)
    public boolean existsByNumDocumento(String numDocumento) {
        return documentoCompraRepository.findByNumDocumento(numDocumento).isPresent();
    }

    /**
     * Anula lógicamente un Documento de Compra (cambia su estado a 0) y revierte el stock de los productos.
     * Realiza validación de fecha (solo mismo día) y requiere un usuario administrador.
     * @param idCompra El ID del documento de compra a anular.
     * @param usuarioAnulacion El objeto Usuario que realiza la anulación (debe ser un administrador).
     * @throws EntityNotFoundException Si el documento de compra no se encuentra.
     * @throws IllegalArgumentException Si el documento ya está anulado o no cumple la validación de fecha.
     */
    @Transactional
    public void anularDocumentoCompra(Integer idCompra, Usuario usuarioAnulacion) {
        DocumentoCompra documentoCompra = documentoCompraRepository.findById(idCompra)
                .orElseThrow(() -> new EntityNotFoundException("Documento de Compra no encontrado con ID: " + idCompra));

        if (!documentoCompra.getFechaRegistro().isEqual(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se pueden anular documentos de compra registrados el mismo día.");
        }

        if (documentoCompra.getEstado() != null && documentoCompra.getEstado() == 0) {
            throw new IllegalArgumentException("El Documento de Compra con ID " + idCompra + " ya está anulado.");
        }


        if (documentoCompra.getDetalleCompras() != null && !documentoCompra.getDetalleCompras().isEmpty()) {
            documentoCompra.getDetalleCompras().forEach(detalle -> {
                if (detalle.getProducto() != null) {

                    updateProductStock(detalle.getProducto().getIdProducto(), -detalle.getCantidad());
                }
            });
        }


        documentoCompra.setEstado((byte) 0); 
        documentoCompra.setFechaAnulacion(LocalDateTime.now()); 
        documentoCompra.setUsuarioAnulacion(usuarioAnulacion); 
        documentoCompraRepository.save(documentoCompra); 
    }

    @Transactional
    private void updateProductStock(Integer idProducto, Integer quantityChange) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado para actualizar stock: ID " + idProducto));


        int newStock = producto.getStock() + quantityChange;
        if (newStock < 0) {

            throw new IllegalArgumentException("No se puede anular la compra, el stock del producto '" + producto.getNombre() + "' sería negativo.");
        }
        producto.setStock(newStock);
        productoRepository.save(producto);
    }

    /**
     * Busca documentos de compra aplicando filtros dinámicos.
     * Carga eagermente las relaciones necesarias para el reporte.
     * @param filterDTO DTO con los criterios de filtro.
     * @return Lista de DocumentoCompra que coinciden con los filtros.
     */
    @Transactional(readOnly = true)
    public List<DocumentoCompra> buscarDocumentosCompraPorFiltros(DocumentoCompraFilterDTO filterDTO) {
        Specification<DocumentoCompra> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                predicates.add(root.get("estado").in(filterDTO.getEstados()));
            }

            if (filterDTO.getTipoDocumento() != null && !filterDTO.getTipoDocumento().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("tipoDocumento")), "%" + filterDTO.getTipoDocumento().toLowerCase() + "%"));
            }
            if (filterDTO.getNumDocumento() != null && !filterDTO.getNumDocumento().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("numDocumento")), "%" + filterDTO.getNumDocumento().toLowerCase() + "%"));
            }
            if (filterDTO.getIdProveedor() != null) {
                predicates.add(cb.equal(root.get("proveedor").get("idProveedor"), filterDTO.getIdProveedor()));
            }
            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }
            if (filterDTO.getTotalMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("total"), filterDTO.getTotalMin()));
            }
            if (filterDTO.getTotalMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("total"), filterDTO.getTotalMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };


        List<DocumentoCompra> documentos = documentoCompraRepository.findAll(spec);
        for (DocumentoCompra doc : documentos) {
            if (doc.getProveedor() != null) {
                doc.getProveedor().getRazonSocial(); 
            }
            if (doc.getDetalleCompras() != null) {
                doc.getDetalleCompras().size(); 
                for (DetalleCompra detalle : doc.getDetalleCompras()) {
                    if (detalle.getProducto() != null) {
                        detalle.getProducto().getNombre(); 
                    }
                }
            }
        }
        return documentos;
    }

    /**
     * Predice el siguiente número de documento consecutivo para el tipo de documento especificado.
     * Este método NO incrementa el contador en la base de datos; es solo para pre-visualización.
     * @param tipoDocumento El tipo de documento ("BOLETA" o "FACTURA").
     * @return El número de documento predicho (ej: "B001-0000000X").
     * @throws IllegalArgumentException si el tipo de documento es inválido o no se encuentra la secuencia.
     */
    @Transactional 
    public String predictNextDocumentNumber(String tipoDocumento) {
        if (tipoDocumento == null || (!tipoDocumento.equalsIgnoreCase("BOLETA") && !tipoDocumento.equalsIgnoreCase("FACTURA"))) {
            throw new IllegalArgumentException("Tipo de documento inválido. Debe ser 'BOLETA' o 'FACTURA'.");
        }

        DocumentoSequence sequence = documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Configuración de secuencia no encontrada para el tipo de documento: " + tipoDocumento));

        Integer nextNumber = sequence.getLastNumber() + 1; 
        String prefix = sequence.getPrefix();
        String formattedNumber = String.format("%0" + NUM_LENGTH + "d", nextNumber);

        return prefix + formattedNumber;
    }
}
