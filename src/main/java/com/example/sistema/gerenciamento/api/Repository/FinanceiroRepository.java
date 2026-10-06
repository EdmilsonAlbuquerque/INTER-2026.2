package com.example.sistema.gerenciamento.api.Repository;
import com.example.sistema.gerenciamento.api.Entity.LancamentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long> {
    void deleteByExemploTrue();
}
