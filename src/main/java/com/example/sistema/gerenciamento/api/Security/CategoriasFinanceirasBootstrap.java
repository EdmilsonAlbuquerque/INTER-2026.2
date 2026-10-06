package com.example.sistema.gerenciamento.api.Security;

import com.example.sistema.gerenciamento.api.Service.CategoriaFinanceiraService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Order(0)
public class CategoriasFinanceirasBootstrap implements ApplicationRunner {
    private final CategoriaFinanceiraService categorias;
    public void run(ApplicationArguments args) { categorias.inicializar(); }
}
