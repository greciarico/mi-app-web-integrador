package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.ProductoFilterDTO;
import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Repository.ProductoRepository;
import com.example.DyD_Natures.Repository.CategoriaRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private static final byte ESTADO_ACTIVO    = 1;
    private static final byte ESTADO_ELIMINADO = 2;

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
        return productoRepository.findByEstadoExcluding(ESTADO_ELIMINADO);
    }

    public List<Producto> listarSoloProductosActivos() {
        return productoRepository.findByEstado((byte) 1);
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
    @Transactional
    public Producto guardarProducto(Producto producto) {
        // Si es creación
        if (producto.getIdProducto() == null) {
            if (productoRepository.existsByNombreAndIdProductoIsNotAndEstadoNot(
                    producto.getNombre(), 0, ESTADO_ELIMINADO)) {
                throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
            }
            producto.setFechaRegistro(LocalDate.now());
            producto.setEstado(ESTADO_ACTIVO);
        }
        else {
            if (productoRepository.existsByNombreAndIdProductoIsNotAndEstadoNot(
                    producto.getNombre(), producto.getIdProducto(), ESTADO_ELIMINADO)) {
                throw new IllegalArgumentException("Ya existe otro producto con ese nombre.");
            }
            // conservar fecha de registro original
            productoRepository.findById(producto.getIdProducto())
                    .ifPresent(orig -> producto.setFechaRegistro(orig.getFechaRegistro()));
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
     * Lista solo las categorías que están en estado '1' (Activas).
     * @return Lista de categorías activas.
     */
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findByEstado((byte) 1); // <-- CAMBIO AQUÍ
    }

    /**
     * Obtiene una categoría por su ID.
     * @param id El ID de la categoría.
     * @return Un Optional que contiene la Categoría si se encuentra, o vacío si no.
     */
    public Optional<Categoria> obtenerCategoriaPorId(Integer id) {
        return categoriaRepository.findById(id);
    }

    /**
     * Busca productos aplicando filtros dinámicamente para la generación de reportes.
     * @param filterDTO DTO con los criterios de búsqueda.
     * @return Lista de productos que coinciden con los filtros.
     */
    public List<Producto> buscarProductosPorFiltros(ProductoFilterDTO filterDTO) {
        return productoRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por nombre
            if (filterDTO.getNombre() != null && !filterDTO.getNombre().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombre().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), searchTerm));
            }

            // Filtro por descripción
            if (filterDTO.getDescripcion() != null && !filterDTO.getDescripcion().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getDescripcion().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), searchTerm));
            }

            // Filtro por categoría
            if (filterDTO.getIdCategoria() != null) {
                Join<Producto, Categoria> categoriaJoin = root.join("categoria");
                predicates.add(criteriaBuilder.equal(categoriaJoin.get("idCategoria"), filterDTO.getIdCategoria()));
            }

            // Filtro por rango de Precio 1
            if (filterDTO.getPrecio1Min() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("precio1"), filterDTO.getPrecio1Min()));
            }
            if (filterDTO.getPrecio1Max() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("precio1"), filterDTO.getPrecio1Max()));
            }

            // Filtro por rango de Precio 2
            if (filterDTO.getPrecio2Min() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("precio2"), filterDTO.getPrecio2Min()));
            }
            if (filterDTO.getPrecio2Max() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("precio2"), filterDTO.getPrecio2Max()));
            }

            // Filtro por rango de Stock
            if (filterDTO.getStockMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("stock"), filterDTO.getStockMin()));
            }
            if (filterDTO.getStockMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("stock"), filterDTO.getStockMax()));
            }

            // Filtro por Estado
            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                // Si eligen ambos (activo y inactivo), no se añade este filtro (ya que se listan todos excepto eliminados)
                if (!(filterDTO.getEstados().contains((byte) 0) && filterDTO.getEstados().contains((byte) 1))) {
                    predicates.add(root.get("estado").in(filterDTO.getEstados()));
                }
            }

            // Filtro por rango de Fecha de Registro
            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }

            // Excluir productos con estado = 2 (eliminado) por defecto en los reportes
            predicates.add(criteriaBuilder.notEqual(root.get("estado"), (byte) 2));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
