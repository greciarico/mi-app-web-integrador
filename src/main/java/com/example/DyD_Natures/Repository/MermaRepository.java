package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Merma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MermaRepository extends JpaRepository<Merma, Long> {
}

