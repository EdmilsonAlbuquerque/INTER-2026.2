package com.example.sistema.gerenciamento.api.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Páginas visuais; as rotas REST existentes continuam disponíveis. */
@Controller
public class PaginaController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/cadastro-usuario")
    public String cadastroUsuario() {
        return "cadastro-usuario";
    }

    @GetMapping("/recuperar-senha")
    public String recuperarSenha() {
        return "recuperar-senha";
    }

    @GetMapping("/cadastro-pessoa")
    public String cadastroPessoa() {
        return "cadastro-pessoa";
    }

    @GetMapping("/gerenciamento")
    public String gerenciamento() {
        return "gerenciamento";
    }

    @GetMapping("/servicos")
    public String servicos() { return "servicos"; }

    @GetMapping({"/financeiro", "/DASHFINACEIRO.html"})
    public String financeiro() { return "financeiro"; }

    @GetMapping("/meu-perfil")
    public String meuPerfil() {
        return "meu-perfil";
    }

    @GetMapping("/admin")
    public String admin() { return "admin"; }

    @GetMapping("/redefinir-senha")
    public String redefinirSenha() { return "redefinir-senha"; }

    @GetMapping("/acesso-negado")
    public String acessoNegado(jakarta.servlet.http.HttpServletResponse response) {
        response.setStatus(403);
        return "acesso-negado";
    }

    @GetMapping({"/", "/institucional", "/index.html"})
    public String institucional() { return "institucional"; }

    @GetMapping({"/nossas-acoes", "/nossas-acoes.html", "/blog"})
    public String acoes() { return "nossas-acoes"; }

    @GetMapping("/quem-somos")
    public String quemSomos() { return "redirect:/#quem-somos"; }
}
