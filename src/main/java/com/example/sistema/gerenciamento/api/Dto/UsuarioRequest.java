package com.example.sistema.gerenciamento.api.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UsuarioRequest(
        @NotBlank(message = "O nome completo é obrigatório")
        @Size(max = 150, message = "O nome completo não pode ter mais de 150 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O nome não deve conter números ou caracteres especiais")
        String nomeCompleto,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 150, message = "O e-mail não pode ter mais de 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve conter entre 8 e 72 caracteres")
        String senha,

        @Size(max = 20, message = "O telefone não pode ter mais de 20 caracteres")
        @Pattern(regexp = "^[0-9]*$", message = "O telefone deve conter apenas números")
        String telefone,

        @Size(min = 11, max = 11, message = "O CPF deve conter exatamente 11 números")
        @Pattern(regexp = "^[0-9]*$", message = "O CPF deve conter apenas números (sem pontos ou traço)")
        String cpf,

        String fotoUrl,

        String tokenConvite,

        LocalDate dataNascimento,

        Boolean ativo
) {}
