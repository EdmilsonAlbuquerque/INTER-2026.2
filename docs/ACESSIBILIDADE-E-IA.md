# Acessibilidade e classificação de serviços com IA

## O que foi implementado

O sistema passou a oferecer validação acessível nos formulários e uma sugestão
experimental de categoria no cadastro de serviços. A IA funciona localmente no
navegador, sem conta externa, chave de API ou alteração do banco de dados.

## Ajustes visuais para facilitar a leitura

Todas as telas têm um botão **Acessibilidade**, no canto inferior direito.
Ele abre um painel com controles grandes e rótulos simples:

- **A+ / A−:** ajustam o texto em 100%, 115%, 130% e 150%.
- **Alto contraste:** usa fundo preto, texto branco e links amarelos.
- **Restaurar padrão:** retorna às cores e ao tamanho originais.
- **Fechar:** fecha o painel; a tecla Esc também funciona.

Esses recursos permitem que pessoas idosas ou com dificuldade de visão
adaptem a leitura à sua preferência. O botão do painel tem 48 pixels de altura
e os controles do painel também têm áreas grandes para clicar.
As preferências ficam no `localStorage` e são recuperadas ao mudar de tela ou
voltar ao site no mesmo navegador.

O script `acessibilidade-visual.js` amplia os tamanhos originais de texto e a
altura das linhas, preservando a hierarquia entre títulos e conteúdo. Textos
carregados depois, como resultados de busca, também recebem o ajuste.
Restaurar o padrão devolve os estilos originais. Ícones e textos dentro de
gráficos SVG não são ampliados por esse controle.

O alto contraste troca as cores da interface sem inverter fotos. Campos com
erro usam borda tracejada e mantêm sua mensagem textual. A seleção de status
já tem nomes como Ativo e Pendente, de modo que a informação não depende
apenas da cor. Esse modo é uma opção de leitura, não uma simulação de tipos
de daltonismo nem uma garantia de atender a todas as necessidades visuais.

Para conferir: abra `/login`, clique em **Acessibilidade**, aumente para 150%,
ative **Alto contraste** e navegue para outra tela. Confira que os ajustes
permanecem. Use **Restaurar padrão** para desfazer as duas preferências.

## Imagens dos formulários

As imagens de login, cadastro de usuário e recuperação de senha dependiam
de URLs externas. Quando o navegador não consegue carregá-las, mostra a
descrição alternativa, como "Ilustração abstrata em gradiente".
Essas telas agora usam o logo local e a ilustração vetorial local
`static/institucional/assets/conexoes-comunitarias.svg`. Isso elimina a
dependência dos endereços externos dessas imagens.

## Filtros do gerenciamento

Os filtros da tela `/gerenciamento` agora operam sobre as linhas exibidas:

- Busca parcial por nome, sem diferenciar maiúsculas ou acentos.
- Busca por CPF com ou sem a pontuação.
- Status e serviço, combinados com os outros filtros.
- Faixa etária calculada pela data de nascimento e aniversário.
- Limpeza individual da busca ou de todos os filtros.
- Estado vazio quando nenhum registro corresponde, e simulação reversível.

O contador, os indicadores e o resumo agora refletem as seis linhas de
demonstração. O número fixo 84 e os botões de páginas inexistentes foram
removidos. O painel lateral inicia fechado, permitindo clicar nos filtros.
A tabela continua usando os registros de demonstração do projeto; essa
correção não integrou o painel ao banco de pessoas.

## 1. Acessibilidade dos formulários

O script compartilhado `src/main/resources/static/js/formularios.js` está
incluído nas 13 telas servidas pelo Spring Boot. Ele também prepara formulários
criados depois do carregamento, como o modal de exportação.

Ao tentar enviar um formulário inválido:

1. O envio é interrompido antes da chamada à API.
2. Uma mensagem explica o problema junto a cada campo inválido.
3. O campo recebe `aria-invalid="true"`.
4. `aria-describedby` conecta o campo à mensagem, mantendo as instruções
   existentes. Assim, o leitor de tela pode ler a ajuda e o erro.
5. Um resumo anuncia quantos campos precisam de revisão.
6. O foco vai para o primeiro campo inválido.

Ao editar um campo, sua marcação de erro é removida. Uma nova tentativa de envio
valida novamente os valores. Ao limpar o formulário, os erros desaparecem e as
instruções permanecem.

Os controles mantêm seus rótulos HTML e os campos obrigatórios recebem uma
instrução textual. No cadastro de usuário, adicionamos instruções para CPF,
telefone, data de nascimento, senha e código de convite. Também adicionamos
atributos de preenchimento automático e teclado numérico onde apropriado.

Além das regras nativas de e-mail, obrigatoriedade e limites numéricos, são
verificados:

- CPF com 11 números, com ou sem a máscara `000.000.000-00`.
- Telefone brasileiro com DDD e 10 ou 11 números.
- Data de nascimento no formato `dd/mm/aaaa`, existente e não futura.
- Nova senha com pelo menos 8 caracteres e no máximo 72 bytes, acompanhando
  o limite que já existe no servidor. Caracteres acentuados ocupam mais bytes.
- Confirmação igual à nova senha, inclusive quando a confirmação está vazia.

A verificação de CPF confere o formato; não verifica os dígitos de controle.
A verificação de e-mail confere a sintaxe; não verifica se a caixa postal existe.

As respostas da API já traziam erros por campo. Agora `acesso.js` preserva esse
mapa e o apresenta junto ao controle correspondente. Os aliases, por exemplo
`fullname` → `nomeCompleto`, permitem relacionar o HTML aos nomes da API.
Erros gerais, como falha de autenticação ou convite expirado, continuam como
mensagens do formulário quando o servidor não identifica um campo específico.

No cadastro de pessoas, removemos o erro de e-mail e a pendência que estavam
sempre visíveis como exemplos do layout. O botão agora aciona a validação.
Essa tela continua sendo uma prévia: após valores válidos, informa que o envio
ao servidor ainda não está disponível. A integração desse cadastro não fez
parte desta implementação.

As melhorias anteriores também permanecem: atalho para o conteúdo, destaque
do foco com Tab, respeito à preferência de movimento reduzido e identificação
do estado do botão de mostrar senha.

## 2. IA: sugestão de categoria de serviços

Na tela `/servicos`, ao cadastrar ou editar um serviço:

1. Preencha nome e, preferencialmente, descrição.
2. Clique em **Sugerir categoria com IA**.
3. Leia a categoria sugerida e os termos reconhecidos.
4. Clique em **Aplicar sugestão** se concordar.
5. Revise os campos e salve o serviço normalmente.

O primeiro botão não altera a categoria. Aplicar a sugestão só preenche o
campo; a gravação continua dependendo de salvar o formulário. Se o nome ou a
descrição mudar, a sugestão anterior é descartada. O resultado é anunciado em
uma região `role="status"`, também acessível por leitor de tela.

### Como o modelo funciona

Usamos **Multinomial Naive Bayes**, um algoritmo de aprendizado supervisionado
para classificação de texto. A implementação está em `ia-categorias.js`.

O conjunto de treinamento contém 16 textos fictícios, quatro por categoria:

| Categoria | Exemplo de conteúdo do treinamento |
| --- | --- |
| Educação | Reforço escolar, alfabetização, informática e estudos |
| Saúde | Consulta, vacinação, nutrição e acompanhamento psicológico |
| Assistência social | Cestas básicas, benefícios, roupas e acolhimento |
| Cultura e esporte | Música, dança, artes e atividades esportivas |

O treinamento ocorre quando o arquivo é carregado. Para cada categoria, o
algoritmo conta quantos exemplos existem e quantas vezes cada palavra aparece.
Antes dessa contagem, converte o texto para minúsculas, remove acentos e ignora
palavras frequentes como "de", "para" e "com".

Na classificação, o nome e a descrição formam o texto de entrada. A pontuação
de cada categoria é calculada por:

```text
log P(categoria) + soma de log P(palavra | categoria)

P(categoria) = exemplos da categoria / total de exemplos
P(palavra | categoria) = (ocorrências + 1) / (total de palavras da categoria + tamanho do vocabulário)
```

O `+1` é a suavização de Laplace: permite avaliar palavras que não apareceram
em uma categoria. O uso de logaritmos evita multiplicar muitos números pequenos.
Palavras desconhecidas em todo o treinamento são ignoradas.

O sistema evita sugerir quando há menos de dois termos diferentes conhecidos.
Também se abstém quando a pontuação normalizada da primeira categoria fica
abaixo de 0,5 ou a diferença para a segunda fica abaixo de 0,15. Esses limites
são escolhas de implementação; não foram calibrados com dados reais.
A pontuação interna não é apresentada como percentual de acerto.

### Exemplos para demonstrar na disciplina

| Nome ou descrição digitada | Resultado esperado |
| --- | --- |
| Reforço escolar de matemática e leitura | Educação |
| Campanha de vacinação com cuidados de enfermagem | Saúde |
| Doação de roupas para famílias em vulnerabilidade | Assistência social |
| Oficina de música, dança e teatro | Cultura e esporte |
| Futebol leitura | Solicitação de mais detalhes por ambiguidade |
| Texto vazio ou palavras desconhecidas | Sem sugestão |

Você pode explicar o recurso assim:

> Implementamos um classificador supervisionado de texto usando Multinomial
> Naive Bayes. Ele aprende frequências de palavras em exemplos rotulados e
> usa essas frequências para sugerir a categoria de um novo serviço. O usuário
> revisa o resultado antes de aplicá-lo.

Esse recurso permite demonstrar dados rotulados, pré-processamento de texto,
treinamento, inferência, probabilidades e tratamento de incerteza. Ele não é
um chatbot ou uma IA generativa, e não aprende automaticamente com os serviços
salvos. A adequação à avaliação depende dos requisitos definidos pelo professor.

### Limitações e evolução possível

Os 16 exemplos servem para uma demonstração didática. O vocabulário é pequeno,
não entende todos os sinônimos e pode errar textos com várias áreas. Os testes
usam frases separadas dos exemplos originais, mas não constituem uma medição de
qualidade em produção. Para evoluir, reúna exemplos revisados por categoria e
separe conjuntos de treinamento e avaliação antes de medir a taxa de acerto.

## 3. Como verificar

Inicie o projeto com JDK 21:

```powershell
.\mvnw.cmd spring-boot:run
```

Para os formulários, abra `/cadastro-usuario` e tente enviar vazio. Verifique
o foco e as mensagens. Experimente `31/02/2000` na data, um CPF curto e um
e-mail sem `@`. Corrija e confira que os erros desaparecem na nova validação.
Na redefinição de senha, informe senhas diferentes. Com um leitor de tela,
confira os rótulos, as instruções, o resumo e os erros associados.

Para a IA, entre como administrador ou operador, abra `/servicos` e use os
exemplos acima em **Novo serviço**. Confira que o campo categoria só muda após
**Aplicar sugestão** e que a gravação só ocorre após **Salvar serviço**.

Os testes de JavaScript usam Node.js e jsdom, instalado apenas em `target`,
sem adicionar dependências ao aplicativo:

```powershell
npm.cmd install --prefix target/frontend-checks --no-save --package-lock=false jsdom
node --test tests/frontend/acessibilidade-ia.test.cjs
```

Os testes verificam bloqueio de envio, foco, associação de mensagens, formatos,
datas, senhas, erros retornados pelo servidor, limpeza do formulário, modais
dinâmicos, rótulos das telas e classificação de textos. Também verificam que
a sugestão exige aplicação explícita e é descartada após editar o texto.

Para executar os testes existentes do backend:

```powershell
.\mvnw.cmd test
```

Os testes com DOM simulado não substituem a conferência visual e o uso real
de teclado e leitor de tela no navegador.

Na validação desta implementação, passaram os 14 testes de JavaScript e os
29 testes existentes do backend, sem falhas. Também conferimos no Chrome as
imagens locais, os controles de leitura, a persistência entre telas, os filtros
e o cadastro em uma tela de 390 pixels com texto em 150%, sem transbordamento
horizontal nem erros de JavaScript. A conferência com leitor de tela real
ainda precisa ser feita.
