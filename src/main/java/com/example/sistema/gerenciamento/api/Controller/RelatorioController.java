package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Service.*;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/relatorios")
public class RelatorioController {
    private final ServicoService servicos;
    private final RelatorioService relatorios;
    @GetMapping("/servicos")
    public ResponseEntity<byte[]> servicos(@RequestParam(defaultValue="pdf") String formato,
            @RequestParam(defaultValue="") String busca, @RequestParam(required=false) Boolean ativo) throws IOException {
        String texto=busca.toLowerCase(Locale.ROOT).trim();
        var rows=servicos.listarTodos().stream().filter(s->ativo==null||s.ativo()==ativo)
                .filter(s->(s.nome()+" "+Objects.toString(s.categoria(),"")).toLowerCase(Locale.ROOT).contains(texto))
                .map(s->List.of(s.nome(),Objects.toString(s.categoria(),""),s.ativo()?"Ativo":"Inativo",
                        Objects.toString(s.duracaoMinutos(),""),Objects.toString(s.responsavelNome(),""),Objects.toString(s.descricao(),""))).toList();
        return download(relatorios.gerar(formato,"Relatório de serviços","Situação: "+(ativo==null?"Todas":ativo?"Ativos":"Inativos"),
                List.of("Serviço","Categoria","Situação","Duração (min)","Responsável","Descrição"),rows),formato,"servicos");
    }
    static ResponseEntity<byte[]> download(byte[] bytes,String formato,String name) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"."+formato+"\"")
                .contentType(MediaType.parseMediaType("pdf".equals(formato)?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
