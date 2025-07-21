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

@Service
public class DocumentoCompraService {

    @Autowired
    private DocumentoCompraRepository documentoCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DocumentoSequenceRepository documentoSequenceRepository; // Inyectar el nuevo repositorio
    // --- CAMBIOS PARA ANULAR COMPRA ---
    // Inyectar UsuarioService para la búsqueda de usuario por DNI
    @Autowired
    private UsuarioService usuarioService;
    // --- FIN CAMBIOS ANULAR COMPRA ---

    private static final String BOLETA_PREFIX = "B001-"; // Prefijo de ejemplo para Boleta
    private static final String FACTURA_PREFIX = "F001-"; // Prefijo de ejemplo para Factura
    private static final int NUM_LENGTH = 8; // Ejemplo: 8 dígitos para el número consecutivo


    /**
     * Lista todos los documentos de compra, asegurando que las relaciones LAZY se carguen.
     * @return Lista de documentos de compra.
     */
    @Transactional // <--- Asegura una sesión activa para cargar relaciones LAZY
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

    /**
     * Obtiene un documento de compra por su ID.
     * @param id El ID del documento de compra.
     * @return Un Optional que contiene el DocumentoCompra si se encuentra, o vacío si no.
     */
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

    /**
     * Guarda un documento de compra nuevo o actualiza uno existente,
     * manejando la persistencia de sus detalles y la actualización del stock de productos.
     * @param documentoCompra El objeto DocumentoCompra a guardar.
     * @return El DocumentoCompra guardado.
     * @throws RuntimeException si ocurre un error de negocio (ej. producto no encontrado).
     */
    @Transactional // Asegura que toda la operación sea atómica
    public DocumentoCompra guardarDocumentoCompra(DocumentoCompra documentoCompra) {
        // Establecer fecha de registro si es un nuevo documento de compra
        if (documentoCompra.getIdCompra() == null) {
            documentoCompra.setFechaRegistro(LocalDate.now());
            if (documentoCompra.getTotal() == null) {
                documentoCompra.setTotal(BigDecimal.ZERO);
            }
        } else {
            // Si es una edición, mantener la fecha de registro existente y manejar stock anterior
            Optional<DocumentoCompra> existingDocOpt = documentoCompraRepository.findById(documentoCompra.getIdCompra());
            if (existingDocOpt.isPresent()) {
                DocumentoCompra oldDocumento = existingDocOpt.get();
                documentoCompra.setFechaRegistro(oldDocumento.getFechaRegistro()); // Mantener fecha original

                // Revertir stock de productos de los detalles del documento antiguo
                // Solo si el detalle no está en el nuevo documento o su cantidad disminuyó
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
                            // Si el detalle antiguo no está en los nuevos, revertir todo el stock
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

                // Si es un nuevo documento (idCompra es null), generamos el número de documento
                if (documentoCompra.getIdCompra() == null) {
                    String tipoDocumento = documentoCompra.getTipoDocumento();
                    if (tipoDocumento == null || (!tipoDocumento.equalsIgnoreCase("BOLETA") && !tipoDocumento.equalsIgnoreCase("FACTURA"))) {
                        throw new IllegalArgumentException("Tipo de documento inválido. Debe ser 'BOLETA' o 'FACTURA'.");
                    }

                    // Generar número de documento único y consecutivo
                    String nuevoNumDocumento = generateNextDocumentNumber(tipoDocumento);
                    documentoCompra.setNumDocumento(nuevoNumDocumento);
                } else {
                    // Si es una edición, podrías añadir lógica para evitar cambiar el numDocumento
                    // o validarlo si se permite el cambio. Por ahora, asumimos que no se cambia.
                    // Opcional: Validar que el numDocumento no haya sido manipulado si ya existe.
                    Optional<DocumentoCompra> existingDoc = documentoCompraRepository.findById(documentoCompra.getIdCompra());
                    if (existingDoc.isPresent() && !existingDoc.get().getNumDocumento().equals(documentoCompra.getNumDocumento())) {
                        // Puedes lanzar un error o manejar esto según tus reglas de negocio
                        throw new IllegalArgumentException("No se permite cambiar el número de documento de una compra existente.");
                    }
                    // Si se está editando, asegúrate de que el estado no se cambie de 0 (cancelado) a 1 (activo)
                    if (existingDoc.isPresent() && existingDoc.get().getEstado() == 0 && documentoCompra.getEstado() == 1) {
                        throw new IllegalArgumentException("No se puede reactivar un documento de compra cancelado directamente desde la edición.");
                    }
                }

                // Obtener el producto gestionado (así Hibernate lo adjunta a la sesión)
                Producto productoExistente = productoRepository.findById(detalle.getProducto().getIdProducto())
                        .orElseThrow(() -> new RuntimeException("Producto con ID " + detalle.getProducto().getIdProducto() + " no encontrado."));

                detalle.setProducto(productoExistente); // Asigna el objeto Producto completo

                // Establecer la referencia bidireccional del detalle al documento de compra
                detalle.setDocumentoCompra(documentoCompra);

                // Calcular el total del detalle
                BigDecimal detalleTotal = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
                detalle.setTotal(detalleTotal);

                currentTotal = currentTotal.add(detalleTotal);

                // Aumentar el stock del producto
                int oldCantidad = 0;
                if (detalle.getIdDetalleCompra() != null && documentoCompra.getIdCompra() != null) { // Si es un detalle existente
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

        documentoCompra.setTotal(currentTotal); // Actualizar el total del documento de compra
        return documentoCompraRepository.save(documentoCompra);
    }

    /**
     * Genera el siguiente número de documento consecutivo y único para el tipo de documento especificado.
     * Utiliza un bloqueo pesimista para asegurar la unicidad en entornos concurrentes.
     * @param tipoDocumento El tipo de documento ("BOLETA" o "FACTURA").
     * @return El número de documento generado (ej: "B001-00000001", "F001-00000001").
     * @throws RuntimeException si no se puede generar el número de documento.
     */
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

    // --- CAMBIOS PARA ANULAR COMPRA (ANTES 'eliminarDocumentoCompra') ---
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
                    // Revertir el stock: restar la cantidad comprada para simular anulación de entrada
                    // En compras, cuando se anula, se debería REDUCIR el stock que se añadió.
                    // Si la compra añadió 10 unidades, al anular la compra, esas 10 unidades se quitan.
                    // Por lo tanto, la operación es RESTA aquí.
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
    // --- FIN CAMBIOS ANULAR COMPRA ---

    // Método auxiliar para actualizar stock (usado en anularDocumentoCompra)
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

        // Forzar la carga de relaciones para evitar LazyInitializationException en el PDF
        // Esto es crucial para que el PDF pueda acceder a Proveedor y DetalleCompra sin problemas
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

    /**
     * Predice el siguiente número de documento consecutivo para el tipo de documento especificado.
     * Este método NO incrementa el contador en la base de datos; es solo para pre-visualización.
     * @param tipoDocumento El tipo de documento ("BOLETA" o "FACTURA").
     * @return El número de documento predicho (ej: "B001-0000000X").
     * @throws IllegalArgumentException si el tipo de documento es inválido o no se encuentra la secuencia.
     */
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
