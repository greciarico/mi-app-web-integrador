package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.*;
import com.example.DyD_Natures.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private IgvRepository igvRepository;

    @Transactional(readOnly = true)
    public List<Venta> listarVentas() {
        List<Venta> ventas = ventaRepository.findAll();
        return ventas;
    }

    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorId(Integer id) {
        Optional<Venta> ventaOpt = ventaRepository.findById(id);
        return ventaOpt;
    }

    // --- Método de Guardado / Actualización 

    @Transactional
    public Venta guardarVenta(Venta ventaForm) {
        Venta ventaToManage;

        // 1. Obtener/Crear la entidad Venta gestionada
        if (ventaForm.getIdVenta() == null) {
            ventaToManage = new Venta();
            ventaToManage.setFechaRegistro(LocalDate.now());
            // Asegurarse de que el usuario por defecto exista.
            Usuario usuarioPorDefecto = usuarioRepository.findById(1)
                    .orElseThrow(() -> new RuntimeException("Usuario por defecto con ID 1 no encontrado. Asegúrate de tener un usuario con ID 1 en tu base de datos."));
            ventaToManage.setUsuario(usuarioPorDefecto);
        } else {
            ventaToManage = ventaRepository.findById(ventaForm.getIdVenta())
                    .orElseThrow(() -> new RuntimeException("Venta con ID " + ventaForm.getIdVenta() + " no encontrada para editar."));

            Set<Integer> incomingDetalleIds = ventaForm.getDetalleVentas().stream()
                    .filter(d -> d.getIdDetalleVenta() != null)
                    .map(DetalleVenta::getIdDetalleVenta)
                    .collect(Collectors.toSet());

            Iterator<DetalleVenta> iterator = ventaToManage.getDetalleVentas().iterator();
            while (iterator.hasNext()) {
                DetalleVenta existingDetalle = iterator.next();
                if (existingDetalle.getIdDetalleVenta() != null && !incomingDetalleIds.contains(existingDetalle.getIdDetalleVenta())) {
                    updateProductStock(existingDetalle.getProducto().getIdProducto(), existingDetalle.getCantidad());
                    iterator.remove();
                }
            }
        }

        // 2. Actualizar las propiedades principales de la venta
        ventaToManage.setTipoDocumento(ventaForm.getTipoDocumento());
        ventaToManage.setNumDocumento(ventaForm.getNumDocumento());
        ventaToManage.setTipoPago(ventaForm.getTipoPago());

        // 3. Validar y adjuntar Cliente e IGV (que ya deberían existir en DB)
        if (ventaForm.getCliente() == null || ventaForm.getCliente().getIdCliente() == null) {
            throw new IllegalArgumentException("El ID del cliente es obligatorio para la venta.");
        }
        Cliente cliente = clienteRepository.findById(ventaForm.getCliente().getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente con ID " + ventaForm.getCliente().getIdCliente() + " no encontrado."));
        ventaToManage.setCliente(cliente);

        if (ventaForm.getIgvEntity() == null || ventaForm.getIgvEntity().getIdIgv() == null) {
            throw new IllegalArgumentException("El ID del IGV es obligatorio para la venta.");
        }
        Igv igvEntity = igvRepository.findById(ventaForm.getIgvEntity().getIdIgv())
                .orElseThrow(() -> new IllegalArgumentException("IGV con ID " + ventaForm.getIgvEntity().getIdIgv() + " no encontrado."));
        ventaToManage.setIgvEntity(igvEntity);

        BigDecimal currentSubtotal = BigDecimal.ZERO;

        // 4. Procesar los detalles de venta entrantes (nuevos y actualizados)
        if (ventaForm.getDetalleVentas() == null || ventaForm.getDetalleVentas().isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un producto.");
        }

        Map<Integer, DetalleVenta> managedDetallesByProductId = ventaToManage.getDetalleVentas().stream()
                .filter(d -> d.getProducto() != null)
                .collect(Collectors.toMap(d -> d.getProducto().getIdProducto(), d -> d,
                        (existing, replacement) -> existing));

        List<DetalleVenta> detallesToAddOrUpdate = new ArrayList<>();

        for (DetalleVenta incomingDetalle : ventaForm.getDetalleVentas()) {
            if (incomingDetalle.getProducto() == null || incomingDetalle.getProducto().getIdProducto() == null) {
                throw new IllegalArgumentException("Detalle de venta inválido: Producto es obligatorio.");
            }
            if (incomingDetalle.getCantidad() == null || incomingDetalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("Detalle de venta inválido: Cantidad debe ser mayor a 0.");
            }

            Producto productoExistente = productoRepository.findById(incomingDetalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto con ID " + incomingDetalle.getProducto().getIdProducto() + " no encontrado."));

            BigDecimal finalPrecioUnitario = incomingDetalle.getPrecioUnitario();
            if (finalPrecioUnitario == null || finalPrecioUnitario.compareTo(BigDecimal.ZERO) < 0) {
                finalPrecioUnitario = productoExistente.getPrecio1();
                if (finalPrecioUnitario == null) {
                    throw new IllegalArgumentException("El producto " + productoExistente.getNombre() + " no tiene un precio unitario o precio1 definido.");
                }
            }
            incomingDetalle.setPrecioUnitario(finalPrecioUnitario);

            DetalleVenta existingDetailInManagedList = null;
            if (incomingDetalle.getIdDetalleVenta() != null) {
                existingDetailInManagedList = ventaToManage.getDetalleVentas().stream()
                        .filter(d -> Objects.equals(d.getIdDetalleVenta(), incomingDetalle.getIdDetalleVenta()))
                        .findFirst().orElse(null);
            }
            if (existingDetailInManagedList == null) {
                existingDetailInManagedList = managedDetallesByProductId.get(productoExistente.getIdProducto());
            }

            if (existingDetailInManagedList != null) {
                int oldQuantity = existingDetailInManagedList.getCantidad();
                int newQuantity = incomingDetalle.getCantidad();
                int quantityDifference = newQuantity - oldQuantity;

                if (quantityDifference != 0) {
                    if (productoExistente.getStock() < quantityDifference) {
                        throw new RuntimeException("Stock insuficiente para el producto: " + productoExistente.getNombre() + ". Stock actual: " + productoExistente.getStock() + ", aumento neto solicitado: " + quantityDifference);
                    }
                    updateProductStock(productoExistente.getIdProducto(), -quantityDifference);
                }

                existingDetailInManagedList.setCantidad(newQuantity);
                existingDetailInManagedList.setPrecioUnitario(finalPrecioUnitario);
                existingDetailInManagedList.setTotal(finalPrecioUnitario.multiply(new BigDecimal(newQuantity)));
                detallesToAddOrUpdate.add(existingDetailInManagedList);

            } else {
                if (productoExistente.getStock() < incomingDetalle.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + productoExistente.getNombre() + ". Stock actual: " + productoExistente.getStock() + ", intento de vender: " + incomingDetalle.getCantidad());
                }
                updateProductStock(productoExistente.getIdProducto(), -incomingDetalle.getCantidad());

                incomingDetalle.setProducto(productoExistente);
                incomingDetalle.setVenta(ventaToManage);
                incomingDetalle.setTotal(finalPrecioUnitario.multiply(new BigDecimal(incomingDetalle.getCantidad())));

                detallesToAddOrUpdate.add(incomingDetalle);
            }
            currentSubtotal = currentSubtotal.add(incomingDetalle.getTotal());
        }

        ventaToManage.getDetalleVentas().clear();
        ventaToManage.getDetalleVentas().addAll(detallesToAddOrUpdate);

        // 5. Calcular IGV y Total final
        BigDecimal porcentajeIgv = igvEntity.getIgv().divide(new BigDecimal(100));
        BigDecimal montoIgv = currentSubtotal.multiply(porcentajeIgv);
        ventaToManage.setIgv(montoIgv);
        ventaToManage.setTotal(currentSubtotal.add(montoIgv));

        try {
            Venta savedVenta = ventaRepository.save(ventaToManage);
            return savedVenta;
        } catch (Exception e) {
            throw new RuntimeException("Fallo al persistir la venta: " + e.getMessage(), e);
        }
    }

    @Transactional
    private void updateProductStock(Integer idProducto, Integer quantityChange) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto con ID " + idProducto + " no encontrado para actualizar stock."));

        int oldStock = producto.getStock();
        int newStock = oldStock + quantityChange;

        if (newStock < 0) {
            throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() + ". Stock actual: " + oldStock + ", intento de cambio: " + quantityChange);
        }
        producto.setStock(newStock);
        productoRepository.save(producto);
    }

    public List<Cliente> listarClientesActivos() {
        return clienteRepository.findAll();
    }

    public List<Producto> listarProductosActivos() {
        return productoRepository.findAll();
    }

    public List<Igv> listarIgvActivos() {
        return igvRepository.findAll();
    }

    public Optional<Usuario> obtenerUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

}
