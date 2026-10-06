package com.example.sistema.gerenciamento.api.Security;

import com.example.sistema.gerenciamento.api.Entity.*;
import com.example.sistema.gerenciamento.api.Repository.*;
import com.example.sistema.gerenciamento.api.Service.FinanceiroService;
import java.io.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class FinanceiroPerfilIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired FinanceiroService financeiro;
    @Autowired FinanceiroRepository repository;
    @Autowired InicializacaoFinanceiraRepository inicializacao;
    @Autowired CategoriaFinanceiraRepository categorias;
    MockHttpSession login(Perfil perfil) throws Exception {
        String email=perfil.name()+"@financeiro.test";
        usuarios.save(Usuario.builder().nomeCompleto("Teste Financeiro").email(email)
            .senha(encoder.encode("SenhaTeste123!")).perfil(perfil).build());
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("email",email).param("password","SenhaTeste123!"))
            .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }
    @Test void exemplosUmaVezERemocaoPreservaReais() {
        inicializacao.deleteAll();repository.deleteAll();
        financeiro.gerarExemplos();long total=repository.count();
        assertThat(total).isEqualTo(LocalDate.now().getMonthValue()*2);
        assertThat(repository.findAll()).allMatch(LancamentoFinanceiro::isExemplo);
        financeiro.gerarExemplos();assertThat(repository.count()).isEqualTo(total);
        financeiro.salvar(null,new FinanceiroService.Dados(LancamentoFinanceiro.Tipo.RECEITA,"Real","Doação",LocalDate.now(),new BigDecimal("123.45")));
        financeiro.removerExemplos();financeiro.gerarExemplos();
        assertThat(repository.findAll()).hasSize(1).allMatch(l->!l.isExemplo());
    }
    @Test void naoAdicionaExemplosEmBancoComLancamentosReais() {
        inicializacao.deleteAll();repository.deleteAll();
        financeiro.salvar(null,new FinanceiroService.Dados(LancamentoFinanceiro.Tipo.DESPESA,"Real","Materiais",LocalDate.now(),BigDecimal.TEN));
        financeiro.gerarExemplos();assertThat(repository.count()).isEqualTo(1);
    }
    @Test void categoriasPersistemRenomeiamLancamentosEProtegemRemocao() throws Exception {
        var admin=login(Perfil.ADMINISTRADOR);
        mvc.perform(post("/api/financeiro/categorias").session(admin).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"  Projeto especial  \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Projeto especial"))
            .andReturn().getResponse().getContentAsString();
        var categoria=categorias.findByChave("projeto especial").orElseThrow();
        var item=financeiro.salvar(null,new FinanceiroService.Dados(LancamentoFinanceiro.Tipo.DESPESA,"Material do projeto","projeto especial",LocalDate.now(),BigDecimal.TEN));
        assertThat(item.getCategoria()).isEqualTo("Projeto especial");
        mvc.perform(post("/api/financeiro/categorias").session(admin).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"PROJETO ESPECIAL\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/financeiro/categorias/"+categoria.getId()).session(admin).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"Assistência especial\"}"))
            .andExpect(status().isOk());
        assertThat(repository.findById(item.getId()).orElseThrow().getCategoria()).isEqualTo("Assistência especial");
        mvc.perform(delete("/api/financeiro/categorias/"+categoria.getId()).session(admin).with(csrf()))
            .andExpect(status().isBadRequest());
        assertThat(repository.existsById(item.getId())).isTrue();
        mvc.perform(get("/api/financeiro/relatorio?formato=xlsx&tipo=DESPESA&busca=Material&buscaHistorico=especial").session(admin))
            .andExpect(status().isOk());
        financeiro.excluir(item.getId());
        mvc.perform(delete("/api/financeiro/categorias/"+categoria.getId()).session(admin).with(csrf()))
            .andExpect(status().isNoContent());
        assertThat(categorias.existsById(categoria.getId())).isFalse();
        var leitor=login(Perfil.LEITOR);
        mvc.perform(get("/api/financeiro/categorias").session(leitor)).andExpect(status().isForbidden());
        mvc.perform(post("/api/financeiro/categorias").session(leitor).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"Sem permissão\"}")).andExpect(status().isForbidden());
    }
    @Test void financeiroCalculaFiltraExportaEProtege() throws Exception {
        var admin=login(Perfil.ADMINISTRADOR);
        repository.deleteAll();
        mvc.perform(post("/api/financeiro").session(admin).with(csrf()).contentType("application/json")
            .content("{\"tipo\":\"RECEITA\",\"descricao\":\"Doação teste\",\"categoria\":\"Doações\",\"data\":\"2026-10-01\",\"valor\":123.45}"))
            .andExpect(status().isCreated());
        financeiro.salvar(null,new FinanceiroService.Dados(LancamentoFinanceiro.Tipo.DESPESA,"Materiais","Compras",LocalDate.of(2026,10,1),new BigDecimal("23.40")));
        mvc.perform(get("/api/financeiro?ano=2026").session(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.saldo").value(100.05)).andExpect(jsonPath("$.lancamentos.length()").value(2));
        mvc.perform(get("/api/financeiro?tipo=DESPESA&busca=Materiais").session(admin))
            .andExpect(jsonPath("$.receitas").value(0)).andExpect(jsonPath("$.despesas").value(23.40));
        byte[] pdf=mvc.perform(get("/api/financeiro/relatorio?formato=pdf&tipo=RECEITA").session(admin))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
        try(var doc=Loader.loadPDF(pdf)){assertThat(new PDFTextStripper().getText(doc)).contains("Doação teste").doesNotContain("Materiais");}
        byte[] excel=mvc.perform(get("/api/financeiro/relatorio?formato=xlsx&tipo=DESPESA").session(admin))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(excel))){assertThat(book.getSheetAt(0).getRow(4).getCell(1).getStringCellValue()).isEqualTo("Materiais");}
        mvc.perform(get("/financeiro").session(admin)).andExpect(status().isOk());
        for(var perfil:new Perfil[]{Perfil.OPERADOR,Perfil.LEITOR}){
            var session=login(perfil);
            mvc.perform(get("/api/financeiro").session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/api/financeiro/relatorio").session(session)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/financeiro").session(admin).with(csrf()).contentType("application/json")
            .content("{\"tipo\":\"RECEITA\",\"descricao\":\"Inválido\",\"categoria\":\"Outros\",\"data\":\"2026-10-01\",\"valor\":-1}"))
            .andExpect(status().isBadRequest());
    }
    @Test void fotoPersisteAtualizaERespeitaProprietario() throws Exception {
        var admin=login(Perfil.ADMINISTRADOR);var leitor=login(Perfil.LEITOR);
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(900,600,BufferedImage.TYPE_INT_RGB),"png",out);
        var file=new MockMultipartFile("arquivo","foto.png","image/png",out.toByteArray());
        mvc.perform(multipart("/auth/me/foto").file(file).session(admin).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/auth/me").session(admin)).andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.startsWith("/auth/me/foto?v=")));
        byte[] data=mvc.perform(get("/auth/me/foto").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(data)).getWidth()).isEqualTo(512);
        mvc.perform(get("/auth/me/foto").session(leitor)).andExpect(status().isNotFound());
        mvc.perform(multipart("/auth/me/foto").file(new MockMultipartFile("arquivo","fake.png","image/png","invalido".getBytes())).session(admin).with(csrf()))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/auth/me/foto").session(admin).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/auth/me/foto").session(admin)).andExpect(status().isNotFound());
    }
}
