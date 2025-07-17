package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.VentaFilterDTO;
import com.example.DyD_Natures.Model.*;
import com.example.DyD_Natures.Repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification; // Importar Specification
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
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

    // --- Métodos de Lectura ---

    @Transactional(readOnly = true)
    public List<Venta> listarVentas() {
        return ventaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorId(Integer id) {
        return ventaRepository.findById(id);
    }

    // --- Método de Guardado / Actualización (CRÍTICO) ---

    @Transactional
    public Venta guardarVenta(Venta ventaForm) {
        Venta ventaToManage;

        // 1) Obtener/Crear la entidad Venta gestionada
        if (ventaForm.getIdVenta() == null) {
            ventaToManage = new Venta();
            ventaToManage.setFechaRegistro(LocalDate.now());

            // → En lugar de “usuarioPorDefecto”, usa el que vino del Controller
            if (ventaForm.getUsuario() == null || ventaForm.getUsuario().getIdUsuario() == null) {
                throw new IllegalArgumentException("No se pudo determinar el usuario autenticado para la venta.");
            }
            ventaToManage.setUsuario(ventaForm.getUsuario());
        } else {
            ventaToManage = ventaRepository.findById(ventaForm.getIdVenta())
                    .orElseThrow(() -> new RuntimeException("Venta con ID " + ventaForm.getIdVenta() + " no encontrada para editar."));
            // (opcional) podrías permitir cambiar el usuario aquí, si quisieras:
            // ventaToManage.setUsuario(ventaForm.getUsuario());
        }

        // 2) Actualizar las propiedades principales de la venta
        ventaToManage.setTipoDocumento(ventaForm.getTipoDocumento());
        ventaToManage.setNumDocumento(ventaForm.getNumDocumento());
        ventaToManage.setTipoPago(ventaForm.getTipoPago());

        // 3) Validar y adjuntar Cliente e IGV
        Cliente cliente = clienteRepository.findById(
                Optional.ofNullable(ventaForm.getCliente())
                        .map(Cliente::getIdCliente)
                        .orElseThrow(() -> new IllegalArgumentException("El ID del cliente es obligatorio para la venta."))
        ).orElseThrow(() -> new IllegalArgumentException(
                "Cliente con ID " + ventaForm.getCliente().getIdCliente() + " no encontrado."));

        Igv igvEntity = igvRepository.findById(
                Optional.ofNullable(ventaForm.getIgvEntity())
                        .map(Igv::getIdIgv)
                        .orElseThrow(() -> new IllegalArgumentException("El ID del IGV es obligatorio para la venta."))
        ).orElseThrow(() -> new IllegalArgumentException(
                "IGV con ID " + ventaForm.getIgvEntity().getIdIgv() + " no encontrado."));

        ventaToManage.setCliente(cliente);
        ventaToManage.setIgvEntity(igvEntity);

        // 4) Procesar detalles, stock, subtotal, IGV y total...
        BigDecimal currentSubtotal = BigDecimal.ZERO;
        List<DetalleVenta> detallesToSave = new ArrayList<>();

        for (DetalleVenta dForm : ventaForm.getDetalleVentas()) {
            // ...validaciones, búsqueda de producto, ajuste de stock...
            Producto prod = productoRepository.findById(dForm.getProducto().getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto con ID " + dForm.getProducto().getIdProducto() + " no encontrado."));

            int qty = dForm.getCantidad();
            BigDecimal price = Optional.ofNullable(dForm.getPrecioUnitario()).orElse(prod.getPrecio1());
            BigDecimal totalDetalle = price.multiply(BigDecimal.valueOf(qty));

            // actualizar stock
            if (prod.getStock() < qty) {
                throw new RuntimeException("Stock insuficiente para el producto: " + prod.getNombre());
            }
            updateProductStock(prod.getIdProducto(), -qty);

            // montar detalle
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(prod);
            detalle.setCantidad(qty);
            detalle.setPrecioUnitario(price);
            detalle.setTotal(totalDetalle);
            detalle.setVenta(ventaToManage);

            detallesToSave.add(detalle);
            currentSubtotal = currentSubtotal.add(totalDetalle);
        }

        ventaToManage.getDetalleVentas().clear();
        ventaToManage.getDetalleVentas().addAll(detallesToSave);

        // 5) Calcular IGV y total final
        BigDecimal porcentajeIgv = igvEntity.getTasa();           // ej. 0.18
        BigDecimal montoIgv     = currentSubtotal.multiply(porcentajeIgv);
        ventaToManage.setIgv(montoIgv);
        ventaToManage.setTotal(currentSubtotal.add(montoIgv));

        // 6) Guarda y devuelve
        return ventaRepository.save(ventaToManage);
    }

    // El resto de la clase (cancelarVenta, updateProductStock, etc.) se mantiene igual...

    // METODO ACTUALIZADO PARA CANCELAR VENTA
    @Transactional
    public void cancelarVenta(Integer idVenta, Usuario usuarioAnulacion) { // Añadir Usuario como parámetro
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + idVenta));

        // --- Nueva validación de fecha ---
        if (!venta.getFechaRegistro().isEqual(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se pueden anular ventas realizadas el mismo día.");
        }
        // --- Fin nueva validación ---

        if (venta.getEstado() != null && venta.getEstado() == 0) {
            throw new IllegalArgumentException("La Venta con ID " + idVenta + " ya está cancelada.");
        }

        if (venta.getDetalleVentas() != null && !venta.getDetalleVentas().isEmpty()) {
            venta.getDetalleVentas().forEach(detalle -> {
                if (detalle.getProducto() != null) {
                    // Revertir el stock: sumar la cantidad vendida
                    updateProductStock(detalle.getProducto().getIdProducto(), detalle.getCantidad());
                }
            });
        }

        venta.setEstado((byte) 0); // Cambia el estado a 0 (cancelada)
        venta.setFechaAnulacion(LocalDateTime.now()); // Establece la fecha y hora de anulación
        venta.setUsuarioAnulacion(usuarioAnulacion); // Establece el usuario que anuló
        ventaRepository.save(venta); // Persiste el cambio
    }

    // Método auxiliar para actualizar stock (usado en cancelarVenta)
    @Transactional
    private void updateProductStock(Integer idProducto, Integer quantityChange) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado para actualizar stock: ID " + idProducto));

        int newStock = producto.getStock() + quantityChange; // quantityChange es positivo (se suma)
        if (newStock < 0) {
            // Esto no debería ocurrir si la cantidad original era positiva, pero es una buena salvaguarda
            throw new RuntimeException("Error: El stock resultante para el producto '" + producto.getNombre() + "' sería negativo.");
        }
        producto.setStock(newStock);
        productoRepository.save(producto);
    }


    // Métodos de utilidad que ya tenías
    public List<Cliente> listarClientesActivos() { return clienteRepository.findAll(); }
    public List<Producto> listarProductosActivos() { return productoRepository.findAll(); }
    public List<Igv> listarIgvActivos() { return igvRepository.findAll(); }
    public Optional<Usuario> obtenerUsuarioPorId(Integer id) { return usuarioRepository.findById(id); }
// --- Nuevos métodos para Reportes ---

    @Transactional(readOnly = true)
    public List<Venta> buscarVentasPorFiltros(VentaFilterDTO filterDTO) {
        return ventaRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Si se busca por nombre de cliente (RazonSocial o Nombre + Apellidos)
            if (filterDTO.getNombreCliente() != null && !filterDTO.getNombreCliente().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreCliente().toLowerCase() + "%";
                Join<Venta, Cliente> clienteJoin = root.join("cliente");
                Predicate byRazonSocial = criteriaBuilder.like(criteriaBuilder.lower(clienteJoin.get("razonSocial")), searchTerm);
                Predicate byNombreCompleto = criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(criteriaBuilder.concat(clienteJoin.get("nombre"), " "), criteriaBuilder.concat(clienteJoin.get("apPaterno"), " "))), searchTerm);
                Predicate byNombreApPaterno = criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(clienteJoin.get("nombre"), " ")), searchTerm); // Para buscar por nombre + apellido paterno
                // Combina las condiciones OR para el nombre del cliente
                predicates.add(criteriaBuilder.or(byRazonSocial, byNombreCompleto, byNombreApPaterno));
            }

            // Filtrar por ID de Cliente
            if (filterDTO.getIdCliente() != null) {
                predicates.add(criteriaBuilder.equal(root.get("cliente").get("idCliente"), filterDTO.getIdCliente()));
            }

            // Filtrar por ID de Usuario (vendedor)
            if (filterDTO.getIdUsuario() != null) {
                predicates.add(criteriaBuilder.equal(root.get("usuario").get("idUsuario"), filterDTO.getIdUsuario()));
            }

            // Filtrar por Tipo de Documento
            if (filterDTO.getTipoDocumento() != null && !filterDTO.getTipoDocumento().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("tipoDocumento")), "%" + filterDTO.getTipoDocumento().toLowerCase() + "%"));
            }

            // Filtrar por Número de Documento
            if (filterDTO.getNumDocumento() != null && !filterDTO.getNumDocumento().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numDocumento")), "%" + filterDTO.getNumDocumento().toLowerCase() + "%"));
            }

            // Filtrar por Tipo de Pago
            if (filterDTO.getTipoPago() != null && !filterDTO.getTipoPago().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("tipoPago")), "%" + filterDTO.getTipoPago().toLowerCase() + "%"));
            }

            // Filtrar por Rango de Fechas
            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }

            // Filtrar por Rango de Total
            if (filterDTO.getTotalMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("total"), filterDTO.getTotalMin()));
            }
            if (filterDTO.getTotalMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("total"), filterDTO.getTotalMax()));
            }

            // Filtrar por Estados (0=Cancelada, 1=Activa)
            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                predicates.add(root.get("estado").in(filterDTO.getEstados()));
            } else {
                // Por defecto, excluimos las ventas eliminadas lógicamente si no se especifica el estado 0
                // Asumo que '2' no se usa para ventas. Si sí, ajustar esta lógica.
                // Aquí, el estado 0 es "cancelada", 1 es "activa".
                // Si el usuario no selecciona ningún estado, muestra solo las "activas" (estado 1)
                // Si selecciona "cancelada" o "activa", usa lo que seleccionó.
                if (filterDTO.getEstados() == null || filterDTO.getEstados().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("estado"), (byte) 1)); // Solo activas por defecto
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }

    public void generarReportePdf(VentaFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, java.io.IOException {
        Document document = new Document(PageSize.A4.rotate()); // Tamaño A4 en horizontal

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_ventas.pdf\"");

        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // Fuentes
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
        Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY);
        Font fontContentCancelled = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(114, 28, 36)); // Rojo oscuro para canceladas


        // Título del Documento
        Paragraph title = new Paragraph("REPORTE DE VENTAS", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Subtítulo de Filtros Aplicados
        Paragraph filtersSubtitle = new Paragraph("Filtros Aplicados:", fontSubtitle);
        filtersSubtitle.setSpacingAfter(5);
        document.add(filtersSubtitle);

        Paragraph filterDetails = new Paragraph(filterDTO.toString(), fontContent);
        filterDetails.setSpacingAfter(10);
        document.add(filterDetails);

        // Obtener ventas filtradas
        List<Venta> ventas = buscarVentasPorFiltros(filterDTO);

        if (ventas.isEmpty()) {
            document.add(new Paragraph("No se encontraron ventas con los filtros especificados.", fontContent));
            document.close();
            return;
        }

        // Tabla de Ventas
        PdfPTable table = new PdfPTable(8); // 8 columnas
        table.setWidthPercentage(100); // Ancho de la tabla
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        table.setWidths(new float[]{0.8f, 1.2f, 2f, 1.5f, 1.2f, 1.5f, 1f, 1f}); // Anchos relativos de las columnas

        // Encabezados de la tabla
        String[] headers = {"ID Venta", "Fecha", "Cliente", "Usuario", "Tipo Doc.", "N° Documento", "Total Venta", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0)); // Color de fondo oscuro (mismo de productos)
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Filas de datos
        BigDecimal totalGeneralReporte = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            Font currentFont = fontContent;
            String estadoText = "Activa"; // Por defecto
            if (venta.getEstado() != null && venta.getEstado() == 0) {
                estadoText = "Cancelada";
                currentFont = fontContentCancelled;
            } else if (venta.getEstado() != null && venta.getEstado() == 1) {
                estadoText = "Activa";
                currentFont = fontContent;
            }

            // ID Venta
            PdfPCell cellId = new PdfPCell(new Phrase(String.valueOf(venta.getIdVenta()), currentFont));
            cellId.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellId);

            // Fecha
            PdfPCell cellFecha = new PdfPCell(new Phrase(venta.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), currentFont));
            cellFecha.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellFecha);

            // Cliente
            String clienteDisplayName = "";
            if (venta.getCliente() != null) {
                if (venta.getCliente().getRazonSocial() != null && !venta.getCliente().getRazonSocial().trim().isEmpty()) {
                    clienteDisplayName = venta.getCliente().getRazonSocial();
                } else if (venta.getCliente().getNombre() != null) {
                    clienteDisplayName = venta.getCliente().getNombre();
                    if (venta.getCliente().getApPaterno() != null) clienteDisplayName += " " + venta.getCliente().getApPaterno();
                    if (venta.getCliente().getApMaterno() != null) clienteDisplayName += " " + venta.getCliente().getApMaterno();
                }
            }
            PdfPCell cellCliente = new PdfPCell(new Phrase(clienteDisplayName, currentFont));
            table.addCell(cellCliente);

            // Usuario (Vendedor)
            String usuarioNombre = (venta.getUsuario() != null && venta.getUsuario().getNombre() != null) ? venta.getUsuario().getNombre() : "N/A";
            PdfPCell cellUsuario = new PdfPCell(new Phrase(usuarioNombre, currentFont));
            table.addCell(cellUsuario);

            // Tipo Documento
            PdfPCell cellTipoDoc = new PdfPCell(new Phrase(venta.getTipoDocumento(), currentFont));
            cellTipoDoc.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellTipoDoc);

            // N° Documento
            PdfPCell cellNumDoc = new PdfPCell(new Phrase(venta.getNumDocumento(), currentFont));
            cellNumDoc.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellNumDoc);

            // Total Venta
            PdfPCell cellTotal = new PdfPCell(new Phrase("S/ " + venta.getTotal().setScale(2, BigDecimal.ROUND_HALF_UP).toString(), currentFont));
            cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cellTotal);
            if (venta.getEstado() != null && venta.getEstado() == 1) { // Solo sumar si la venta está activa
                totalGeneralReporte = totalGeneralReporte.add(venta.getTotal());
            }


            // Estado
            PdfPCell cellEstado = new PdfPCell(new Phrase(estadoText, currentFont));
            cellEstado.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellEstado);
        }
        document.add(table);

        // Resumen del reporte
        Paragraph summary = new Paragraph();
        summary.setAlignment(Element.ALIGN_RIGHT);
        summary.setSpacingBefore(10);
        summary.add(new Phrase("Total General de Ventas Activas: ", fontSubtitle));
        summary.add(new Phrase("S/ " + totalGeneralReporte.setScale(2, BigDecimal.ROUND_HALF_UP).toString(), fontTitle));
        document.add(summary);

        // Pie de página
        Paragraph footer = new Paragraph("Reporte generado el " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontFooter);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();
    }
    /**
     * Genera un comprobante de venta (boleta/factura) en formato PDF para una venta específica.
     * @param idVenta El ID de la venta para la cual se generará el comprobante.
     * @param response HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si hay un error al generar el documento PDF.
     * @throws IOException Si hay un error de E/S.
     */
    public void generarComprobanteVentaPdf(Integer idVenta, HttpServletResponse response) throws DocumentException, IOException {
        Optional<Venta> ventaOptional = ventaRepository.findById(idVenta);
        if (ventaOptional.isEmpty()) {
            throw new EntityNotFoundException("Venta no encontrada con ID: " + idVenta);
        }
        Venta venta = ventaOptional.get();

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        // --- FUENTES ---
        Font fontHeaderEmpresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Font fontAddress = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
        Font fontTableHeaders = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);

        // --- ENCABEZADO DE EMPRESA (TÍTULO) ---
        Paragraph companyName = new Paragraph("DYD NATURE´S S.A.C.", fontHeaderEmpresa);
        companyName.setAlignment(Element.ALIGN_CENTER);
        document.add(companyName);

        Paragraph companyAddress = new Paragraph("RUC: 10418074073\nAv. Arica & Héroes Civiles, Chiclayo, Perú\nTeléfono: 917 144 866\nGmail: dydnatures@gmail.com", fontAddress);
        companyAddress.setAlignment(Element.ALIGN_CENTER);
        companyAddress.setSpacingAfter(15);
        document.add(companyAddress);

        // --- TIPO Y NÚMERO DE DOCUMENTO DE VENTA ---
        String tipoDocVenta = venta.getTipoDocumento().toUpperCase();
        String numDocVenta = venta.getNumDocumento();
        Paragraph receiptTitle = new Paragraph(tipoDocVenta + " ELECTRÓNICA\n" + numDocVenta, fontTitle);
        receiptTitle.setAlignment(Element.ALIGN_CENTER);
        receiptTitle.setSpacingAfter(20);
        document.add(receiptTitle);

        // --- DATOS DEL CLIENTE Y VENTA ---
        PdfPTable infoTable = new PdfPTable(2); // Dos columnas para cliente y venta
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);
        infoTable.setWidths(new float[]{1, 1});

        // Columna de Cliente
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.addElement(new Paragraph("CLIENTE:", fontSubtitle));
        clientCell.addElement(new Paragraph("Razón Social/Nombres: " + getClientDisplayName(venta.getCliente()), fontContent));

        // *********************************************************************************
        // CAMBIO AQUÍ: Adaptar la obtención del tipo y número de documento de identidad del cliente
        // *********************************************************************************
        Cliente cliente = venta.getCliente();
        String tipoDocIdentidadCliente = "N/A";
        String numDocIdentidadCliente = "N/A";

        if (cliente != null && cliente.getTipoCliente() != null) {
            if (cliente.getTipoCliente().getIdRolCliente().equals(1)) { // Asumiendo ID 1 para Persona Natural
                tipoDocIdentidadCliente = "DNI";
                numDocIdentidadCliente = cliente.getDni() != null ? cliente.getDni() : "N/A";
            } else if (cliente.getTipoCliente().getIdRolCliente().equals(2)) { // Asumiendo ID 2 para Persona Jurídica
                tipoDocIdentidadCliente = "RUC";
                numDocIdentidadCliente = cliente.getRuc() != null ? cliente.getRuc() : "N/A";
            }
        }

        clientCell.addElement(new Paragraph("Tipo Doc. Identidad: " + tipoDocIdentidadCliente, fontContent));
        clientCell.addElement(new Paragraph("N° Doc. Identidad: " + numDocIdentidadCliente, fontContent));
        // *********************************************************************************
        // FIN CAMBIO
        // *********************************************************************************

        clientCell.addElement(new Paragraph("Dirección: " + (venta.getCliente().getDireccion() != null ? venta.getCliente().getDireccion() : "N/A"), fontContent));
        infoTable.addCell(clientCell);

        // Columna de Datos de Venta
        PdfPCell saleDataCell = new PdfPCell();
        saleDataCell.setBorder(Rectangle.NO_BORDER);
        saleDataCell.addElement(new Paragraph("DATOS DE VENTA:", fontSubtitle));
        saleDataCell.addElement(new Paragraph("Fecha de Emisión: " + venta.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontContent));
        saleDataCell.addElement(new Paragraph("Vendedor: " + (venta.getUsuario() != null ? venta.getUsuario().getNombre() : "N/A"), fontContent));
        saleDataCell.addElement(new Paragraph("Tipo de Pago: " + venta.getTipoPago(), fontContent));
        saleDataCell.addElement(new Paragraph("Estado: " + (venta.getEstado() == 1 ? "ACTIVA" : "CANCELADA"), fontContent));
        infoTable.addCell(saleDataCell);

        document.add(infoTable);

        // --- DETALLE DE PRODUCTOS ---
        PdfPTable detailTable = new PdfPTable(5); // Cantidad, Descripción, P. Unitario, Importe, Dcto (si aplica)
        detailTable.setWidthPercentage(100);
        detailTable.setSpacingAfter(15);
        detailTable.setWidths(new float[]{0.8f, 3f, 1f, 1f, 1f}); // Anchos relativos

        // Encabezados de la tabla de detalles
        String[] detailHeaders = {"Cant.", "Descripción", "P. Unit.", "Importe", "Descuento"};
        for (String header : detailHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fontTableHeaders));
            cell.setBackgroundColor(BaseColor.GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            detailTable.addCell(cell);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVenta detalle : venta.getDetalleVentas()) {
            detailTable.addCell(createCell(String.valueOf(detalle.getCantidad()), fontContent, Element.ALIGN_CENTER));
            detailTable.addCell(createCell(detalle.getProducto().getNombre(), fontContent, Element.ALIGN_LEFT));
            detailTable.addCell(createCell("S/ " + detalle.getPrecioUnitario().setScale(2, BigDecimal.ROUND_HALF_UP).toString(), fontContent, Element.ALIGN_RIGHT));
            BigDecimal importeLinea = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
            detailTable.addCell(createCell("S/ " + importeLinea.setScale(2, BigDecimal.ROUND_HALF_UP).toString(), fontContent, Element.ALIGN_RIGHT));
            detailTable.addCell(createCell("S/ 0.00", fontContent, Element.ALIGN_RIGHT));
            subtotal = subtotal.add(importeLinea);
        }
        document.add(detailTable);

        // --- TOTALES ---
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingAfter(20);
        totalsTable.setWidths(new float[]{1.5f, 1f});

        totalsTable.addCell(createCell("SUBTOTAL:", fontSubtitle, Element.ALIGN_RIGHT));
        totalsTable.addCell(createCell("S/ " + subtotal.setScale(2, BigDecimal.ROUND_HALF_UP).toString(), fontTotal, Element.ALIGN_RIGHT));

        totalsTable.addCell(createCell("IGV (" + venta.getIgvEntity().getTasa().multiply(new BigDecimal(100)).stripTrailingZeros().toPlainString() + "%):", fontSubtitle, Element.ALIGN_RIGHT));
        totalsTable.addCell(createCell("S/ " + venta.getIgv().setScale(2, BigDecimal.ROUND_HALF_UP).toString(), fontTotal, Element.ALIGN_RIGHT));

        totalsTable.addCell(createCell("TOTAL A PAGAR:", fontSubtitle, Element.ALIGN_RIGHT));
        totalsTable.addCell(createCell("S/ " + venta.getTotal().setScale(2, BigDecimal.ROUND_HALF_UP).toString(), fontTotal, Element.ALIGN_RIGHT));

        document.add(totalsTable);

        // --- NOTA FINAL ---
        Paragraph finalNote = new Paragraph("Gracias por su compra. Todos los precios incluyen IGV.", fontContent);
        finalNote.setAlignment(Element.ALIGN_CENTER);
        document.add(finalNote);

        document.close();

        // Configurar la respuesta HTTP
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"comprobante_venta_" + venta.getNumDocumento() + ".pdf\"");
        response.setContentLength(baos.size());
        baos.writeTo(response.getOutputStream());
        response.getOutputStream().flush();
    }

    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String getClientDisplayName(Cliente client) {
        if (client == null) {
            return "Cliente Desconocido";
        }
        if (client.getRazonSocial() != null && !client.getRazonSocial().trim().isEmpty()) {
            return client.getRazonSocial();
        }
        String fullName = "";
        if (client.getNombre() != null) fullName += client.getNombre().trim();
        if (client.getApPaterno() != null) fullName += " " + client.getApPaterno().trim();
        if (client.getApMaterno() != null) fullName += " " + client.getApMaterno().trim();
        return fullName.trim().isEmpty() ? "Cliente Desconocido" : fullName.trim();
    }

}
