package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Repository.ProductoRepository;
import com.example.DyD_Natures.Repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;



    /**
     * Lista todos los productos que no tienen el estado '2' (eliminado lógicamente).
     * Este método se usa para la carga inicial de datos en el frontend
     * y para recargar la lista completa después de operaciones CRUD.
     * @return Lista de productos activos/inactivos (no eliminados).
     */
    public List<Producto> listarProductosActivos() {
        // CAMBIO CLAVE AQUÍ: Llamar al nuevo método con @Query y pasar un Byte
        return productoRepository.findByEstadoExcluding((byte) 2);
    }

    /**
     * Obtiene un producto por su ID.
     * @param id El ID del producto.
     * @return Un Optional que contiene el Producto si se encuentra, o vacío si no.
     */
    public Optional<Producto> obtenerProductoPorId(Integer id) {
        return productoRepository.findById(id);
    }


    /**
     * Guarda un producto nuevo o actualiza uno existente.
     * Si es un producto nuevo, establece el estado inicial a '1' (Activo) y la fecha de registro.
     * @param producto El objeto Producto a guardar.
     * @return El Producto guardado.
     */
    public Producto guardarProducto(Producto producto) {
        if (producto.getIdProducto() == null) {
            producto.setEstado((byte) 1); // Nuevo producto por defecto es Activo (Byte)
            producto.setFechaRegistro(LocalDate.now()); // Establecer fecha de registro si es nuevo
        }
        return productoRepository.save(producto);
    }

    /**
     * Realiza una eliminación lógica de un producto, cambiando su estado a '2'.
     * @param id El ID del producto a eliminar lógicamente.
     */
    public void eliminarProducto(Integer id) {
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setEstado((byte) 2); // Cambiar estado a 2 = eliminado lógicamente (Byte)
            productoRepository.save(producto);
        }
    }



    /**
     * Lista todas las categorías.
     * @return Lista de categorías.
     */
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    /**
     * Obtiene una categoría por su ID.
     * @param id El ID de la categoría.
     * @return Un Optional que contiene la Categoría si se encuentra, o vacío si no.
     */
    public Optional<Categoria> obtenerCategoriaPorId(Integer id) {
        return categoriaRepository.findById(id);
    }
}

