package com.example.sistema.gerenciamento.api.Security;
import com.example.sistema.gerenciamento.api.Service.FinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.financeiro.dados-exemplo", havingValue="true")
public class FinanceiroBootstrap implements ApplicationRunner {
    private final FinanceiroService service;
    public void run(ApplicationArguments args) { service.gerarExemplos(); }
}
