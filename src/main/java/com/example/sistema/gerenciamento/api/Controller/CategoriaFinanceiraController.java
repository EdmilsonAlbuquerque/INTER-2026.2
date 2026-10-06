package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Entity.CategoriaFinanceira;
import com.example.sistema.gerenciamento.api.Service.CategoriaFinanceiraService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/financeiro/categorias")
public class CategoriaFinanceiraController {
    private final CategoriaFinanceiraService service;
    @GetMapping public List<CategoriaFinanceira> listar() { return service.listar(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CategoriaFinanceira criar(@Valid @RequestBody CategoriaFinanceiraService.Dados dados) { return service.salvar(null, dados); }
    @PutMapping("/{id}")
    public CategoriaFinanceira editar(@PathVariable Long id, @Valid @RequestBody CategoriaFinanceiraService.Dados dados) { return service.salvar(id, dados); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}
