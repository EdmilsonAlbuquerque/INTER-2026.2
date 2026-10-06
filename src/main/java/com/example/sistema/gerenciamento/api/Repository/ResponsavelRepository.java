package com.example.sistema.gerenciamento.api.Repository;

import com.example.sistema.gerenciamento.api.Entity.Responsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio basico para gestores responsaveis pelos servicos. */
@Repository
public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {
}
