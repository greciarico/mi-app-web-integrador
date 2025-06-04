package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    // Listar todas las categorías activas (suponiendo que estado true es activo)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }
}

