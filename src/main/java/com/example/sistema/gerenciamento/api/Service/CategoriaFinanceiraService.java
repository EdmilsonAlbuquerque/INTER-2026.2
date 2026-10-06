package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Entity.*;
import com.example.sistema.gerenciamento.api.Repository.*;
import com.example.sistema.gerenciamento.api.Exception.*;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class CategoriaFinanceiraService {
    private final CategoriaFinanceiraRepository categorias;
    private final FinanceiroRepository lancamentos;
    private final InicializacaoFinanceiraRepository inicializacao;
    public record Dados(@NotBlank @Size(max=100) String nome) {}
    private String nome(String raw) { return raw.trim().replaceAll("\\s+", " "); }
    private String chave(String raw) { return nome(raw).toLowerCase(Locale.ROOT); }

    @Transactional(readOnly=true)
    public List<CategoriaFinanceira> listar() { return categorias.findAllByOrderByNomeAsc(); }

    @Transactional
    public String garantir(String raw) {
        return categorias.findByChave(chave(raw)).orElseGet(() -> {
            var categoria = new CategoriaFinanceira();
            categoria.setNome(nome(raw)); categoria.setChave(chave(raw));
            return categorias.save(categoria);
        }).getNome();
    }

    @Transactional
    public CategoriaFinanceira salvar(Long id, Dados dados) {
        String nome = nome(dados.nome()), chave = chave(nome);
        var existente = categorias.findByChave(chave);
        if (existente.isPresent() && !Objects.equals(existente.get().getId(), id))
            throw new BusinessException("Já existe uma categoria com esse nome.");
        var categoria = id == null ? new CategoriaFinanceira() : categorias.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
        if (id != null) {
            for (var item : lancamentos.findAll()) {
                if (chave(item.getCategoria()).equals(categoria.getChave())) item.setCategoria(nome);
            }
        }
        categoria.setNome(nome); categoria.setChave(chave);
        return categorias.save(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        var categoria = categorias.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
        if (lancamentos.findAll().stream().anyMatch(item -> chave(item.getCategoria()).equals(categoria.getChave())))
            throw new BusinessException("Esta categoria possui lançamentos. Edite os lançamentos para outra categoria antes de removê-la.");
        categorias.delete(categoria);
    }

    @Transactional
    public void inicializar() {
        if (!inicializacao.existsById("categorias-v1")) {
            for (String nome : List.of("Cestas e Alimentos", "Infraestrutura Social", "Tecnologia & Operações", "Fundo de Reserva", "Doações")) garantir(nome);
            inicializacao.save(new InicializacaoFinanceira("categorias-v1"));
        }
        for (var item : lancamentos.findAll()) item.setCategoria(garantir(item.getCategoria()));
    }
}
