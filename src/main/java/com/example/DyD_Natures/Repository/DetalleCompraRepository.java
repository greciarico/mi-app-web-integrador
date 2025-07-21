package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.DetalleCompra;
import com.example.DyD_Natures.Model.DocumentoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {
    List<DetalleCompra> findByDocumentoCompra(DocumentoCompra documentoCompra);

}
