package com.example.sistema.gerenciamento.api.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vinculo entre uma pessoa e um servico em um horario especifico. */
// @Entity habilita o armazenamento da participacao pelo JPA.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participacao {

	/** Identificador gerado automaticamente. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Data futura em que o servico sera realizado. */
	@NotNull
	@Future
	private LocalDateTime dataHora;

	// EnumType.STRING preserva o nome do status no banco, evitando ordinal fragil.
	@Enumerated(EnumType.STRING)
	/** Status atual da participacao. */
	private StatusAtividade status = StatusAtividade.AGENDADA;

	// @ManyToOne permite varias participacoes da mesma pessoa.
	@ManyToOne
	// A chave estrangeira e obrigatoria para preservar o vinculo.
	@JoinColumn(name = "pessoa_id", nullable = false)
	/** Pessoa vinculada ao agendamento. */
	private Pessoa pessoa;

	// Uma mesma atividade pode receber varias participacoes.
	@ManyToOne
	@JoinColumn(name = "servico_id", nullable = false)
	/** Servico escolhido pela pessoa. */
	private Servico servico;

}
