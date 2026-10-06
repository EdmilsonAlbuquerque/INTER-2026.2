package com.example.sistema.gerenciamento.api.Security;

import com.example.sistema.gerenciamento.api.Entity.*;
import com.example.sistema.gerenciamento.api.Repository.*;
import com.example.sistema.gerenciamento.api.Service.AcessoService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class AcessoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired AcessoService acesso;
    Usuario admin, operador, leitor;

    @BeforeEach void preparar() {
        admin = criar("admin@teste.com", Perfil.ADMINISTRADOR);
        operador = criar("operador@teste.com", Perfil.OPERADOR);
        leitor = criar("leitor@teste.com", Perfil.LEITOR);
    }

    Usuario criar(String email, Perfil perfil) {
        return usuarios.save(Usuario.builder().nomeCompleto("Pessoa Teste").email(email)
                .senha(encoder.encode("SenhaTeste123!")).perfil(perfil).build());
    }

    MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", email)
                .param("password", "SenhaTeste123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/gerenciamento"))
                .andReturn().getRequest().getSession(false);
    }

    @Test void sitePublicoEPainelProtegido() throws Exception {
        for (String url : new String[]{"/", "/quem-somos", "/nossas-acoes", "/blog", "/login"}) {
            mvc.perform(get(url)).andExpect(result -> assertThat(result.getResponse().getStatus()).isLessThan(400));
        }
        mvc.perform(get("/institucional/style.css")).andExpect(status().isOk());
        mvc.perform(get("/gerenciamento")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/pessoas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test void loginRealCaseInsensitivePerfilELogout() throws Exception {
        var session = login("ADMIN@TESTE.COM");
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?saiu"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void senhaErradaEContaDesativadaNaoEntram() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("email", admin.getEmail()).param("password", "errada"))
                .andExpect(redirectedUrl("/login?erro"));
        leitor.setAtivo(false); usuarios.flush();
        mvc.perform(post("/login").with(csrf()).param("email", leitor.getEmail()).param("password", "SenhaTeste123!"))
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test void leitorConsultaMasNaoEscreveNemAdministra() throws Exception {
        var session = login(leitor.getEmail());
        mvc.perform(get("/api/pessoas").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/pessoas").session(session).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/usuarios").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/cadastro-pessoa").session(session)).andExpect(redirectedUrl("/acesso-negado"));
        mvc.perform(post("/admin/api/convites").session(session).with(csrf()).contentType("application/json")
                .content("{\"perfil\":\"ADMINISTRADOR\"}")).andExpect(status().isForbidden());
    }

    @Test void operadorCriaEditaMasNaoExcluiNemGerenciaContas() throws Exception {
        var session = login(operador.getEmail());
        mvc.perform(get("/cadastro-pessoa").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/responsaveis").session(session).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Responsavel Teste\",\"email\":\"resp@teste.com\"}"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/pessoas/999").session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios").session(session)).andExpect(status().isForbidden());
    }

    @Test void csrfObrigatorioMesmoParaAdministrador() throws Exception {
        var session = login(admin.getEmail());
        mvc.perform(post("/admin/api/convites").session(session).contentType("application/json")
                .content("{\"perfil\":\"LEITOR\"}")).andExpect(status().isForbidden());
    }

    @Test void conviteDeterminaPerfilESoFuncionaUmaVez() throws Exception {
        String token = acesso.convite("novo@teste.com", Perfil.OPERADOR, false);
        String body = """
                {"nomeCompleto":"Novo Usuario","email":"novo@teste.com","senha":"NovaSenha123!",
                 "tokenConvite":"%s","perfil":"ADMINISTRADOR","ativo":false}
                """.formatted(token);
        mvc.perform(post("/auth/cadastro").with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.perfil").value("OPERADOR"))
                .andExpect(jsonPath("$.ativo").value(true));
        var novo = usuarios.findByEmail("novo@teste.com").orElseThrow();
        assertThat(novo.getSenha()).isNotEqualTo("NovaSenha123!");
        assertThat(encoder.matches("NovaSenha123!", novo.getSenha())).isTrue();
        mvc.perform(post("/auth/cadastro").with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test void cadastroSemConviteNaoElevaPermissoes() throws Exception {
        mvc.perform(post("/auth/cadastro").with(csrf()).contentType("application/json")
                .content("{\"nomeCompleto\":\"Invasor Teste\",\"email\":\"x@teste.com\",\"senha\":\"NovaSenha123!\",\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void alteracaoDePerfilRevogaSessaoExistente() throws Exception {
        var session = login(operador.getEmail());
        acesso.alterarAcesso(operador.getId(), Perfil.LEITOR, true);
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void naoPermiteRemoverUltimoAdministrador() {
        assertThatThrownBy(() -> acesso.alterarAcesso(admin.getId(), Perfil.LEITOR, false))
                .hasMessageContaining("pelo menos um administrador");
    }

    @Test void perfilExigeSenhaAtualERevogaSessaoAoAlterar() throws Exception {
        var session = login(leitor.getEmail());
        mvc.perform(put("/auth/me").session(session).with(csrf()).contentType("application/json")
                .content("{\"nomeCompleto\":\"Nome Atualizado\",\"email\":\"leitor@teste.com\",\"senhaAtual\":\"errada\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/me").session(session).with(csrf()).contentType("application/json")
                .content("{\"nomeCompleto\":\"Nome Atualizado\",\"email\":\"leitor@teste.com\",\"senhaAtual\":\"SenhaTeste123!\",\"novaSenha\":\"SenhaNova123!\",\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("LEITOR"));
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test void redefinicaoDeSenhaInvalidaTokenESessoes() throws Exception {
        var session = login(leitor.getEmail());
        String token = acesso.criarRedefinicao(leitor.getEmail());
        String body = "{\"token\":\"" + token + "\",\"senha\":\"SenhaNova123!\"}";
        mvc.perform(post("/auth/redefinir").with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/redefinir").with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/login").with(csrf()).param("email", leitor.getEmail()).param("password", "SenhaNova123!"))
                .andExpect(redirectedUrl("/gerenciamento"));
    }

    @Test void lembrarDispositivoEmiteCookieEAutenticaSemSessao() throws Exception {
        var result = mvc.perform(post("/login").with(csrf()).param("email", leitor.getEmail())
                .param("password", "SenhaTeste123!").param("remember-me", "on"))
                .andExpect(redirectedUrl("/gerenciamento")).andReturn();
        var cookie = result.getResponse().getCookie("remember-me");
        assertThat(cookie).isNotNull(); assertThat(cookie.isHttpOnly()).isTrue();
        mvc.perform(get("/auth/me").cookie(cookie)).andExpect(status().isOk());
        leitor.setAtivo(false); usuarios.flush();
        mvc.perform(get("/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }
}
