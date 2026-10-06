package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Dto.AtualizarStatusRequest;
import com.example.sistema.gerenciamento.api.Entity.HistoricoStatus;
import com.example.sistema.gerenciamento.api.Entity.Participacao;
import com.example.sistema.gerenciamento.api.Entity.StatusAtividade;
import com.example.sistema.gerenciamento.api.Exception.RecursoNaoEncontradoException;
import com.example.sistema.gerenciamento.api.Repository.HistoricoStatusRepository;
import com.example.sistema.gerenciamento.api.Repository.ParticipacaoRepository;
import com.example.sistema.gerenciamento.api.Repository.PessoaRepository;
import com.example.sistema.gerenciamento.api.Repository.ServicoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipacaoService {

    private final ParticipacaoRepository repository;
    private final PessoaRepository pessoaRepository;
    private final ServicoRepository servicoRepository;
    private final HistoricoStatusRepository historicoRepository;

    // [ GET ] - LISTA TODAS AS PARTICIPAÇÕES
    public List<Participacao> listar() {
        return repository.findAll();
    }

    // [ GET ] - BUSCA PARTICIPAÇÃO POR ID
    public Participacao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participação", id));
    }

    // [ POST ] - CADASTRA UMA NOVA PARTICIPAÇÃO
    public Participacao salvar(Participacao participacao) {
        participacao.setPessoa(pessoaRepository.findById(participacao.getPessoa().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa", participacao.getPessoa().getId())));
        participacao.setServico(servicoRepository.findById(participacao.getServico().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", participacao.getServico().getId())));
        Participacao salva = repository.save(participacao);
        registrarHistorico(salva, salva.getStatus(), "Agendamento criado");
        return salva;
    }

    // [ PUT ] - ATUALIZA O STATUS DA PARTICIPAÇÃO
    public Participacao atualizarStatus(Long id, AtualizarStatusRequest request) {
        Participacao participacao = buscar(id);
        participacao.setStatus(request.status());
        Participacao atualizada = repository.save(participacao);
        registrarHistorico(atualizada, request.status(), request.observacao());
        return atualizada;
    }

    // [ DELETE ] - EXCLUI A PARTICIPAÇÃO POR ID
    public void excluir(Long id) {
        repository.delete(buscar(id));
    }

    // REGISTRA AUTOMATICAMENTE O HISTÓRICO DE STATUS
    private void registrarHistorico(Participacao participacao, StatusAtividade status, String observacao) {
        HistoricoStatus historico = HistoricoStatus.builder()
                .status(status)
                .observacao(observacao)
                .participacao(participacao)
                .build();

        historicoRepository.save(historico);
    }
}