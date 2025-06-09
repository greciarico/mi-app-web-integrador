package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Venta;
import com.example.DyD_Natures.Model.Cliente;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Model.Igv;
import com.example.DyD_Natures.Service.VentaService;
import com.example.DyD_Natures.Service.ClienteService;
import com.example.DyD_Natures.Service.ProductoService;
import com.example.DyD_Natures.Service.IgvService;
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
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private IgvService igvService;

    /**
     * Muestra la vista principal de Ventas.
     * @param model El modelo para pasar datos a la vista.
     * @return El nombre de la vista (venta.html).
     */
    @GetMapping
    public String listarVentas(Model model) {
        try {
            model.addAttribute("ventas", ventaService.listarVentas());
            return "venta";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar la página de Ventas: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Endpoint para obtener todas las ventas en formato JSON.
     * Usado por el JavaScript para recargar la lista después de operaciones CRUD.
     * @return Una lista de ventas.
     */
    @GetMapping("/all")
    @ResponseBody
    public List<Venta> getAllVentasJson() {
        return ventaService.listarVentas();
    }

    /**
     * Muestra el formulario para crear una nueva venta.
     * @param model El modelo.
     * @return Fragmento del formulario.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Venta venta = new Venta();
        venta.setCliente(new Cliente());
        venta.setIgvEntity(new Igv());
        venta.setDetalleVentas(new ArrayList<>());
        model.addAttribute("venta", venta);
        model.addAttribute("clientes", clienteService.listarTodosLosClientesActivos());
        model.addAttribute("productos", productoService.listarProductosActivos());
        model.addAttribute("igvs", igvService.listarIgvActivos());
        return "fragments/venta_form_modal :: formContent";
    }

    /**
     * Muestra el formulario para editar una venta existente.
     * @param id El ID de la venta.
     * @param model El modelo.
     * @return Fragmento del formulario o error.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Venta> ventaOpt = ventaService.obtenerVentaPorId(id);
        Venta venta;

        if (ventaOpt.isPresent()) {
            venta = ventaOpt.get();
            if (venta.getCliente() == null) venta.setCliente(new Cliente());
            if (venta.getIgvEntity() == null) venta.setIgvEntity(new Igv());
            if (venta.getDetalleVentas() == null) venta.setDetalleVentas(new ArrayList<>());
        } else {
            venta = new Venta();
            venta.setCliente(new Cliente());
            venta.setIgvEntity(new Igv());
            venta.setDetalleVentas(new ArrayList<>());
            model.addAttribute("mensajeError", "Venta no encontrada para editar.");
        }

        model.addAttribute("venta", venta);
        model.addAttribute("clientes", clienteService.listarTodosLosClientesActivos());
        model.addAttribute("productos", productoService.listarProductosActivos());
        model.addAttribute("igvs", igvService.listarIgvActivos());
        return "fragments/venta_form_modal :: formContent";
    }

    /**
     * Muestra el modal de visualización para una venta existente.
     * @param id El ID de la venta.
     * @param model El modelo.
     * @return Fragmento de la vista de la venta o error.
     */
    @GetMapping("/visualizar/{id}")
    public String mostrarVenta(@PathVariable Integer id, Model model) {
        Optional<Venta> ventaOpt = ventaService.obtenerVentaPorId(id);
        if (ventaOpt.isPresent()) {
            model.addAttribute("venta", ventaOpt.get());
            return "fragments/venta_view_modal :: viewContent";
        } else {
            model.addAttribute("mensajeError", "Venta no encontrada.");
            return "error";
        }
    }

    /**
     * Guarda una Venta y sus DetalleVenta, actualizando el stock.
     * @param venta El objeto Venta.
     * @return ResponseEntity con el resultado.
     */
    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarVenta(@RequestBody Venta venta) {
        Map<String, String> response = new HashMap<>();
        try {
            ventaService.guardarVenta(venta);
            response.put("status", "success");
            response.put("message", "Venta guardada exitosamente y stock de productos actualizado!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) { // Captura excepciones de negocio como stock insuficiente
            response.put("status", "error");
            response.put("message", "Error de negocio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error interno al guardar la Venta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
