package com.example.sistema.gerenciamento.api.Repository;

import com.example.sistema.gerenciamento.api.Entity.TokenAcesso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface TokenAcessoRepository extends JpaRepository<TokenAcesso, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenAcesso t where t.hash = :hash")
    Optional<TokenAcesso> buscarParaUso(String hash);
    void deleteByInicialTrue();
    void deleteByEmailAndTipo(String email, String tipo);
}
