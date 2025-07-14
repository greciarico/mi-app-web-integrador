package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Dto.ClienteFilterDTO;
import com.example.DyD_Natures.Dto.api.ReniecDataDTO;
import com.example.DyD_Natures.Dto.api.SunatDataDTO;
import com.example.DyD_Natures.Model.Cliente;
import com.example.DyD_Natures.Model.TipoCliente;
import com.example.DyD_Natures.Service.ClienteService;
import com.example.DyD_Natures.Service.TipoClienteService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TipoClienteService tipoClienteService;

    @GetMapping
    public String listarClientes(
            Model model,
            @RequestParam(required = false) Integer idTipoCliente,
            @RequestParam(required = false) String searchTerm) {

        model.addAttribute("clientes", clienteService.listarClientes(idTipoCliente, searchTerm));
        model.addAttribute("tiposCliente", tipoClienteService.listarTiposCliente());
        model.addAttribute("selectedTipoClienteId", idTipoCliente);
        model.addAttribute("searchTerm", searchTerm);
        return "cliente";
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Cliente> getAllClientesJson(
            @RequestParam(required = false) Integer idTipoCliente,
            @RequestParam(required = false) String searchTerm) {
        return clienteService.listarClientes(idTipoCliente, searchTerm);
    }

    // ClienteController
    @GetMapping("/activos")
    @ResponseBody
    public List<Cliente> getActiveClientesJson() {
        return clienteService.listarTodosLosClientesActivos();
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Cliente cliente = new Cliente();
        cliente.setTipoCliente(new TipoCliente());
        model.addAttribute("cliente", cliente);
        model.addAttribute("tiposCliente", tipoClienteService.listarTiposCliente());
        return "fragments/cliente_form_modal :: formContent";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Cliente> clienteOpt = clienteService.obtenerClientePorId(id);
        if (clienteOpt.isPresent()) {
            model.addAttribute("cliente", clienteOpt.get());
        } else {
            model.addAttribute("cliente", new Cliente());
            model.addAttribute("mensajeError", "Cliente no encontrado.");
        }
        model.addAttribute("tiposCliente", tipoClienteService.listarTiposCliente());
        return "fragments/cliente_form_modal :: formContent";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarCliente(@RequestBody Cliente cliente) {
        Map<String, String> response = new HashMap<>();
        try {
            clienteService.guardarCliente(cliente);
            response.put("status", "success");
            response.put("message", "Cliente guardado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al guardar el cliente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarCliente(@PathVariable("id") Integer id) {
        Map<String, String> response = new HashMap<>();
        try {
            clienteService.eliminarCliente(id);
            response.put("status", "success");
            response.put("message", "Cliente eliminado lógicamente exitosamente!");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al eliminar el cliente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verifica si un DNI ya existe.
     * @param dni DNI a verificar.
     * @param idCliente ID del cliente actual (para exclusión).
     * @return JSON con "exists": true/false.
     */
    @GetMapping("/checkDni")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkDni(@RequestParam String dni,
                                                         @RequestParam(required = false) Integer idCliente) {
        Map<String, Boolean> response = new HashMap<>();
        // CAMBIADO: Llamar al método expuesto por el servicio
        boolean exists = clienteService.existsByDniExcludingCurrent(dni, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si un RUC ya existe.
     * @param ruc RUC a verificar.
     * @param idCliente ID del cliente actual (para exclusión).
     * @return JSON con "exists": true/false.
     */
    @GetMapping("/checkRuc")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkRuc(@RequestParam String ruc,
                                                         @RequestParam(required = false) Integer idCliente) {
        Map<String, Boolean> response = new HashMap<>();
        // CAMBIADO: Llamar al método expuesto por el servicio
        boolean exists = clienteService.existsByRucExcludingCurrent(ruc, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
    // ===============================================
    // NUEVO ENDPOINT PARA CONSULTA DE API DE DOCUMENTOS
    // ===============================================

    /**
     * Endpoint para buscar datos de RENIEC/SUNAT basados en DNI o RUC.
     * @param tipoDocumento "dni" o "ruc"
     * @param numeroDocumento El número de DNI o RUC
     * @return ResponseEntity con los datos encontrados o un error.
     */
    @GetMapping("/buscarPorDocumento")
    @ResponseBody
    public ResponseEntity<?> buscarPorDocumento(@RequestParam String tipoDocumento, @RequestParam String numeroDocumento) {
        System.out.println("DEBUG: Entrando a buscarPorDocumento para tipo: " + tipoDocumento + ", numero: " + numeroDocumento);
        // ... el resto de tu código
        if ("dni".equalsIgnoreCase(tipoDocumento)) {
            if (numeroDocumento.length() != 8 || !numeroDocumento.matches("\\d+")) {
                return ResponseEntity.badRequest().body(Map.of("message", "El DNI debe tener 8 dígitos numéricos."));
            }
            Optional<ReniecDataDTO> data = clienteService.buscarReniecPorDni(numeroDocumento);
            if (data.isPresent()) {
                return ResponseEntity.ok(data.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "DNI no encontrado o error en la consulta RENIEC."));
            }
        } else if ("ruc".equalsIgnoreCase(tipoDocumento)) {
            if (numeroDocumento.length() != 11 || !numeroDocumento.matches("\\d+")) {
                return ResponseEntity.badRequest().body(Map.of("message", "El RUC debe tener 11 dígitos numéricos."));
            }
            Optional<SunatDataDTO> data = clienteService.buscarSunatPorRuc(numeroDocumento);
            if (data.isPresent()) {
                return ResponseEntity.ok(data.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "RUC no encontrado o error en la consulta SUNAT."));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Tipo de documento no válido. Use 'dni' o 'ruc'."));
        }
    }
    // ===============================================
    // NUEVOS ENDPOINTS PARA EL REPORTE DE CLIENTES
    // ===============================================

    /**
     * Muestra el fragmento del modal de filtros para el reporte de clientes.
     * También carga los tipos de cliente para el dropdown de filtro.
     * @param model El modelo para pasar datos a la vista.
     * @return La ruta al fragmento del modal.
     */
    @GetMapping("/reporte/modal")
    public String obtenerModalReporteClientes(Model model) {
        model.addAttribute("tiposCliente", tipoClienteService.listarTiposCliente());
        return "fragments/reporte_clientes_modal :: reporteModalContent";
    }

    /**
     * Genera y devuelve un informe PDF de clientes basado en los filtros.
     * @param filterDTO DTO con los criterios de filtrado para el reporte.
     * @param response Objeto HttpServletResponse para escribir el PDF.
     * @throws DocumentException Si ocurre un error al crear el documento PDF.
     * @throws IOException Si ocurre un error de E/S.
     */
    @GetMapping("/reporte/pdf")
    public void generarReporteClientePdf(@ModelAttribute ClienteFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=reporte_clientes.pdf";
        response.setHeader(headerKey, headerValue);

        List<Cliente> clientes = clienteService.buscarClientesPorFiltros(filterDTO);

        Document document = new Document(PageSize.A4.rotate()); // A4 horizontal
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Paragraph title = new Paragraph("Reporte de Clientes", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Mostrar filtros aplicados
        Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        StringBuilder filtrosAplicados = new StringBuilder("Filtros Aplicados:\n");

        if (filterDTO.getNombreCompletoODoc() != null && !filterDTO.getNombreCompletoODoc().isEmpty()) {
            filtrosAplicados.append("- Nombre/Documento: ").append(filterDTO.getNombreCompletoODoc()).append("\n");
        }
        if (filterDTO.getIdTipoCliente() != null) {
            Optional<TipoCliente> tipoClienteOpt = tipoClienteService.obtenerTipoClientePorId(filterDTO.getIdTipoCliente());
            tipoClienteOpt.ifPresent(tc -> filtrosAplicados.append("- Tipo de Cliente: ").append(tc.getRolCliente()).append("\n"));
        }
        if (filterDTO.getFechaRegistroStart() != null || filterDTO.getFechaRegistroEnd() != null) {
            filtrosAplicados.append("- Fecha de Registro: ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            if (filterDTO.getFechaRegistroStart() != null) filtrosAplicados.append("Desde ").append(filterDTO.getFechaRegistroStart().format(formatter)).append(" ");
            if (filterDTO.getFechaRegistroEnd() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getFechaRegistroEnd().format(formatter));
            filtrosAplicados.append("\n");
        }
        if (filterDTO.getDireccion() != null && !filterDTO.getDireccion().isEmpty()) {
            filtrosAplicados.append("- Dirección: ").append(filterDTO.getDireccion()).append("\n");
        }
        if (filterDTO.getTelefono() != null && !filterDTO.getTelefono().isEmpty()) {
            filtrosAplicados.append("- Teléfono: ").append(filterDTO.getTelefono()).append("\n");
        }
        // NUEVO: Mostrar filtro de estado aplicado
        if (filterDTO.getEstado() != null) {
            String estadoTexto = "";
            if (filterDTO.getEstado() == 1) {
                estadoTexto = "Activo";
            } else if (filterDTO.getEstado() == 0) {
                estadoTexto = "Inactivo";
            } else {
                // Este caso no debería ocurrir si se elimina la opción de 2 del HTML
                estadoTexto = "Desconocido/No Reportable";
            }
            filtrosAplicados.append("- Estado: ").append(estadoTexto).append("\n");
        } else {
            // Si el estado es null, significa "Todos (Activos e Inactivos)"
            filtrosAplicados.append("- Estado: Activos e Inactivos\n");
        }


        if (filtrosAplicados.toString().equals("Filtros Aplicados:\n")) { // Si solo queda el título
            filtrosAplicados.append("- Ninguno (Reporte Completo de Clientes)\n"); // CAMBIADO
        }

        Paragraph pFiltros = new Paragraph(filtrosAplicados.toString(), fontFilters);
        pFiltros.setAlignment(Paragraph.ALIGN_LEFT);
        pFiltros.setSpacingAfter(10);
        document.add(pFiltros);

        // Crear la tabla PDF
        // Columnas: ID, Tipo Cliente, DNI/RUC, Nombre/Razon Social, Teléfono, Dirección, F. Registro, Estado
        PdfPTable table = new PdfPTable(8); // Aumentar a 8 columnas
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Ajustar anchos de columnas para incluir el Estado
        float[] columnWidths = {0.5f, 1f, 1f, 2f, 0.8f, 2f, 1f, 0.8f}; // Añadido ancho para Estado
        table.setWidths(columnWidths);

        PdfPCell cell;
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.BLACK);
        Font fontContentActive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(0, 128, 0)); // Verde
        Font fontContentInactive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(255, 0, 0)); // Rojo

        String[] headers = {"ID", "Tipo Cliente", "Doc. Identidad", "Nombre/Raz. Social", "Teléfono", "Dirección", "F. Registro", "Estado"}; // Añadir "Estado"
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        if (clientes.isEmpty()) {
            cell = new PdfPCell(new Phrase("No se encontraron registros de clientes con los filtros aplicados.", fontContent));
            cell.setColspan(8); // Colspan ajustado a 8
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Cliente cliente : clientes) {
                table.addCell(new Phrase(String.valueOf(cliente.getIdCliente()), fontContent));
                table.addCell(new Phrase(cliente.getTipoCliente() != null ? cliente.getTipoCliente().getRolCliente() : "N/A", fontContent));

                // Documento de Identidad
                String docIdentidad = "N/A";
                if (cliente.getDni() != null && !cliente.getDni().isEmpty()) {
                    docIdentidad = "DNI: " + cliente.getDni();
                } else if (cliente.getRuc() != null && !cliente.getRuc().isEmpty()) {
                    docIdentidad = "RUC: " + cliente.getRuc();
                }
                table.addCell(new Phrase(docIdentidad, fontContent));

                // Nombre / Razón Social
                String nombreRazonSocial = "N/A";
                if (cliente.getTipoCliente() != null) { // Asegurar que tipoCliente no sea null
                    if (cliente.getTipoCliente().getIdRolCliente() == 1) { // Natural
                        nombreRazonSocial = String.format("%s %s %s",
                                Optional.ofNullable(cliente.getNombre()).orElse(""),
                                Optional.ofNullable(cliente.getApPaterno()).orElse(""),
                                Optional.ofNullable(cliente.getApMaterno()).orElse("")).trim();
                    } else if (cliente.getTipoCliente().getIdRolCliente() == 2) { // Jurídica
                        nombreRazonSocial = Optional.ofNullable(cliente.getRazonSocial()).orElse("N/A");
                    }
                }
                table.addCell(new Phrase(nombreRazonSocial, fontContent));


                table.addCell(new Phrase(cliente.getTelefono() != null ? cliente.getTelefono() : "N/A", fontContent));
                table.addCell(new Phrase(cliente.getDireccion() != null && !cliente.getDireccion().isEmpty() ? cliente.getDireccion() : "Sin dirección", fontContent));
                table.addCell(new Phrase(cliente.getFechaRegistro() != null ? cliente.getFechaRegistro().format(formatter) : "N/A", fontContent));

                // NUEVO: Celda para el Estado
                String estadoTexto;
                Font estadoFont;
                if (cliente.getEstado() == 1) {
                    estadoTexto = "Activo";
                    estadoFont = fontContentActive;
                } else if (cliente.getEstado() == 0) {
                    estadoTexto = "Inactivo";
                    estadoFont = fontContentInactive;
                } else {
                    estadoTexto = "Eliminado"; // Aunque no deberíamos llegar aquí si el filtro funciona
                    estadoFont = fontContent; // O definir un color para "Eliminado" si se diera el caso
                }
                table.addCell(new Phrase(estadoTexto, estadoFont));
            }
        }

        document.add(table);
        document.close();
    }
}
