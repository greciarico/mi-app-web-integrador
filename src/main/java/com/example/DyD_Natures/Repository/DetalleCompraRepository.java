package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.DetalleCompra;
import com.example.DyD_Natures.Model.DocumentoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {
    // Buscar detalles de compra por documento de compra
    List<DetalleCompra> findByDocumentoCompra(DocumentoCompra documentoCompra);

    // Eliminar todos los detalles de compra asociados a un documento de compra
    void deleteByDocumentoCompra(DocumentoCompra documentoCompra);
}
