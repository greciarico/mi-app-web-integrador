package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.DocumentoCompraFilterDTO; // Importa el DTO
import com.example.DyD_Natures.Model.*;
import com.example.DyD_Natures.Repository.DocumentoCompraRepository;
import com.example.DyD_Natures.Repository.DocumentoSequenceRepository;
import com.example.DyD_Natures.Repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate; // Importar
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification; // Importar
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.ZoneId;

@Service
public class DocumentoCompraService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

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


    private static final byte ESTADO_ELIMINADO = 2;
    private static final byte ESTADO_ACTIVO    = 1;


    @Transactional
    public List<DocumentoCompra> listarDocumentosCompra() {
        List<DocumentoCompra> documentos = documentoCompraRepository.findAll();
        // Forzar la inicialización de las colecciones LAZY para evitar LazyInitializationException
        for (DocumentoCompra doc : documentos) {
            // Accede a una propiedad para forzar carga del proveedor (si no es nulo)
            if (doc.getProveedor() != null) {
                doc.getProveedor().getRazonSocial(); // Acceder a una propiedad para inicializar
            }
            // Forzar carga de la colección de detalles (si no es nula)
            if (doc.getDetalleCompras() != null) {
                doc.getDetalleCompras().size(); // Forzar inicialización de la colección
                for (DetalleCompra detalle : doc.getDetalleCompras()) {
                    // Forzar carga del producto en el detalle (si no es nulo)
                    if (detalle.getProducto() != null) {
                        detalle.getProducto().getNombre(); // Acceder a una propiedad para inicializar
                    }
                }
            }
        }
        return documentos;
    }

    @Transactional(readOnly = true)
    // También es crucial que este método sea transaccional para cargar relaciones LAZY
    public Optional<DocumentoCompra> obtenerDocumentoCompraPorId(Integer id) {
        Optional<DocumentoCompra> documentoOpt = documentoCompraRepository.findById(id);
        documentoOpt.ifPresent(doc -> {
            // Forzar la inicialización de las colecciones LAZY para el documento específico
            if (doc.getProveedor() != null) {
                doc.getProveedor().getRazonSocial();
            }
            if (doc.getDetalleCompras() != null) {
                doc.getDetalleCompras().size(); // Forzar inicialización de la colección
                for (DetalleCompra detalle : doc.getDetalleCompras()) {
                    if (detalle.getProducto() != null) {
                        detalle.getProducto().getNombre(); // Forzar inicialización del producto
                    }
                }
            }
        });
        return documentoOpt;
    }


    @Transactional
    public DocumentoCompra guardarDocumentoCompra(DocumentoCompra doc) {
        // --- 1) Fecha y número de documento ---
        doc.setFechaRegistro(LocalDate.now(LIMA));
        if (doc.getNumDocumento() == null || doc.getNumDocumento().isBlank()) {
            doc.setNumDocumento(generateNextDocumentNumber(doc.getTipoDocumento()));
        }

        // --- 2) Calcular total y asignar detalle -> documento ---
        BigDecimal totalDoc = BigDecimal.ZERO;
        for (DetalleCompra det : doc.getDetalleCompras()) {
            if (det.getProducto() == null || det.getCantidad() == null || det.getPrecioUnitario() == null) {
                throw new IllegalArgumentException("Cada línea debe traer producto, cantidad y precio unitario.");
            }
            det.setDocumentoCompra(doc);
            det.setTotal(det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad())));
            totalDoc = totalDoc.add(det.getTotal());
        }
        doc.setTotal(totalDoc);

        // --- 3) Guardar documento con cascada de detalles ---
        DocumentoCompra savedDoc = documentoCompraRepository.save(doc);

        // --- 4) Para cada detalle: crear/reactivar y actualizar stock del producto ---
        for (DetalleCompra det : savedDoc.getDetalleCompras()) {
            Producto prod;
            Integer prodId = det.getProducto().getIdProducto();

            if (prodId != null) {
                prod = productoRepository.findById(prodId)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + prodId));
            } else {
                // buscamos por nombre+desc+cat (ignorando eliminados)
                prod = productoRepository
                        .findByNombreAndDescripcionAndCategoria_IdCategoriaAndEstadoNot(
                                det.getProducto().getNombre(),
                                det.getProducto().getDescripcion(),
                                det.getProducto().getCategoria().getIdCategoria(),
                                ESTADO_ELIMINADO
                        )
                        .map(p -> {
                            if (p.getEstado() == ESTADO_ELIMINADO) p.setEstado(ESTADO_ACTIVO);
                            return p;
                        })
                        .orElseGet(() -> {
                            Producto np = new Producto();
                            np.setNombre(det.getProducto().getNombre());
                            np.setDescripcion(det.getProducto().getDescripcion());
                            np.setCategoria(det.getProducto().getCategoria());
                            np.setPrecio1(det.getPrecioUnitario());
                            np.setPrecio2(det.getPrecioUnitario());
                            np.setFechaRegistro(LocalDate.now(LIMA));
                            np.setStock(0);
                            np.setEstado(ESTADO_ACTIVO);
                            return np;
                        });
            }

            // sumar stock
            prod.setStock(prod.getStock() + det.getCantidad());
            productoRepository.save(prod);

            // asegurar que el detalle apunte al producto persistido
            det.setProducto(prod);
        }

        // --- 5) Volver a salvar para fijar FK detalle->producto  ---
        return documentoCompraRepository.save(savedDoc);
    }

    @Transactional // Esta transacción necesita ser separada o propagarse adecuadamente
    public String generateNextDocumentNumber(String tipoDocumento) {
        DocumentoSequence sequence = documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .orElseThrow(() -> new RuntimeException("Configuración de secuencia no encontrada para el tipo de documento: " + tipoDocumento));

        Integer nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        documentoSequenceRepository.save(sequence); // Guarda la secuencia incrementada inmediatamente

        String prefix = sequence.getPrefix();
        String formattedNumber = String.format("%0" + NUM_LENGTH + "d", nextNumber); // Formato a 8 dígitos con ceros iniciales

        return prefix + formattedNumber;
    }

    @Transactional(readOnly = true)
    public boolean existsByNumDocumento(String numDocumento) {
        return documentoCompraRepository.findByNumDocumento(numDocumento).isPresent();
    }

    @Transactional
    public void anularDocumentoCompra(Integer idCompra, Usuario usuarioAnulacion) {
        DocumentoCompra documentoCompra = documentoCompraRepository.findById(idCompra)
                .orElseThrow(() -> new EntityNotFoundException("Documento de Compra no encontrado con ID: " + idCompra));

        // 1. Validación de fecha: Solo se pueden anular compras del mismo día
        if (!documentoCompra.getFechaRegistro().isEqual(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se pueden anular documentos de compra registrados el mismo día.");
        }

        // 2. Verificar si ya está anulado
        if (documentoCompra.getEstado() != null && documentoCompra.getEstado() == 0) {
            throw new IllegalArgumentException("El Documento de Compra con ID " + idCompra + " ya está anulado.");
        }

        // 3. Revertir el stock de los productos
        if (documentoCompra.getDetalleCompras() != null && !documentoCompra.getDetalleCompras().isEmpty()) {
            documentoCompra.getDetalleCompras().forEach(detalle -> {
                if (detalle.getProducto() != null) {
                    updateProductStock(detalle.getProducto().getIdProducto(), -detalle.getCantidad());
                }
            });
        }

        // 4. Actualizar el estado del documento y registrar la auditoría de anulación
        documentoCompra.setEstado((byte) 0); // Cambia el estado a 0 (anulada)
        documentoCompra.setFechaAnulacion(LocalDateTime.now()); // Establece la fecha y hora de anulación
        documentoCompra.setUsuarioAnulacion(usuarioAnulacion); // Establece el usuario que anuló
        documentoCompraRepository.save(documentoCompra); // Persiste el cambio
    }

    @Transactional
    private void updateProductStock(Integer idProducto, Integer quantityChange) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado para actualizar stock: ID " + idProducto));

        // quantityChange puede ser positivo (suma) o negativo (resta)
        int newStock = producto.getStock() + quantityChange;
        if (newStock < 0) {
            // Esto es una advertencia o error, no debería permitir stock negativo
            throw new IllegalArgumentException("No se puede anular la compra, el stock del producto '" + producto.getNombre() + "' sería negativo.");
        }
        producto.setStock(newStock);
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public List<DocumentoCompra> buscarDocumentosCompraPorFiltros(DocumentoCompraFilterDTO filterDTO) {
        Specification<DocumentoCompra> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Excluir documentos con estado = 2 (si tienes ese estado para "eliminado físicamente" o similar)
            // Si 0 es inactivo/cancelado, y solo quieres activos para reportes por defecto, ajusta.
            // En tu lógica, 0 es 'cancelado'. Para un reporte general, podrías querer ambos.
            // Para este ejemplo, incluiremos 1 (activo) y 0 (cancelado) si no se especifica el estado.
            // Si el DTO permite estados específicos, úsalos.
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
                doc.getProveedor().getRazonSocial(); // Forzar carga
            }
            if (doc.getDetalleCompras() != null) {
                doc.getDetalleCompras().size(); // Forzar carga de la colección
                for (DetalleCompra detalle : doc.getDetalleCompras()) {
                    if (detalle.getProducto() != null) {
                        detalle.getProducto().getNombre(); // Forzar carga
                    }
                }
            }
        }
        return documentos;
    }

    @Transactional // <--- ¡Añade esto para indicar explícitamente que es de solo lectura!
    public String predictNextDocumentNumber(String tipoDocumento) {
        if (tipoDocumento == null || (!tipoDocumento.equalsIgnoreCase("BOLETA") && !tipoDocumento.equalsIgnoreCase("FACTURA"))) {
            throw new IllegalArgumentException("Tipo de documento inválido. Debe ser 'BOLETA' o 'FACTURA'.");
        }

        DocumentoSequence sequence = documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Configuración de secuencia no encontrada para el tipo de documento: " + tipoDocumento));

        Integer nextNumber = sequence.getLastNumber() + 1; // Solo se incrementa para la predicción, no se guarda
        String prefix = sequence.getPrefix();
        String formattedNumber = String.format("%0" + NUM_LENGTH + "d", nextNumber);

        return prefix + formattedNumber;
    }
}
