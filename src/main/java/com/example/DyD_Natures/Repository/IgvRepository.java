package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Igv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IgvRepository extends JpaRepository<Igv, Long> {
}
