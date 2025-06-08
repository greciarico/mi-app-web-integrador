package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.DetalleCompra;
import com.example.DyD_Natures.Model.DocumentoCompra;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Repository.DocumentoCompraRepository;
import com.example.DyD_Natures.Repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentoCompraService {

    @Autowired
    private DocumentoCompraRepository documentoCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

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
     * Elimina un DocumentoCompra y sus DetalleCompra asociados, revirtiendo el stock de los productos.
     * @param id El ID del documento de compra a eliminar.
     * @throws EntityNotFoundException Si el documento de compra no se encuentra.
     * @throws RuntimeException Si un producto asociado no se encuentra o el stock se volvería negativo.
     */
    @Transactional // Asegura que toda la operación sea atómica
    public void eliminarDocumentoCompra(Integer id) {
        Optional<DocumentoCompra> documentoOpt = documentoCompraRepository.findById(id);
        if (documentoOpt.isEmpty()) {
            throw new EntityNotFoundException("Documento de Compra no encontrado con ID: " + id);
        }
        DocumentoCompra documento = documentoOpt.get();

        // Revertir el stock de los productos de los detalles de compra
        if (documento.getDetalleCompras() != null && !documento.getDetalleCompras().isEmpty()) {
            documento.getDetalleCompras().size(); // Forzar la carga de la colección si es LAZY
            for (DetalleCompra detalle : new ArrayList<>(documento.getDetalleCompras())) {
                if (detalle.getProducto() != null) {
                    detalle.getProducto().getIdProducto(); // Forzar la carga del producto si es LAZY
                    updateProductStock(detalle.getProducto().getIdProducto(), -detalle.getCantidad()); // LLAMA CON CANTIDAD NEGATIVA PARA RESTAR EL STOCK
                }
            }
        }

        documentoCompraRepository.delete(documento);
    }

    /**
     * Actualiza el stock de un producto sumando o restando una cantidad.
     * @param idProducto El ID del producto.
     * @param cantidadChange La cantidad a cambiar del stock. Positiva para sumar, negativa para restar.
     * @throws EntityNotFoundException si el producto no se encuentra.
     * @throws RuntimeException si el stock se vuelve negativo después de la operación de resta.
     */
    private void updateProductStock(Integer idProducto, Integer cantidadChange) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new EntityNotFoundException("Producto con ID " + idProducto + " no encontrado para actualizar stock."));

        int currentStock = producto.getStock();
        int newStock = currentStock + cantidadChange;

        // Validar stock negativo SOLO si la operación es una RESTA (cantidadChange < 0)
        if (cantidadChange < 0 && newStock < 0) {
            throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() +
                    ". Stock actual: " + currentStock + ", intento de reducir en: " + (-cantidadChange) +
                    ". El stock resultante sería: " + newStock);
        }

        producto.setStock(newStock);
        productoRepository.save(producto);
    }
}
