package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.MermaFilterDTO;
import com.example.DyD_Natures.Model.Merma;
import com.example.DyD_Natures.Model.Producto;
import com.example.DyD_Natures.Repository.MermaRepository;
import com.example.DyD_Natures.Repository.ProductoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MermaService {

    @Autowired
    private MermaRepository mermaRepository;

    @Autowired
    private ProductoRepository productoRepository; // Necesario para interactuar con el stock del producto

    /**
     * Lista todos los registros de Merma.
     * @return Lista de registros de Merma.
     */
    public List<Merma> listarMermas() {
        return mermaRepository.findAll(); // Merma no tiene campo estado, así que listar todos
    }

    /**
     * Obtiene un registro de Merma por su ID.
     * @param id El ID del registro de Merma.
     * @return Un Optional que contiene la Merma si se encuentra, o vacío si no.
     */
    public Optional<Merma> obtenerMermaPorId(Integer id) {
        return mermaRepository.findById(id);
    }

    /**
     * Guarda un registro de Merma nuevo o actualiza uno existente.
     * Implementa la lógica para disminuir el stock del producto.
     * @param merma El objeto Merma a guardar.
     * @throws RuntimeException Si el producto no se encuentra o el stock es insuficiente.
     */
    @Transactional // Asegura que ambas operaciones (Merma y Producto) sean atómicas
    public Merma guardarMerma(Merma merma) {
        Producto producto = merma.getProducto();
        if (producto == null || producto.getIdProducto() == null) {
            throw new RuntimeException("El producto asociado a la merma no puede ser nulo.");
        }

        Optional<Producto> productoOpt = productoRepository.findById(producto.getIdProducto());
        if (productoOpt.isEmpty()) {
            throw new RuntimeException("Producto no encontrado con ID: " + producto.getIdProducto());
        }
        Producto productoExistente = productoOpt.get();

        Integer oldCantidadMerma = 0;
        if (merma.getIdMerma() != null) { // Si es una edición, recupera la cantidad anterior
            Optional<Merma> oldMermaOpt = mermaRepository.findById(merma.getIdMerma());
            if (oldMermaOpt.isPresent()) {
                oldCantidadMerma = oldMermaOpt.get().getCantidad();
            }
        }

        // Calcular la diferencia de cantidad de merma
        // Si es nuevo: diferencia = nueva cantidad
        // Si es edición: diferencia = nueva cantidad - antigua cantidad
        Integer cantidadCambio = merma.getCantidad() - oldCantidadMerma;

        // Actualizar el stock del producto
        Integer nuevoStock = productoExistente.getStock() - cantidadCambio;

        if (nuevoStock < 0) {
            throw new RuntimeException("Stock insuficiente para registrar la merma. Stock actual: " + productoExistente.getStock() + ", cantidad de merma: " + merma.getCantidad());
        }

        productoExistente.setStock(nuevoStock);
        productoRepository.save(productoExistente); // Guarda el producto con el stock actualizado

        if (merma.getIdMerma() == null) { // Si es nuevo, establece la fecha de registro
            merma.setFechaRegistro(LocalDate.now());
        } else { // Si es edición, mantiene la fecha de registro original
            Optional<Merma> existingMermaOpt = mermaRepository.findById(merma.getIdMerma());
            existingMermaOpt.ifPresent(existingMerma -> merma.setFechaRegistro(existingMerma.getFechaRegistro()));
        }

        return mermaRepository.save(merma); // Guarda la merma
    }

    /**
     * Elimina lógicamente un registro de Merma (actualmente elimina físicamente y repone stock).
     * Implementa la lógica para reponer el stock del producto.
     * NOTA: Debido a que Merma no tiene un campo 'estado', esta es una eliminación física.
     * Si deseas eliminación lógica, añade un campo 'estado' a la entidad Merma.
     * @param id El ID del registro de Merma a eliminar.
     * @throws RuntimeException Si la merma o el producto asociado no se encuentran.
     */
    @Transactional
    public void eliminarMerma(Integer id) {
        Optional<Merma> mermaOpt = mermaRepository.findById(id);
        if (mermaOpt.isEmpty()) {
            throw new RuntimeException("Merma no encontrada con ID: " + id);
        }
        Merma merma = mermaOpt.get();

        Optional<Producto> productoOpt = productoRepository.findById(merma.getProducto().getIdProducto());
        if (productoOpt.isEmpty()) {
            throw new RuntimeException("Producto asociado a la merma no encontrado con ID: " + merma.getProducto().getIdProducto());
        }
        Producto productoExistente = productoOpt.get();

        // Reponer el stock del producto
        productoExistente.setStock(productoExistente.getStock() + merma.getCantidad());
        productoRepository.save(productoExistente); // Guarda el producto con el stock repuesto

        mermaRepository.delete(merma); // Elimina el registro de merma
    }
    /**
     * Busca registros de Merma aplicando filtros dinámicamente para la generación de reportes.
     * @param filterDTO DTO con los criterios de búsqueda.
     * @return Lista de registros de Merma que coinciden con los filtros.
     */
    public List<Merma> buscarMermasPorFiltros(MermaFilterDTO filterDTO) {
        return mermaRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Unir con la entidad Producto para filtrar por sus atributos
            Join<Merma, Producto> productoJoin = root.join("producto");

            // Filtro por nombre de producto
            if (filterDTO.getNombreProducto() != null && !filterDTO.getNombreProducto().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreProducto().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(productoJoin.get("nombre")), searchTerm));
            }

            // Filtro por descripción de la merma
            if (filterDTO.getDescripcionMerma() != null && !filterDTO.getDescripcionMerma().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getDescripcionMerma().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), searchTerm));
            }

            // Filtro por un producto específico (por ID)
            if (filterDTO.getIdProducto() != null) {
                predicates.add(criteriaBuilder.equal(productoJoin.get("idProducto"), filterDTO.getIdProducto()));
            }

            // Filtro por rango de fecha de registro
            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }

            // Filtro por rango de cantidad
            if (filterDTO.getCantidadMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("cantidad"), filterDTO.getCantidadMin()));
            }
            if (filterDTO.getCantidadMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("cantidad"), filterDTO.getCantidadMax()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
