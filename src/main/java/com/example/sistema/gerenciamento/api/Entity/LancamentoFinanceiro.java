package com.example.sistema.gerenciamento.api.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class LancamentoFinanceiro {
    public enum Tipo { RECEITA, DESPESA }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Tipo tipo;
    @Column(nullable=false, length=200) private String descricao;
    @Column(nullable=false, length=100) private String categoria;
    @Column(nullable=false) private LocalDate data;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal valor;
    @Column(nullable=false) private boolean exemplo;
}
