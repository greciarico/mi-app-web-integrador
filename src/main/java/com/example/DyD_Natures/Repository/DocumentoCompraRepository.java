package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.DocumentoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoCompraRepository extends JpaRepository<DocumentoCompra, Integer> {
}

