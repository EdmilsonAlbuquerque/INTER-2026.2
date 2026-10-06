package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Entity.*;
import com.example.sistema.gerenciamento.api.Repository.*;
import com.example.sistema.gerenciamento.api.Exception.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class FinanceiroService {
    private final FinanceiroRepository repository;
    private final InicializacaoFinanceiraRepository inicializacao;
    private final CategoriaFinanceiraService categorias;
    public record Dados(@NotNull LancamentoFinanceiro.Tipo tipo, @NotBlank @Size(max=200) String descricao,
            @NotBlank @Size(max=100) String categoria, @NotNull LocalDate data,
            @NotNull @DecimalMin("0.01") @Digits(integer=12, fraction=2) BigDecimal valor) {}

    @Transactional(readOnly=true)
    public List<LancamentoFinanceiro> listar(Integer ano, String tipo, String busca) {
        if (ano != null && (ano < 1900 || ano > 9999)) throw new BusinessException("Informe um ano válido.");
        LancamentoFinanceiro.Tipo filtro = tipo == null || tipo.isBlank() ? null : LancamentoFinanceiro.Tipo.valueOf(tipo);
        String texto = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(l -> ano == null || l.getData().getYear() == ano)
                .filter(l -> filtro == null || l.getTipo() == filtro)
                .filter(l -> (l.getDescricao() + " " + l.getCategoria()).toLowerCase(Locale.ROOT).contains(texto))
                .sorted(Comparator.comparing(LancamentoFinanceiro::getData).reversed().thenComparing(LancamentoFinanceiro::getId))
                .toList();
    }

    @Transactional
    public LancamentoFinanceiro salvar(Long id, Dados dados) {
        var item = id == null ? new LancamentoFinanceiro() : repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado."));
        item.setTipo(dados.tipo()); item.setDescricao(dados.descricao().trim()); item.setCategoria(categorias.garantir(dados.categoria()));
        item.setData(dados.data()); item.setValor(dados.valor());
        return repository.save(item);
    }

    @Transactional public void excluir(Long id) {
        repository.delete(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado.")));
    }
    @Transactional public void removerExemplos() { repository.deleteByExemploTrue(); }

    @Transactional
    public void gerarExemplos() {
        if (inicializacao.existsById("exemplos-v1")) return;
        if (repository.count() == 0) {
            int ano = LocalDate.now().getYear();
            for (int mes = 1; mes <= LocalDate.now().getMonthValue(); mes++) {
                var receita = salvar(null, new Dados(LancamentoFinanceiro.Tipo.RECEITA, "Doação de demonstração " + mes,
                        "Doações", LocalDate.of(ano, mes, 1), new BigDecimal(3000 + mes * 200)));
                receita.setExemplo(true);
                var despesa = salvar(null, new Dados(LancamentoFinanceiro.Tipo.DESPESA, "Compra de demonstração " + mes,
                        mes % 2 == 0 ? "Alimentação" : "Infraestrutura", LocalDate.of(ano, mes, 1), new BigDecimal(1200 + mes * 90)));
                despesa.setExemplo(true);
            }
        }
        inicializacao.save(new InicializacaoFinanceira("exemplos-v1"));
    }
}
