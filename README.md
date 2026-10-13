# Sistema de gerenciamento — Instituto Manancial

## Executar no Windows

Requer JDK 21 e acesso à internet para baixar as dependências na primeira execução.
Na pasta que contém `pom.xml`, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Abra http://localhost:8080. Também é possível executar a classe
`com.example.sistema.gerenciamento.api.Application` pela IDE com JDK 21.

O perfil padrão `local` usa H2 persistente em `data/agendamento.mv.db`.
Os dados permanecem após reiniciar. Execute uma instância por vez.
Os testes usam um banco separado em memória.

## Telas incorporadas

| Rota | HTML de referência |
| --- | --- |
| `/` ou `/institucional` | `INTER-/index.html` |
| `/quem-somos` | Seção Quem somos da página institucional |
| `/nossas-acoes` ou `/blog` | `INTER-/nossas-acoes.html` |
| `/login` | `tela-login/code.html` |
| `/cadastro-usuario` | `tela-cadastro-usuario/code.html` |
| `/recuperar-senha` | `tela-de-recuperar-senha/code.html` |
| `/cadastro-pessoa` | `tela-de-cadstro-de-pessoas/code.html` |
| `/gerenciamento` | `tela-gerenciamento/code.html` |
| `/meu-perfil` | `tela-meu-perfil/code.html` |
| `/financeiro` | `DASHFINACEIRO.html`, conectado aos lançamentos financeiros |
| `/servicos` | Cadastro e gerenciamento de serviços reais |

As cópias usadas pela aplicação estão em `src/main/resources/templates`.
Mantêm as classes, CSS, fontes, imagens e conteúdo visual dos originais.
A navegação entre telas foi conectada; os arquivos de referência foram preservados.
Tailwind, fontes e imagens continuam nos endereços externos dos modelos e
precisam de internet no navegador. O gerenciamento permite rolar até o final,
sem a altura fixa que limitava o HTML fornecido.

O site institucional é público, com acesso ao sistema pelo menu. CSS, JavaScript
e imagens de `INTER-` foram copiados para `static/institucional`. Duas imagens
referenciadas no modelo (futebol e cestas básicas) não foram fornecidas; seus
espaços mostram temporariamente o logo do Instituto. `/blog` apresenta a galeria
Nossas ações fornecida, sem criar artigos ou um CMS.

Login, cadastro por convite, edição de nome/e-mail/senha e administração de
acessos estão conectados ao banco. O painel e o cadastro de pessoas ainda
preservam os exemplos visuais; submissão desses cadastros e
Pix não foram implementados nesta etapa. A API operacional existente está
protegida pelas permissões abaixo.

## Financeiro, serviços e foto

O menu **Financeiro**, disponível para Administrador, registra receitas e despesas
no banco, com edição, exclusão e filtros por ano, tipo e descrição/categoria.
Totais, gráfico mensal e categorias são calculados a partir desses registros.
**Exportar relatório** oferece PDF e Excel (.xlsx) respeitando os filtros.

No perfil `local`, `app.financeiro.dados-exemplo=true` gera uma receita e uma
despesa para cada mês do ano até o mês atual, somente quando ainda não há
lançamentos. A marca de inicialização impede duplicação após reiniciar e também
impede recriação dos exemplos removidos. Os registros têm a indicação
**Demonstração**, entram nos totais e podem ser removidos pelo botão próprio,
preservando os lançamentos reais. Em PostgreSQL a carga fica desabilitada por padrão.

**Serviços** usa os registros reais da API, permite cadastrar responsáveis,
filtrar por nome/categoria e situação e exportar PDF ou Excel. Administrador
pode excluir; Operador pode cadastrar/editar; Leitor somente consulta/exporta.

Em **Meu perfil**, Trocar Foto aceita JPG/PNG de até 5 MB e 16 milhões de pixels.
A imagem é validada, redimensionada para até 512 px e salva no banco. A foto
do perfil e do cabeçalho é atualizada e permanece após recarregar a página.
Cada usuário acessa apenas a própria foto. Remover também exclui a imagem salva.

## Primeiro acesso e RBAC

1. Inicie o projeto e procure no terminal a mensagem **PRIMEIRO ACESSO**.
2. Copie o **Token de Convite** exibido e abra `/cadastro-usuario`.
3. Preencha seus dados e o token para criar a primeira conta Administrador.
4. Entre em `/login`. Use **Usuários e acessos** no cabeçalho para abrir `/admin`.
5. Gere convites para a equipe, escolhendo o perfil e, opcionalmente, o e-mail
   autorizado. A tela também permite alterar perfis e ativar/desativar contas.

O convite inicial só é gerado quando não há administrador ativo. No perfil
`local`, um novo token substitui o anterior a cada reinicialização enquanto
o primeiro cadastro não for concluído. Em outros perfis, a inicialização deve
ser habilitada explicitamente com `APP_BOOTSTRAP_ENABLED=true`; após criar o
administrador, remova essa variável. Não há conta com senha padrão.

| Permissão | Administrador | Operador | Leitor |
| --- | --- | --- | --- |
| Consultar painel e API operacional | Sim | Sim | Sim |
| Cadastrar/editar pessoas, responsáveis, serviços e participações | Sim | Sim | Não |
| Registrar histórico de status | Sim | Sim | Não |
| Excluir registros operacionais | Sim | Não | Não |
| Administrar usuários, convites e perfis | Sim | Não | Não |
| Editar o próprio nome, e-mail e senha | Sim | Sim | Sim |

As permissões são verificadas no servidor, inclusive em chamadas diretas à API.
O perfil de novos cadastros vem exclusivamente do convite. Novas contas criadas
pela API administrativa `/usuarios` começam como Leitor; o administrador pode
ajustá-las em `/admin`. Não é possível desativar, excluir ou rebaixar o último
administrador ativo.

Senhas são armazenadas com BCrypt; as senhas em texto da versão anterior são
migradas na inicialização. Contas preexistentes recebem o perfil Leitor.
Não são retornadas senhas nos JSONs. Alterações de senha, perfil ou situação
invalidam as sessões anteriores. O perfil próprio exige a senha atual para
salvar e solicita novo login após alterações.

O login usa sessão e CSRF. Para consumir a API em um cliente, obtenha primeiro
`GET /auth/csrf`, preserve os cookies e envie o token nas operações de escrita.
O login é `POST /login` com formulário `email`, `password` e o parâmetro CSRF;
o logout é `POST /logout`. `GET /auth/me` retorna a conta atual.
APIs retornam 401 sem autenticação e 403 quando falta permissão ou CSRF.

“Lembrar deste dispositivo” usa cookie HttpOnly de sete dias. Defina uma chave
secreta aleatória e persistente em `APP_REMEMBER_KEY` para manter esse recurso
entre reinicializações; sem essa variável, reiniciar exige novo login.

## Recuperação de senha

O administrador pode gerar um link de redefinição em `/admin` e entregá-lo ao
titular da conta. Esse link expira em 30 minutos e só pode ser usado uma vez.
Convites expiram em 24 horas e também são de uso único. Os tokens são armazenados
no banco apenas como hash SHA-256.

Para o envio automático pela tela “Esqueceu a senha?”, configure o seu SMTP:

```powershell
$env:SPRING_MAIL_HOST = 'smtp.seu-provedor.com'
$env:SPRING_MAIL_PORT = '587'
$env:SPRING_MAIL_USERNAME = 'seu-usuario'
$env:SPRING_MAIL_PASSWORD = 'sua-senha-smtp'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = 'true'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = 'true'
$env:MAIL_FROM = 'contato@seu-dominio.com'
$env:APP_PUBLIC_URL = 'https://seu-dominio.com'
```

Sem SMTP, a página orienta a solicitar o link ao administrador. Nenhum envio é
simulado. Para hospedagem HTTPS, configure também
`SERVER_SERVLET_SESSION_COOKIE_SECURE=true`.

## PostgreSQL opcional

Crie o banco e configure as credenciais no ambiente:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'postgres'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/agendamento'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'sua-senha'
.\mvnw.cmd spring-boot:run
```

Para voltar ao banco local, remova `SPRING_PROFILES_ACTIVE` da sessão.

## Verificação

```powershell
.\mvnw.cmd test
```

Foram corrigidos o acesso ao PowerShell e a leitura de `.m2` no Wrapper,
os testes incompatíveis com as entidades/repositórios atuais e a ausência de
configuração de banco para execução normal. H2 agora está disponível em runtime.

A suíte inclui testes reais de autenticação, permissões por método HTTP, CSRF,
cadastro por convite, revogação de sessão, último administrador, troca de senha,
cookie de lembrar dispositivo e páginas institucionais públicas. A validação
no navegador usa uma instância temporária com banco em memória na porta 8081;
encerre-a ao terminar para manter livre a execução pela IDE.

## Acessibilidade

As telas da aplicação incluem um link "Pular para o conteúdo" como primeiro
atalho de teclado e um contorno visível nos controles ao navegar com Tab.
As animações e transições são reduzidas quando essa preferência está ativada
no sistema operacional. O botão de mostrar senha informa seu estado aos
leitores de tela, e as mensagens dos formulários são anunciadas por inteiro.

Para conferir no navegador:

1. Abra uma tela e pressione Tab: o link de pular conteúdo deve aparecer.
2. Pressione Enter: o foco deve ir para o conteúdo principal; pressione Tab
   novamente para continuar nos controles dessa área.
3. Navegue com Tab e Shift+Tab e confira o contorno nos links e campos.
4. No login, ative "Mostrar senha" com Espaço e confira o estado com um
   leitor de tela. Confira também o anúncio de um erro de autenticação.
5. Ative a preferência de reduzir movimento no sistema e recarregue a tela.

Essas melhorias são uma base de acessibilidade; não representam uma auditoria
completa de contraste, formulários, tabelas ou compatibilidade com leitores de tela.

Os formulários também mostram erros junto aos campos, preservam instruções
para leitores de tela e direcionam o foco ao primeiro problema. O cadastro de
serviços oferece uma sugestão experimental de categoria usando Naive Bayes,
executado no navegador com exemplos fictícios e aplicação explícita pelo usuário.

Veja a explicação, os exemplos para apresentação e os comandos de teste em
[Acessibilidade e IA](docs/ACESSIBILIDADE-E-IA.md).

Todas as telas também oferecem o botão **Acessibilidade** para aumentar o
texto até 150%, ativar alto contraste e restaurar o padrão. As preferências
ficam salvas no navegador. As imagens dos formulários de acesso são locais.
Os filtros do gerenciamento funcionam sobre as seis linhas de demonstração,
com contagem coerente, busca por nome/CPF, status, serviço e faixa etária.
