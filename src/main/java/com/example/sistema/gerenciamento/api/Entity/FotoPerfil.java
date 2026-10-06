package com.example.sistema.gerenciamento.api.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Getter @Setter @NoArgsConstructor
public class FotoPerfil {
    @Id private Long usuarioId;
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(columnDefinition = "bytea", nullable = false)
    private byte[] imagem;
}
