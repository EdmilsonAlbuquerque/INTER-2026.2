package com.example.sistema.gerenciamento.api.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class CategoriaFinanceira {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String nome;
    @Column(nullable = false, unique = true, length = 100) private String chave;
}
