package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Entity.Responsavel;
import com.example.sistema.gerenciamento.api.Exception.RecursoNaoEncontradoException;
import com.example.sistema.gerenciamento.api.Repository.ResponsavelRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResponsavelService {

    private final ResponsavelRepository repository;

    public ResponsavelService(ResponsavelRepository repository) { this.repository = repository; }

    public List<Responsavel> listar() { return repository.findAll(); }

    public Responsavel buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Responsável", id));
    }

    public Responsavel salvar(Responsavel responsavel) { return repository.save(responsavel); }

    public Responsavel atualizar(Long id, Responsavel dados) {
        Responsavel responsavel = buscar(id);
        responsavel.setNome(dados.getNome());
        responsavel.setEmail(dados.getEmail());
        responsavel.setTelefone(dados.getTelefone());
        return repository.save(responsavel);
    }

    public void excluir(Long id) { repository.delete(buscar(id)); }
}