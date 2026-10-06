package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Entity.LancamentoFinanceiro;
import com.example.sistema.gerenciamento.api.Service.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/financeiro")
public class FinanceiroController {
    private final FinanceiroService service;
    private final RelatorioService relatorios;
    @GetMapping public Map<String,Object> listar(@RequestParam(required=false) Integer ano,
            @RequestParam(required=false) String tipo, @RequestParam(required=false) String busca) {
        var itens=service.listar(ano,tipo,busca);
        var receitas=total(itens,LancamentoFinanceiro.Tipo.RECEITA);var despesas=total(itens,LancamentoFinanceiro.Tipo.DESPESA);
        return Map.of("lancamentos",itens,"receitas",receitas,"despesas",despesas,"saldo",receitas.subtract(despesas));
    }
    private BigDecimal total(List<LancamentoFinanceiro> itens,LancamentoFinanceiro.Tipo tipo) {
        return itens.stream().filter(l->l.getTipo()==tipo).map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public LancamentoFinanceiro criar(@Valid @RequestBody FinanceiroService.Dados dto) { return service.salvar(null,dto); }
    @PutMapping("/{id}") public LancamentoFinanceiro editar(@PathVariable Long id,@Valid @RequestBody FinanceiroService.Dados dto) {return service.salvar(id,dto);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluir(@PathVariable Long id){service.excluir(id);}
    @DeleteMapping("/exemplos") @ResponseStatus(HttpStatus.NO_CONTENT) public void limpar(){service.removerExemplos();}
    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> exportar(@RequestParam(defaultValue="pdf") String formato,
            @RequestParam(required=false) Integer ano,@RequestParam(required=false) String tipo,@RequestParam(required=false) String busca,
            @RequestParam(required=false) String buscaHistorico) throws IOException {
        var itens=service.listar(ano,tipo,busca);
        if (buscaHistorico != null && !buscaHistorico.isBlank()) {
            String texto = buscaHistorico.trim().toLowerCase(Locale.ROOT);
            itens = itens.stream().filter(l -> (l.getDescricao() + " " + l.getCategoria()).toLowerCase(Locale.ROOT).contains(texto)).toList();
        }
        var rows=itens.stream().map(l->List.of(l.getData().toString(),l.getDescricao(),l.getTipo().name(),l.getCategoria(),
                l.getValor().toPlainString(),l.isExemplo()?"Demonstração":"Real")).toList();
        String contexto="Ano: "+(ano==null?"Todos":ano)+" | Receitas: R$ "+total(itens,LancamentoFinanceiro.Tipo.RECEITA)+" | Despesas: R$ "+total(itens,LancamentoFinanceiro.Tipo.DESPESA);
        return RelatorioController.download(relatorios.gerar(formato,"Relatório financeiro",contexto,
                List.of("Data","Descrição","Tipo","Categoria","Valor (R$)","Origem"),rows),formato,"financeiro");
    }
}
