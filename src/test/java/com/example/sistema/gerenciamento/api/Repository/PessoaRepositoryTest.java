package com.example.sistema.gerenciamento.api.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.sistema.gerenciamento.api.Entity.Pessoa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/** Testes de persistencia e consultas derivadas de Pessoa. */
@DataJpaTest
class PessoaRepositoryTest {

    /** Repositorio real conectado ao banco de teste. */
    @Autowired
    private PessoaRepository repository;

    /** Valida buscas por CPF, matricula e texto parcial. */
    @Test
    void deveBuscarPessoaPorCpfMatriculaNomeOuCpfParcial() {
        // Arrange: cria duas pessoas para exercitar os dois lados da consulta OR.
        repository.save(Pessoa.builder()
                .nome("Ana Souza")
                .matricula("MAT-001")
                .email("ana@exemplo.com")
                .cpf("11122233344")
                .telefone("85999999991")
                .build());
        repository.save(Pessoa.builder()
                .nome("Bruno Lima")
                .matricula("MAT-002")
                .email("bruno@exemplo.com")
                .cpf("55566677788")
                .telefone("85999999992")
                .build());

        // Act: usa cada consulta publica do repositorio.
        var porCpf = repository.findByCpf("11122233344");
        var porMatricula = repository.findByMatricula("MAT-002");
        var porNome = repository.findByNomeContainingIgnoreCaseOrCpfContaining("ana", "nao-encontrado");
        var porCpfParcial = repository.findByNomeContainingIgnoreCaseOrCpfContaining("nao-encontrado", "777");

        // Assert: confirma que cada filtro retornou a pessoa correta.
        assertThat(porCpf).isPresent();
        assertThat(porCpf.get().getNome()).isEqualTo("Ana Souza");
        assertThat(porMatricula).isPresent();
        assertThat(porMatricula.get().getNome()).isEqualTo("Bruno Lima");
        assertThat(porNome).extracting(Pessoa::getNome).containsExactly("Ana Souza");
        assertThat(porCpfParcial).extracting(Pessoa::getNome).containsExactly("Bruno Lima");
        assertThat(repository.existsByCpf("11122233344")).isTrue();
    }

    /** Confirma que a matricula unica nao permite dois cadastros iguais. */
    @Test
    void deveImpedirMatriculaDuplicada() {
        // Arrange: grava o primeiro cadastro com a matricula reservada.
        repository.saveAndFlush(Pessoa.builder()
                .nome("Primeira Pessoa")
                .matricula("MAT-UNICA")
                .email("primeira@exemplo.com")
                .cpf("11122233344")
                .telefone("85999999991")
                .build());

        // Act e Assert: o segundo cadastro viola a restricao unique.
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(
                Pessoa.builder()
                        .nome("Segunda Pessoa")
                        .matricula("MAT-UNICA")
                        .email("segunda@exemplo.com")
                        .cpf("55566677788")
                        .telefone("85999999992")
                        .build()));
    }
}
