package com.example.sistema.gerenciamento.api.Repository;

import com.example.sistema.gerenciamento.api.Entity.CategoriaFinanceira;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, Long> {
    Optional<CategoriaFinanceira> findByChave(String chave);
    List<CategoriaFinanceira> findAllByOrderByNomeAsc();
}
