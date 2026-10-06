package com.example.sistema.gerenciamento.api.Security;

import com.example.sistema.gerenciamento.api.Entity.Perfil;
import com.example.sistema.gerenciamento.api.Repository.*;
import com.example.sistema.gerenciamento.api.Service.AcessoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor
public class InicializarAcesso implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final TokenAcessoRepository tokens;
    private final PasswordEncoder encoder;
    private final AcessoService acesso;
    private final Environment environment;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        // Migra as senhas em texto que eram gravadas pela versão anterior.
        usuarios.findAll().forEach(usuario -> {
            if (!usuario.getSenha().matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
                usuario.setSenha(encoder.encode(usuario.getSenha()));
                usuario.setVersaoAcesso(usuario.getVersaoAcesso() + 1);
            }
        });
        if (!environment.getProperty("app.bootstrap.enabled", Boolean.class, true)
                || usuarios.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)) return;
        if (environment.acceptsProfiles(Profiles.of("local"))
                || environment.getProperty("app.bootstrap.enabled", Boolean.class, false)) {
            tokens.deleteByInicialTrue();
            String convite = acesso.convite(null, Perfil.ADMINISTRADOR, true);
            LoggerFactory.getLogger(getClass()).warn(
                    "PRIMEIRO ACESSO: abra /cadastro-usuario e use o Token de Convite: {} (válido por 24h).", convite);
        }
    }
}
