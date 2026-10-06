package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Dto.AtualizarStatusRequest;
import com.example.sistema.gerenciamento.api.Entity.Participacao;
import com.example.sistema.gerenciamento.api.Service.ParticipacaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participacoes")
public class ParticipacaoController {

	private final ParticipacaoService service;

	public ParticipacaoController(ParticipacaoService service) { this.service = service; }
	@GetMapping public List<Participacao> listar() { return service.listar(); }
	@GetMapping("/{id}") public Participacao buscar(@PathVariable Long id) { return service.buscar(id); }
	@PostMapping @ResponseStatus(HttpStatus.CREATED)
	public Participacao criar(@Valid @RequestBody Participacao participacao) { return service.salvar(participacao); }
	@PatchMapping("/{id}/status")
	public Participacao atualizarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest request) {
		return service.atualizarStatus(id, request);
	}
	@DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) { service.excluir(id); }

}
