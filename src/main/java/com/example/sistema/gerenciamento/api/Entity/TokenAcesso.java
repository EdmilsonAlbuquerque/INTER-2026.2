package com.example.sistema.gerenciamento.api.Entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class TokenAcesso {
    @Id private String hash;
    @Column(nullable = false) private String tipo;
    private String email;
    @Enumerated(EnumType.STRING) private Perfil perfil;
    @Column(nullable = false) private Instant expiraEm;
    private boolean usado;
    private boolean inicial;
}
