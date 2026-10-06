package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Dto.UsuarioRequest;
import com.example.sistema.gerenciamento.api.Dto.UsuarioResponse;
import com.example.sistema.gerenciamento.api.Entity.*;
import com.example.sistema.gerenciamento.api.Exception.BusinessException;
import com.example.sistema.gerenciamento.api.Repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AcessoService {
    private final UsuarioRepository usuarios;
    private final TokenAcessoRepository tokens;
    private final PasswordEncoder encoder;

    public static String normalizarEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    public static void validarSenha(String senha) {
        if (senha == null || senha.length() < 8 || senha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("A senha deve ter ao menos 8 caracteres e no máximo 72 bytes.");
    }

    @Transactional
    public String convite(String email, Perfil perfil, boolean inicial) {
        String token = novoToken();
        var entry = new TokenAcesso();
        entry.setHash(hash(token)); entry.setTipo("CONVITE");
        entry.setEmail(email == null || email.isBlank() ? null : normalizarEmail(email));
        entry.setPerfil(perfil); entry.setInicial(inicial);
        entry.setExpiraEm(Instant.now().plus(24, ChronoUnit.HOURS));
        tokens.save(entry);
        return token;
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioRequest dto) {
        validarSenha(dto.senha());
        var convite = validarToken(dto.tokenConvite(), "CONVITE");
        var email = normalizarEmail(dto.email());
        if (convite.getEmail() != null && !convite.getEmail().equals(email))
            throw new BusinessException("O convite pertence a outro e-mail.");
        if (usuarios.existsByEmailIgnoreCase(email) || (dto.cpf() != null && usuarios.existsByCpf(dto.cpf())))
            throw new BusinessException("Já existe uma conta com os dados informados.");
        var usuario = Usuario.builder().nomeCompleto(dto.nomeCompleto().trim()).email(email)
                .senha(encoder.encode(dto.senha())).telefone(dto.telefone()).cpf(dto.cpf())
                .dataNascimento(dto.dataNascimento()).perfil(convite.getPerfil()).ativo(true).build();
        convite.setUsado(true);
        return UsuarioResponse.fromEntity(usuarios.save(usuario));
    }

    @Transactional
    public String criarRedefinicao(String email) {
        var usuario = usuarios.findByEmailIgnoreCase(email).filter(Usuario::isAtivo)
                .orElseThrow(() -> new BusinessException("Conta não disponível."));
        tokens.deleteByEmailAndTipo(usuario.getEmail(), "SENHA");
        String raw = novoToken();
        var token = new TokenAcesso();
        token.setHash(hash(raw)); token.setTipo("SENHA"); token.setEmail(usuario.getEmail());
        token.setExpiraEm(Instant.now().plus(30, ChronoUnit.MINUTES));
        tokens.save(token);
        return raw;
    }

    @Transactional
    public void redefinir(String raw, String senha) {
        validarSenha(senha);
        var token = validarToken(raw, "SENHA");
        var usuario = usuarios.findByEmailIgnoreCase(token.getEmail()).filter(Usuario::isAtivo)
                .orElseThrow(() -> new BusinessException("Link inválido ou expirado."));
        usuario.setSenha(encoder.encode(senha));
        usuario.setVersaoAcesso(usuario.getVersaoAcesso() + 1);
        token.setUsado(true);
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(Long id, String nome, String email, String atual, String nova) {
        var usuario = usuarios.findById(id).orElseThrow();
        if (!encoder.matches(atual == null ? "" : atual, usuario.getSenha()))
            throw new BusinessException("Informe sua senha atual corretamente para salvar as alterações.");
        String normalizado = normalizarEmail(email);
        if (!usuario.getEmail().equals(normalizado) && usuarios.existsByEmailIgnoreCase(normalizado))
            throw new BusinessException("E-mail indisponível.");
        usuario.setNomeCompleto(nome.trim()); usuario.setEmail(normalizado);
        if (nova != null && !nova.isBlank()) {
            validarSenha(nova); usuario.setSenha(encoder.encode(nova));
        }
        usuario.setVersaoAcesso(usuario.getVersaoAcesso() + 1);
        return UsuarioResponse.fromEntity(usuario);
    }

    @Transactional
    public UsuarioResponse alterarAcesso(Long id, Perfil perfil, boolean ativo) {
        var administradores = usuarios.bloquearAdministradores();
        var usuario = usuarios.findById(id).orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        protegerUltimoAdministrador(usuario, perfil, ativo, administradores);
        usuario.setPerfil(perfil); usuario.setAtivo(ativo);
        usuario.setVersaoAcesso(usuario.getVersaoAcesso() + 1);
        return UsuarioResponse.fromEntity(usuario);
    }

    public static void protegerUltimoAdministrador(Usuario usuario, Perfil novo, boolean ativo, List<Usuario> admins) {
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR && usuario.isAtivo()
                && (novo != Perfil.ADMINISTRADOR || !ativo)
                && admins.stream().filter(Usuario::isAtivo).count() <= 1)
            throw new BusinessException("Mantenha pelo menos um administrador ativo.");
    }

    private TokenAcesso validarToken(String raw, String tipo) {
        if (raw == null || raw.isBlank()) throw new BusinessException("Informe o token de acesso.");
        return tokens.buscarParaUso(hash(raw.trim()))
                .filter(t -> tipo.equals(t.getTipo()) && !t.isUsado() && t.getExpiraEm().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessException("Token inválido, expirado ou já utilizado."));
    }

    private static String novoToken() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
