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
import jakarta.servlet.http.HttpServletRequest;
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
    public String listarClientes(HttpServletRequest request,
                                 Model model,
                                 @RequestParam(required = false) Integer idTipoCliente,
                                 @RequestParam(required = false) String searchTerm) {

        model.addAttribute("currentUri", request.getRequestURI());
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
    // NUEVO/MODIFICADO ENDPOINT PARA FILTRADO DE TABLA (COMO EN PRODUCTOS)
    // ===============================================

    /**
     * Nuevo endpoint para buscar/filtrar clientes en la tabla principal vía AJAX.
     * Recibe los filtros del ClienteFilterDTO y devuelve una lista de clientes en formato JSON.
     * @param filterDTO DTO con los criterios de filtrado.
     * @return Lista de clientes que coinciden con los filtros.
     */
    @GetMapping("/buscar")
    @ResponseBody
    public List<Cliente> buscarClientes(@ModelAttribute ClienteFilterDTO filterDTO) {
        // Reutiliza el mismo servicio y lógica que usas para el reporte.
        // Asegúrate de que buscarClientesPorFiltros en ClienteService maneje bien los nulls
        // si un filtro no se proporciona desde el frontend.
        return clienteService.buscarClientesPorFiltros(filterDTO);
    }
    // El endpoint de reporte PDF se mantiene igual, ya que ClienteFilterDTO es completo
    @GetMapping("/reporte/pdf")
    public void generarReporteClientePdf(@ModelAttribute ClienteFilterDTO filterDTO, HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=reporte_clientes.pdf";
        response.setHeader(headerKey, headerValue);

        List<Cliente> clientes = clienteService.buscarClientesPorFiltros(filterDTO); // Usa el mismo método de filtrado

        Document document = new Document(PageSize.A4.rotate()); // A4 horizontal
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Paragraph title = new Paragraph("Reporte de Clientes", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // --- Mostrar filtros aplicados (como en productos) ---
        Font fontFilters = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        StringBuilder filtrosAplicados = new StringBuilder("Filtros Aplicados:\n");

        if (filterDTO.getNombreCompletoODoc() != null && !filterDTO.getNombreCompletoODoc().isEmpty()) {
            filtrosAplicados.append("- Nombre, Apellidos, DNI o RUC: ").append(filterDTO.getNombreCompletoODoc()).append("\n");
        }
        if (filterDTO.getIdTipoCliente() != null) {
            // CORRECCIÓN AQUÍ: Ahora el método existe en clienteService
            clienteService.obtenerTipoClientePorId(filterDTO.getIdTipoCliente())
                    .ifPresent(tipo -> filtrosAplicados.append("- Tipo de Cliente: ").append(tipo.getRolCliente()).append("\n"));
        }
        if (filterDTO.getDireccion() != null && !filterDTO.getDireccion().isEmpty()) {
            filtrosAplicados.append("- Dirección: ").append(filterDTO.getDireccion()).append("\n");
        }
        if (filterDTO.getTelefono() != null && !filterDTO.getTelefono().isEmpty()) {
            filtrosAplicados.append("- Teléfono: ").append(filterDTO.getTelefono()).append("\n");
        }
        if (filterDTO.getEstado() != null) {
            filtrosAplicados.append("- Estado: ").append(filterDTO.getEstado() == 1 ? "Activo" : (filterDTO.getEstado() == 0 ? "Inactivo" : "Desconocido")).append("\n");
        }
        if (filterDTO.getFechaRegistroStart() != null || filterDTO.getFechaRegistroEnd() != null) {
            filtrosAplicados.append("- Fecha de Registro: ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            if (filterDTO.getFechaRegistroStart() != null) filtrosAplicados.append("Desde ").append(filterDTO.getFechaRegistroStart().format(formatter)).append(" ");
            if (filterDTO.getFechaRegistroEnd() != null) filtrosAplicados.append("Hasta ").append(filterDTO.getFechaRegistroEnd().format(formatter));
            filtrosAplicados.append("\n");
        }

        if (filtrosAplicados.toString().equals("Filtros Aplicados:\n")) {
            filtrosAplicados.append("- Ninguno (Reporte Completo)\n");
        }

        Paragraph pFiltros = new Paragraph(filtrosAplicados.toString(), fontFilters);
        pFiltros.setAlignment(Paragraph.ALIGN_LEFT);
        pFiltros.setSpacingAfter(10);
        document.add(pFiltros);
        // --- Fin Mostrar filtros aplicados ---

        // Crear la tabla PDF (ajustar columnas según tu modelo Cliente)
        PdfPTable table = new PdfPTable(8); // Ejemplo: ID, Tipo, DNI/RUC, Nombre/Razón Social, Dirección, Teléfono, Estado, F. Registro
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Ajusta los anchos de columna según tus datos de cliente
        float[] columnWidths = {0.5f, 1f, 1f, 2f, 2f, 0.8f, 0.7f, 1.2f};
        table.setWidths(columnWidths);

        PdfPCell cell;
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.BLACK);
        Font fontContentActive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(0, 128, 0)); // Verde
        Font fontContentInactive = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new BaseColor(255, 0, 0)); // Rojo

        String[] headers = {"ID", "Tipo", "DNI/RUC", "Nombre/Razón Social", "Dirección", "Teléfono", "Estado", "F. Registro"};
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(24, 61, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        if (clientes.isEmpty()) {
            cell = new PdfPCell(new Phrase("No se encontraron clientes con los filtros aplicados.", fontContent));
            cell.setColspan(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Cliente cliente : clientes) {
                table.addCell(new Phrase(String.valueOf(cliente.getIdCliente()), fontContent));
                table.addCell(new Phrase(cliente.getTipoCliente() != null ? cliente.getTipoCliente().getRolCliente() : "N/A", fontContent));
                table.addCell(new Phrase(cliente.getDni() != null ? cliente.getDni() : (cliente.getRuc() != null ? cliente.getRuc() : "N/A"), fontContent));
                String nombreDisplay = "N/A";
                if (cliente.getTipoCliente() != null) {
                    if (cliente.getTipoCliente().getIdRolCliente() == 1) { // Natural
                        nombreDisplay = String.format("%s %s %s",
                                cliente.getNombre() != null ? cliente.getNombre() : "",
                                cliente.getApPaterno() != null ? cliente.getApPaterno() : "",
                                cliente.getApMaterno() != null ? cliente.getApMaterno() : "").trim();
                    } else if (cliente.getTipoCliente().getIdRolCliente() == 2) { // Jurídica
                        nombreDisplay = cliente.getRazonSocial() != null ? cliente.getRazonSocial() : "N/A";
                    }
                }
                table.addCell(new Phrase(nombreDisplay, fontContent));
                table.addCell(new Phrase(cliente.getDireccion() != null ? cliente.getDireccion() : "N/A", fontContent));
                table.addCell(new Phrase(cliente.getTelefono() != null ? cliente.getTelefono() : "N/A", fontContent));

                String estadoText;
                Font estadoFont;
                if (cliente.getEstado() == 1) {
                    estadoText = "Activo";
                    estadoFont = fontContentActive;
                } else if (cliente.getEstado() == 0) {
                    estadoText = "Inactivo";
                    estadoFont = fontContentInactive;
                } else {
                    estadoText = "Desconocido";
                    estadoFont = fontContent;
                }
                table.addCell(new Phrase(estadoText, estadoFont));
                table.addCell(new Phrase(cliente.getFechaRegistro() != null ? cliente.getFechaRegistro().format(formatter) : "N/A", fontContent));
            }
        }

        document.add(table);
        document.close();
    }
}
