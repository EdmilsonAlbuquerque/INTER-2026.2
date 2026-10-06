package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Dto.*;
import com.example.sistema.gerenciamento.api.Entity.Perfil;
import com.example.sistema.gerenciamento.api.Repository.UsuarioRepository;
import com.example.sistema.gerenciamento.api.Security.UsuarioAutenticado;
import com.example.sistema.gerenciamento.api.Service.AcessoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class AcessoController {
    private final AcessoService acesso;
    private final UsuarioRepository usuarios;
    private final ObjectProvider<JavaMailSender> mail;
    @Value("${app.public-url:http://localhost:8080}") private String baseUrl;
    @Value("${app.mail.from:noreply@localhost}") private String remetente;

    public record PerfilRequest(@NotBlank @Size(max=150) String nomeCompleto,
            @NotBlank @Email @Size(max=150) String email, @NotBlank String senhaAtual, String novaSenha) {}
    public record ConviteRequest(@Email @Size(max=150) String email, @NotNull Perfil perfil) {}
    public record AcessoRequest(@NotNull Perfil perfil, boolean ativo) {}
    public record EmailRequest(@NotBlank @Email @Size(max=150) String email) {}
    public record RedefinirRequest(@NotBlank String token, @NotBlank String senha) {}

    @GetMapping("/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName(), "parameterName", token.getParameterName());
    }

    @PostMapping("/auth/cadastro") @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody UsuarioRequest dto) { return acesso.cadastrar(dto); }

    @GetMapping("/auth/me")
    public UsuarioResponse atual(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return UsuarioResponse.fromEntity(usuarios.findById(principal.getId()).orElseThrow());
    }

    @PutMapping("/auth/me")
    public UsuarioResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @RequestBody PerfilRequest dto) {
        return acesso.atualizarPerfil(principal.getId(), dto.nomeCompleto(), dto.email(), dto.senhaAtual(), dto.novaSenha());
    }

    @PostMapping("/admin/api/convites")
    public Map<String, String> convite(@Valid @RequestBody ConviteRequest dto) {
        String token = acesso.convite(dto.email(), dto.perfil(), false);
        return Map.of("token", token, "url", baseUrl + "/cadastro-usuario?convite=" + token);
    }

    @PatchMapping("/admin/api/usuarios/{id}/acesso")
    public UsuarioResponse permissao(@PathVariable Long id, @Valid @RequestBody AcessoRequest dto) {
        return acesso.alterarAcesso(id, dto.perfil(), dto.ativo());
    }

    @PostMapping("/admin/api/usuarios/{id}/redefinir")
    public Map<String, String> redefinicaoAdmin(@PathVariable Long id) {
        var usuario = usuarios.findById(id).orElseThrow();
        return Map.of("url", baseUrl + "/redefinir-senha?token=" + acesso.criarRedefinicao(usuario.getEmail()));
    }

    @PostMapping("/auth/recuperar")
    public ResponseEntity<Map<String, String>> recuperar(@Valid @RequestBody EmailRequest dto) {
        var sender = mail.getIfAvailable();
        if (sender == null) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Solicite um link de redefinição ao administrador. O envio de e-mail ainda não está configurado."));
        var usuario = usuarios.findByEmailIgnoreCase(dto.email().trim()).filter(u -> u.isAtivo());
        if (usuario.isPresent()) {
            var message = new SimpleMailMessage();
            message.setFrom(remetente); message.setTo(usuario.get().getEmail());
            message.setSubject("Redefinição de senha — Instituto Manancial");
            message.setText("Use o link em até 30 minutos: " + baseUrl + "/redefinir-senha?token="
                    + acesso.criarRedefinicao(usuario.get().getEmail()) + "\nSe não solicitou, ignore esta mensagem.");
            sender.send(message);
        }
        return ResponseEntity.ok(Map.of("message", "Se houver uma conta ativa para este e-mail, você receberá um link de redefinição."));
    }

    @PostMapping("/auth/redefinir")
    public Map<String, String> redefinir(@Valid @RequestBody RedefinirRequest dto) {
        acesso.redefinir(dto.token(), dto.senha());
        return Map.of("message", "Senha atualizada. Entre com sua nova senha.");
    }
}
