package com.example.sistema.gerenciamento.api.Repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sistema.gerenciamento.api.Entity.Pessoa;
import com.example.sistema.gerenciamento.api.Entity.Participacao;
import com.example.sistema.gerenciamento.api.Entity.Responsavel;
import com.example.sistema.gerenciamento.api.Entity.Servico;
import com.example.sistema.gerenciamento.api.Entity.StatusAtividade;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Testes das consultas de vinculos entre pessoas e servicos. */
@DataJpaTest
class ParticipacaoRepositoryTest {

    /** Repositorio sob teste. */
    @Autowired
    private ParticipacaoRepository repository;

    /** Repositorios auxiliares para criar as chaves estrangeiras. */
    @Autowired
    private PessoaRepository pessoaRepository;

    /** Repositorio auxiliar do servico relacionado. */
    @Autowired
    private ServicoRepository servicoRepository;

    /** Repositorio auxiliar do responsavel do servico. */
    @Autowired
    private ResponsavelRepository responsavelRepository;

    /** Valida filtros por pessoa, servico, status e duplicidade ativa. */
    @Test
    void deveConsultarParticipacoesEIdentificarDuplicidade() {
        // Arrange: cria os agregados necessarios antes dos vinculos.
        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                null, "Pessoa Teste", "MAT-PART", "pessoa@exemplo.com", "99988877766",
                "85999999999", null, null, null, null));
        Responsavel responsavel = responsavelRepository.save(new Responsavel(
                null, "Responsavel Teste", "resp@exemplo.com", null));
        Servico servico = servicoRepository.save(new Servico(
                null, "Servico Teste", "Social", "Descricao", 30, true, responsavel));
        Participacao ativa = repository.save(new Participacao(
                null, LocalDateTime.now().plusDays(1), StatusAtividade.ATIVO, pessoa, servico));
        Participacao pendente = repository.save(new Participacao(
                null, LocalDateTime.now().plusDays(2), StatusAtividade.PENDENTE, pessoa, servico));

        // Act: executa as consultas derivadas do repositorio.
        var porPessoa = repository.findByPessoaId(pessoa.getId());
        var porServico = repository.findByServicoId(servico.getId());
        var porStatus = repository.findByStatus(StatusAtividade.ATIVO);
        boolean duplicadaAtiva = repository.existsByPessoaIdAndServicoIdAndStatus(
                pessoa.getId(), servico.getId(), StatusAtividade.ATIVO);

        // Assert: os filtros encontram os registros esperados.
        assertThat(porPessoa).extracting(Participacao::getId)
                .containsExactlyInAnyOrder(ativa.getId(), pendente.getId());
        assertThat(porServico).hasSize(2);
        assertThat(porStatus).extracting(Participacao::getStatus)
                .containsExactly(StatusAtividade.ATIVO);
        assertThat(duplicadaAtiva).isTrue();
    }
}
