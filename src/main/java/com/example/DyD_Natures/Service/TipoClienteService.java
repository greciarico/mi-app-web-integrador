package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.TipoCliente;
import com.example.DyD_Natures.Repository.TipoClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoClienteService {

    @Autowired
    private TipoClienteRepository tipoClienteRepository;

    /**
     * Lista todos los tipos de cliente.
     * @return Lista de TipoCliente.
     */
    public List<TipoCliente> listarTiposCliente() {
        return tipoClienteRepository.findAll();
    }

    /**
     * Obtiene un tipo de cliente por su ID.
     * @param id El ID del tipo de cliente (idRolCliente).
     * @return Un Optional que contiene el TipoCliente si se encuentra, o vacío si no.
     */
    public Optional<TipoCliente> obtenerTipoClientePorId(Integer id) {
        return tipoClienteRepository.findById(id);
    }

    /**
     * Guarda un nuevo tipo de cliente o actualiza uno existente.
     * @param tipoCliente El objeto TipoCliente a guardar.
     * @return El TipoCliente guardado.
     */
    public TipoCliente guardarTipoCliente(TipoCliente tipoCliente) {
        return tipoClienteRepository.save(tipoCliente);
    }

    /**
     * Elimina un tipo de cliente por su ID.
     * @param id El ID del tipo de cliente a eliminar (idRolCliente).
     */
    public void eliminarTipoCliente(Integer id) {
        tipoClienteRepository.deleteById(id);
    }
}
