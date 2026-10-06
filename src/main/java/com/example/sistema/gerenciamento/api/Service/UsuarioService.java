package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Dto.UsuarioRequest;
import com.example.sistema.gerenciamento.api.Dto.UsuarioResponse;
import com.example.sistema.gerenciamento.api.Entity.Usuario;
import com.example.sistema.gerenciamento.api.Exception.ResourceNotFoundException;
import com.example.sistema.gerenciamento.api.Repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // [ GET ] - RETORNA TODOS OS USUÁRIOS CADASTRADOS
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    // [ GET ] - RETORNA APENAS OS USUÁRIOS COM STATUS ATIVO
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarAtivos() {
        return usuarioRepository.findByAtivoTrue().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    // [ GET ] - BUSCA UM USUÁRIO PELO ID OU LANÇA EXCEÇÃO CASO NÃO ENCONTRE
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
        return UsuarioResponse.fromEntity(usuario);
    }

    // [ POST ] - VALIDA DUPLICIDADES E CADASTRA UM NOVO USUÁRIO
    @Transactional
    public UsuarioResponse salvar(UsuarioRequest dto) {
        AcessoService.validarSenha(dto.senha());
        if (usuarioRepository.existsByEmailIgnoreCase(dto.email().trim())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com o e-mail informado.");
        }
        if (dto.cpf() != null && !dto.cpf().isBlank() && usuarioRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com o CPF informado.");
        }

        Usuario usuario = Usuario.builder()
                .nomeCompleto(dto.nomeCompleto())
                .email(AcessoService.normalizarEmail(dto.email()))
                .senha(passwordEncoder.encode(dto.senha()))
                .telefone(dto.telefone())
                .cpf(dto.cpf())
                .fotoUrl(dto.fotoUrl())
                .dataNascimento(dto.dataNascimento())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .build();

        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    // [ PUT ] - ATUALIZA OS DADOS DE UM USUÁRIO EXISTENTE
    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest dto) {
        var administradores = usuarioRepository.bloquearAdministradores();
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        if (!usuario.getEmail().equalsIgnoreCase(dto.email().trim()) && usuarioRepository.existsByEmailIgnoreCase(dto.email().trim())) {
            throw new IllegalArgumentException("Já existe outro usuário cadastrado com o e-mail informado.");
        }

        if (dto.cpf() != null && !dto.cpf().isBlank()
                && !dto.cpf().equals(usuario.getCpf())
                && usuarioRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException("Já existe outro usuário cadastrado com o CPF informado.");
        }

        usuario.setNomeCompleto(dto.nomeCompleto());
        usuario.setEmail(AcessoService.normalizarEmail(dto.email()));
        if (dto.senha() != null && !dto.senha().isBlank()) {
            AcessoService.validarSenha(dto.senha());
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }
        usuario.setTelefone(dto.telefone());
        usuario.setCpf(dto.cpf());
        usuario.setFotoUrl(dto.fotoUrl());
        usuario.setDataNascimento(dto.dataNascimento());
        if (dto.ativo() != null) {
            AcessoService.protegerUltimoAdministrador(usuario, usuario.getPerfil(), dto.ativo(), administradores);
            usuario.setAtivo(dto.ativo());
        }
        usuario.setVersaoAcesso(usuario.getVersaoAcesso() + 1);

        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    // [ DELETE ] - REMOVE UM USUÁRIO DO BANCO DE DADOS PELO ID
    @Transactional
    public void excluir(Long id) {
        var administradores = usuarioRepository.bloquearAdministradores();
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
        }
        var usuario = usuarioRepository.findById(id).orElseThrow();
        AcessoService.protegerUltimoAdministrador(usuario, usuario.getPerfil(), false, administradores);
        usuarioRepository.deleteById(id);
    }
}
