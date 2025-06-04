package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    // Listar productos que no estén "eliminados" (estado distinto de 2)
    public List<Producto> listarProductos() {
        return productoRepository.findByEstadoNot((byte) 2);
    }

    // Buscar producto por ID
    public Optional<Producto> obtenerProductoPorId(Integer id) {
        return productoRepository.findById(id);
    }

    // Guardar o actualizar producto
    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    // Eliminar producto (cambiar estado a 2)
    public void eliminarProducto(Integer id) {
        productoRepository.findById(id).ifPresent(producto -> {
            producto.setEstado((byte) 2);
            productoRepository.save(producto);
        });
    }
}

