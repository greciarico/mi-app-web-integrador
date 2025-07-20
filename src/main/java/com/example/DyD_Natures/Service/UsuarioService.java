package com.example.DyD_Natures.Service;
import com.example.DyD_Natures.Dto.UsuarioFilterDTO;
import com.example.DyD_Natures.Model.RolUsuario; // Necesario para el join
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join; // Importar Join
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Listar todos
    public List<Usuario> listarUsuariosActivos() {
        return usuarioRepository.findByEstadoNot((byte) 2);
    }

    // Buscar por ID
    public Optional<Usuario> obtenerUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    // Guardar o actualizar usuario
    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    // Eliminar usuario
    public void eliminarUsuario(Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setEstado((byte) 2);  // Cambiar estado a 2 = eliminado
            usuarioRepository.save(usuario);
        }
    }
    public Optional<Usuario> obtenerUsuarioPorDni(String dni) {
        // Sólo traerá usuarios cuyo estado ≠ 2
        return usuarioRepository.findByDniAndEstadoNot(dni, (byte)2);
    }

    /**
     * Verifica si un DNI ya existe (ignorando registros con estado = 2).
     * @param dni El DNI a verificar.
     * @param idUsuario Si no es null, excluye ese mismo ID (para edición).
     */
    public boolean existsByDniExcludingId(String dni, Integer idUsuario) {
        Byte ELIMINADO = (byte) 2;
        if (idUsuario != null) {
            // Edición: ignora al propio usuario y a los eliminados
            return usuarioRepository
                    .existsByDniAndIdUsuarioIsNotAndEstadoNot(dni, idUsuario, ELIMINADO);
        } else {
            // Creación: ignora sólo los eliminados
            return usuarioRepository
                    .existsByDniAndEstadoNot(dni, ELIMINADO);
        }
    }

    /**
     * Busca usuarios aplicando filtros dinámicamente.
     * @param filterDTO DTO con los criterios de búsqueda.
     * @return Lista de usuarios que coinciden con los filtros.
     */
    public List<Usuario> buscarUsuariosPorFiltros(UsuarioFilterDTO filterDTO) {
        return usuarioRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por nombre, apellido, DNI, correo (búsqueda general)
            if (filterDTO.getNombreApellidoDniCorreo() != null && !filterDTO.getNombreApellidoDniCorreo().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreApellidoDniCorreo().toLowerCase() + "%";
                Predicate generalSearchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apPaterno")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apMaterno")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("dni")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("correo")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("telefono")), searchTerm) // Añadir búsqueda por teléfono
                );
                predicates.add(generalSearchPredicate);
            }

            // Filtro por Rol (tipo de usuario) - Ahora maneja múltiples selecciones
            if (filterDTO.getIdRoles() != null && !filterDTO.getIdRoles().isEmpty()) {
                Join<Usuario, RolUsuario> rolJoin = root.join("rolUsuario"); // Unir con la entidad RolUsuario
                predicates.add(rolJoin.get("idRol").in(filterDTO.getIdRoles()));
            }

            // Filtro por Estado - Ahora maneja múltiples selecciones
            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                // Si ambos (0 y 1) están seleccionados, no necesitamos filtrar por estado.
                // Esto asume que 0 y 1 son los únicos estados 'visibles' además de 2 (eliminado).
                if (!(filterDTO.getEstados().contains(0) && filterDTO.getEstados().contains(1))) {
                    // Convertir List<Integer> a List<Byte> para la comparación con el campo 'estado'
                    List<Byte> estadosBytes = new ArrayList<>();
                    filterDTO.getEstados().forEach(estadoInt -> estadosBytes.add(estadoInt.byteValue()));
                    predicates.add(root.get("estado").in(estadosBytes));
                }
            }

            // Filtro por rango de Fecha de Registro
            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }


            // Excluir usuarios con estado = 2 (eliminado) por defecto en los reportes
            predicates.add(criteriaBuilder.notEqual(root.get("estado"), (byte) 2));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
    public Optional<Usuario> findByNombre(String nombre) { // <--- ¡Cambiado el nombre del método en el servicio!
        return usuarioRepository.findByNombre(nombre); // <--- ¡Cambiado el nombre del método llamado en el repositorio!
    }
}


