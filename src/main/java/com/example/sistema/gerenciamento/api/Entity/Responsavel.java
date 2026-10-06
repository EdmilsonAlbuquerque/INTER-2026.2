package com.example.sistema.gerenciamento.api.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Gestor responsavel por um ou mais servicos. */
// @Entity define o mapeamento persistente do gestor.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Responsavel {

	/** Identificador gerado pelo banco. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Nome obrigatorio do responsavel. */
	@NotBlank
	private String nome;

	/** Email obrigatorio com formato validado. */
	@Email
	@NotBlank
	private String email;

	/** Telefone opcional para contato. */
	private String telefone;
}
