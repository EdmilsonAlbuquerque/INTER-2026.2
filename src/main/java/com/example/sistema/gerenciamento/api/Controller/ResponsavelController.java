package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Entity.Responsavel;
import com.example.sistema.gerenciamento.api.Service.ResponsavelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/responsaveis")
public class ResponsavelController {

	private final ResponsavelService service;

	public ResponsavelController(ResponsavelService service) { this.service = service; }
	@GetMapping public List<Responsavel> listar() { return service.listar(); }
	@GetMapping("/{id}") public Responsavel buscar(@PathVariable Long id) { return service.buscar(id); }
	@PostMapping @ResponseStatus(HttpStatus.CREATED)
	public Responsavel criar(@Valid @RequestBody Responsavel responsavel) { return service.salvar(responsavel); }
	@PutMapping("/{id}") public Responsavel atualizar(@PathVariable Long id, @Valid @RequestBody Responsavel responsavel) {
		return service.atualizar(id, responsavel);
	}
	@DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) { service.excluir(id); }
}
