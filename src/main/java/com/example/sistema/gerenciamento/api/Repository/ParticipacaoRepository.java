package com.example.sistema.gerenciamento.api.Repository;

import com.example.sistema.gerenciamento.api.Entity.Participacao;
import com.example.sistema.gerenciamento.api.Entity.StatusAtividade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Consultas para vinculos entre pessoas e servicos. */
@Repository
public interface ParticipacaoRepository extends JpaRepository<Participacao, Long> {

	/** Retorna todas as participacoes de uma pessoa. */
	List<Participacao> findByPessoaId(Long pessoaId);

	/** Retorna todas as pessoas vinculadas a um servico. */
	List<Participacao> findByServicoId(Long servicoId);

	/** Filtra participacoes pelo status atual. */
	List<Participacao> findByStatus(StatusAtividade status);

	/** Evita uma nova inscricao ativa para o mesmo servico e pessoa. */
	boolean existsByPessoaIdAndServicoIdAndStatus(Long pessoaId, Long servicoId, StatusAtividade status);
}
