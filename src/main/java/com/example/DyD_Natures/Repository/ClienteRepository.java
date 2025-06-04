package com.example.DyD_Natures.Repository;
import com.example.DyD_Natures.Model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    // Puedes agregar consultas específicas aquí si lo necesitas
}


