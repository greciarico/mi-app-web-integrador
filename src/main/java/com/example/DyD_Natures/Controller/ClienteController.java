package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Cliente;
import com.example.DyD_Natures.Model.TipoCliente;
import com.example.DyD_Natures.Service.ClienteService;
import com.example.DyD_Natures.Service.TipoClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        boolean exists = clienteService.existsByRucExcludingCurrent(ruc, idCliente);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}
