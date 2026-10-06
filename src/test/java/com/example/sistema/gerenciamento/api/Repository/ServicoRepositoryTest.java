package com.example.sistema.gerenciamento.api.Repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sistema.gerenciamento.api.Entity.Responsavel;
import com.example.sistema.gerenciamento.api.Entity.Servico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Testes das consultas de servicos ativos e por categoria. */
@DataJpaTest
class ServicoRepositoryTest {

    /** Repositorio de servicos configurado pelo contexto de teste. */
    @Autowired
    private ServicoRepository repository;

    /** Repositorio necessario para satisfazer a chave estrangeira de Servico. */
    @Autowired
    private ResponsavelRepository responsavelRepository;

    /** Verifica filtros de atividade e categoria. */
    @Test
    void deveFiltrarServicosAtivosEPorCategoria() {
        // Arrange: persiste o responsavel obrigatorio e servicos com estados distintos.
        Responsavel responsavel = responsavelRepository.save(new Responsavel(
                null, "Carlos Gestor", "carlos@exemplo.com", "11999999999"));
        repository.save(new Servico(null, "Oficina", "Educacao", "Oficina semanal", 60, true, responsavel));
        repository.save(new Servico(null, "Atendimento", "Educacao", "Atendimento individual", 30, false, responsavel));
        repository.save(new Servico(null, "Esporte", "Saude", "Atividade fisica", 45, true, responsavel));

        // Act: executa as consultas derivadas solicitadas.
        var ativos = repository.findByAtivoTrue();
        var educacao = repository.findByCategoriaIgnoreCase("educacao");

        // Assert: somente os registros que atendem aos filtros sao retornados.
        assertThat(ativos).extracting(Servico::getNome).containsExactlyInAnyOrder("Oficina", "Esporte");
        assertThat(educacao).extracting(Servico::getNome).containsExactlyInAnyOrder("Oficina", "Atendimento");
    }
}
