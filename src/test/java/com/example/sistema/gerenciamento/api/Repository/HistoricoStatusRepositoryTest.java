package com.example.sistema.gerenciamento.api.Repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sistema.gerenciamento.api.Entity.HistoricoStatus;
import com.example.sistema.gerenciamento.api.Entity.Pessoa;
import com.example.sistema.gerenciamento.api.Entity.Participacao;
import com.example.sistema.gerenciamento.api.Entity.Responsavel;
import com.example.sistema.gerenciamento.api.Entity.Servico;
import com.example.sistema.gerenciamento.api.Entity.StatusAtividade;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Testes da consulta ordenada do historico de status. */
@DataJpaTest
class HistoricoStatusRepositoryTest {

    /** Repositorio sob teste. */
    @Autowired
    private HistoricoStatusRepository repository;

    /** Repositorios auxiliares para montar a participacao relacionada. */
    @Autowired
    private ParticipacaoRepository participacaoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    /** Confirma que o evento mais recente aparece primeiro. */
    @Test
    void deveOrdenarHistoricoPorDataDecrescente() {
        // Arrange: cria uma participacao e dois eventos em momentos diferentes.
        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                null, "Pessoa Historico", "MAT-HIST", "historico@exemplo.com", "11111111111",
                "85999999999", null, null, null, null));
        Responsavel responsavel = responsavelRepository.save(new Responsavel(
                null, "Gestor Historico", "gestor@exemplo.com", null));
        Servico servico = servicoRepository.save(new Servico(
                null, "Servico Historico", "Social", "Descricao", 30, true, responsavel));
        Participacao participacao = participacaoRepository.save(new Participacao(
                null, LocalDateTime.now().plusDays(1), StatusAtividade.PENDENTE, pessoa, servico));
        LocalDateTime maisAntiga = LocalDateTime.now().minusMinutes(10);
        LocalDateTime maisRecente = LocalDateTime.now();
        repository.save(HistoricoStatus.builder().dataAlteracao(maisAntiga)
                .status(StatusAtividade.PENDENTE).observacao("Criado").participacao(participacao).build());
        repository.save(HistoricoStatus.builder().dataAlteracao(maisRecente)
                .status(StatusAtividade.ATIVO).observacao("Ativado").participacao(participacao).build());

        // Act: busca o historico pela participacao.
        var historico = repository.findByParticipacaoIdOrderByDataAlteracaoDesc(participacao.getId());

        // Assert: o evento mais recente ocupa a primeira posicao.
        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).getStatus()).isEqualTo(StatusAtividade.ATIVO);
        assertThat(historico.get(1).getStatus()).isEqualTo(StatusAtividade.PENDENTE);
    }
}
