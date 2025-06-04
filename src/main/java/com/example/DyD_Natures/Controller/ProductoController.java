package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Service.CategoriaService;
import com.example.DyD_Natures.Service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    // Listar productos
    @GetMapping
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.listarProductos());
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "fragments/productos :: contenido"; // Ajusta a tu vista Thymeleaf
    }

    // Mostrar formulario para crear nuevo producto
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        Producto producto = new Producto();
        producto.setFechaRegistro(LocalDate.now());
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "fragments/producto_form :: contenido";
    }

    // Guardar producto (nuevo o editado)
    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto, Model model) {
        if (producto.getFechaRegistro() == null) {
            producto.setFechaRegistro(LocalDate.now());
        }
        productoService.guardarProducto(producto);
        model.addAttribute("productos", productoService.listarProductos());
        return "fragments/productos :: contenido";
    }

    // Mostrar formulario para editar producto
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model) {
        Optional<Producto> productoOpt = productoService.obtenerProductoPorId(id);
        if (productoOpt.isPresent()) {
            model.addAttribute("producto", productoOpt.get());
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "fragments/producto_form :: contenido";
        }
        model.addAttribute("productos", productoService.listarProductos());
        return "fragments/productos :: contenido";
    }

    // Eliminar producto
    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Integer id, Model model) {
        productoService.eliminarProducto(id);
        model.addAttribute("productos", productoService.listarProductos());
        return "fragments/productos :: contenido";
    }
}

