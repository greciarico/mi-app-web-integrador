package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoClienteRepository extends JpaRepository<TipoCliente, Integer> {
}
