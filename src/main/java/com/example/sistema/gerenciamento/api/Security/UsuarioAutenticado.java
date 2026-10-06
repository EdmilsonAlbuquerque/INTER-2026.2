package com.example.sistema.gerenciamento.api.Security;

import com.example.sistema.gerenciamento.api.Entity.Usuario;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UsuarioAutenticado extends User {
    private final Long id;
    private final long versao;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenha(), usuario.isAtivo(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
        id = usuario.getId();
        versao = usuario.getVersaoAcesso();
    }

    public Long getId() { return id; }
    public long getVersao() { return versao; }
}
