package com.example.DyD_Natures.Service;
import com.example.DyD_Natures.Dto.UsuarioFilterDTO;
import com.example.DyD_Natures.Model.RolUsuario; 
import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join; 
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;


    public List<Usuario> listarUsuariosActivos() {
        return usuarioRepository.findByEstadoNot((byte) 2);
    }


    public Optional<Usuario> obtenerUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id);
    }


    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }


    public void eliminarUsuario(Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setEstado((byte) 2);  
            usuarioRepository.save(usuario);
        }
    }
    public Optional<Usuario> obtenerUsuarioPorDni(String dni) {
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
            return usuarioRepository
                    .existsByDniAndIdUsuarioIsNotAndEstadoNot(dni, idUsuario, ELIMINADO);
        } else {
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

            if (filterDTO.getNombreApellidoDniCorreo() != null && !filterDTO.getNombreApellidoDniCorreo().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreApellidoDniCorreo().toLowerCase() + "%";
                Predicate generalSearchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apPaterno")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apMaterno")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("dni")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("correo")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("telefono")), searchTerm) 
                );
                predicates.add(generalSearchPredicate);
            }

            if (filterDTO.getIdRoles() != null && !filterDTO.getIdRoles().isEmpty()) {
                Join<Usuario, RolUsuario> rolJoin = root.join("rolUsuario"); 
                predicates.add(rolJoin.get("idRol").in(filterDTO.getIdRoles()));
            }


            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                if (!(filterDTO.getEstados().contains(0) && filterDTO.getEstados().contains(1))) {
                    List<Byte> estadosBytes = new ArrayList<>();
                    filterDTO.getEstados().forEach(estadoInt -> estadosBytes.add(estadoInt.byteValue()));
                    predicates.add(root.get("estado").in(estadosBytes));
                }
            }

            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }



            predicates.add(criteriaBuilder.notEqual(root.get("estado"), (byte) 2));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
    public Optional<Usuario> findByNombre(String nombre) { 
        return usuarioRepository.findByNombre(nombre); 
    }

    public List<Usuario> obtenerUsuariosPorTipoRol(String tipoRol) {
        return usuarioRepository.findByRolUsuario_TipoRolAndEstadoNot(tipoRol, (byte)2);
    }
}


