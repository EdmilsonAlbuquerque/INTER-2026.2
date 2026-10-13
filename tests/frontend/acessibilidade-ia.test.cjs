const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('../../target/frontend-checks/node_modules/jsdom');
const root = path.resolve(__dirname, '../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const ia = require('../../src/main/resources/static/js/ia-categorias.js');

function page(html) {
    const dom = new JSDOM(html, { runScripts: 'outside-only', url: 'http://localhost/login' });
    dom.window.TextEncoder = TextEncoder;
    dom.window.eval(read('src/main/resources/static/js/formularios.js'));
    return dom;
}

test('envio inválido é bloqueado, erro é associado ao campo e foco vai para o primeiro erro', () => {
    const dom = page('<form><label for="email">E-mail</label><input id="email" name="email" type="email" required data-hint="E-mail institucional."><button>Enviar</button></form>');
    const { document, Event } = dom.window;
    const form = document.querySelector('form'), field = document.querySelector('input');
    let sent = false;
    form.addEventListener('submit', () => sent = true);
    assert.equal(form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })), false);
    assert.equal(sent, false);
    assert.equal(field.getAttribute('aria-invalid'), 'true');
    assert.equal(document.activeElement, field);
    assert.match(field.getAttribute('aria-describedby'), /email-ajuda/);
    assert.match(field.getAttribute('aria-describedby'), /email-erro/);
    field.value = 'invalido';
    dom.window.formularios.validate(form);
    assert.match(document.getElementById('email-erro').textContent, /e-mail válido/);
    field.value = 'pessoa@exemplo.com';
    field.dispatchEvent(new Event('input', { bubbles: true }));
    assert.equal(field.hasAttribute('aria-invalid'), false);
    assert.equal(field.getAttribute('aria-describedby'), 'email-ajuda');
    assert.equal(form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })), true);
    assert.equal(sent, true);
    dom.window.close();
});

test('formatos brasileiros, datas impossíveis, senha em bytes e confirmação vazia', () => {
    const dom = page('<form><label>CPF<input id="cpf" data-format="cpf"></label><label>Telefone<input id="phone" data-format="telefone"></label><label>Data<input id="birth" data-format="nascimento"></label><label>Senha<input id="password" data-password="nova"></label><label>Confirmar<input id="confirm" data-matches="#password"></label></form>');
    const form = dom.window.document.querySelector('form');
    const field = id => dom.window.document.getElementById(id);
    field('cpf').value = '123'; field('phone').value = 'abc'; field('birth').value = '31/02/2000';
    assert.equal(dom.window.formularios.validate(form), false);
    assert.equal(form.querySelectorAll('[aria-invalid="true"]').length, 3);
    field('cpf').value = '123.456.789-01'; field('phone').value = '(85) 99999-9999'; field('birth').value = '29/02/2000';
    field('password').value = 'Senha123';
    assert.equal(dom.window.formularios.validate(form), false);
    assert.equal(field('confirm').getAttribute('aria-invalid'), 'true');
    field('confirm').value = 'Senha123';
    assert.equal(dom.window.formularios.validate(form), true);
    field('password').value = 'á'.repeat(37); field('confirm').value = field('password').value;
    assert.equal(dom.window.formularios.validate(form), false);
    assert.match(field('password').getAttribute('aria-describedby'), /password-erro/);
    field('birth').value = '01/01/2999';
    assert.equal(dom.window.formularios.validate(form), false);
    assert.equal(field('birth').getAttribute('aria-invalid'), 'true');
    dom.window.close();
});

test('erros do servidor usam os aliases e o reset remove erros preservando instruções', () => {
    const dom = page('<form><label>Nome<input id="fullname" data-api-field="nomeCompleto" data-hint="Nome civil."></label></form>');
    const form = dom.window.document.querySelector('form'), input = form.querySelector('input');
    dom.window.formularios.serverErrors(form, { nomeCompleto: 'Nome já existente.', token: 'Token inválido.' });
    assert.equal(dom.window.document.activeElement, input);
    assert.equal(input.getAttribute('aria-invalid'), 'true');
    assert.equal(dom.window.document.getElementById('fullname-erro').textContent, 'Nome já existente.');
    form.reset();
    assert.equal(input.hasAttribute('aria-invalid'), false);
    assert.equal(input.getAttribute('aria-describedby'), 'fullname-ajuda');
    dom.window.close();
});

test('formulários inseridos em modais também recebem validação', async () => {
    const dom = page('<main></main>');
    dom.window.document.body.insertAdjacentHTML('beforeend', '<dialog><form><label>Nome<input required></label></form></dialog>');
    await new Promise(resolve => dom.window.setTimeout(resolve, 0));
    const form = dom.window.document.querySelector('form');
    assert.equal(form.noValidate, true);
    assert.equal(dom.window.formularios.validate(form), false);
    dom.window.close();
});

test('templates têm destino focável, script compartilhado e controles de formulário com rótulos', () => {
    const dir = path.join(root, 'src/main/resources/templates');
    for (const file of fs.readdirSync(dir).filter(name => name.endsWith('.html'))) {
        const dom = new JSDOM(read(`src/main/resources/templates/${file}`));
        const doc = dom.window.document;
        const main = doc.querySelector('main');
        assert.equal(main.getAttribute('tabindex'), '-1', file);
        assert.equal(doc.querySelector('.skip-link').getAttribute('href'), `#${main.id}`, file);
        assert.ok(doc.querySelector('script[src="/js/formularios.js"]'), file);
        for (const field of doc.querySelectorAll('form input, form select, form textarea')) {
            if (['hidden', 'submit', 'button', 'file'].includes(field.type)) continue;
            assert.ok(field.labels.length || field.hasAttribute('aria-label') || field.hasAttribute('aria-labelledby'), `${file}: ${field.name || field.id}`);
        }
        dom.window.close();
    }
});

test('IA classifica exemplos de avaliação das quatro categorias e normaliza acentos', () => {
    const casos = [
        ['Aulas de leitura e matemática para alunos', 'Educação'],
        ['Campanha de vacinação com cuidados de enfermagem', 'Saúde'],
        ['Doação de roupas para famílias em vulnerabilidade', 'Assistência social'],
        ['Treino de futebol e basquete para jovens', 'Cultura e esporte']
    ];
    for (const [texto, categoria] of casos) assert.equal(ia.sugerir(texto).categoria, categoria);
    assert.equal(ia.sugerir('OFICINA MÚSICA DANÇA').categoria, ia.sugerir('oficina musica danca').categoria);
});

test('IA se abstém em texto vazio, desconhecido, curto ou ambíguo', () => {
    for (const texto of ['', 'xyz foguetes interplanetários', 'futebol', 'futebol leitura']) {
        assert.equal(ia.sugerir(texto).categoria, null, texto);
    }
});

test('sugestão não altera categoria sem aplicar e é descartada se o texto muda', () => {
    const dom = page(read('src/main/resources/templates/servicos.html'));
    dom.window.eval(read('src/main/resources/static/js/ia-categorias.js'));
    dom.window.eval(read('src/main/resources/static/js/ia-servicos.js'));
    const doc = dom.window.document, form = doc.getElementById('servico-form');
    form.elements.nome.value = 'Reforço escolar de matemática e leitura';
    form.elements.categoria.value = 'Categoria original';
    doc.getElementById('sugerir-categoria').click();
    assert.equal(form.elements.categoria.value, 'Categoria original');
    assert.equal(doc.getElementById('aplicar-categoria').hidden, false);
    doc.getElementById('aplicar-categoria').click();
    assert.equal(form.elements.categoria.value, 'Educação');
    doc.getElementById('sugerir-categoria').click();
    form.elements.nome.dispatchEvent(new dom.window.Event('input', { bubbles: true }));
    assert.equal(doc.getElementById('aplicar-categoria').hidden, true);
    assert.equal(doc.getElementById('ia-resultado').textContent, '');
    form.reset();
    assert.equal(doc.getElementById('aplicar-categoria').hidden, true);
    dom.window.close();
});

function visualPage(saved) {
    const dom = new JSDOM('<body><p id="texto" style="font-size: 16px; line-height: 24px">Texto de leitura</p><span class="material-symbols-outlined" style="font-size: 20px">person</span></body>', {
        runScripts: 'outside-only', url: 'http://localhost/login', pretendToBeVisual: true
    });
    if (saved !== undefined) dom.window.localStorage.setItem('manancial-acessibilidade-visual', JSON.stringify(saved));
    dom.window.HTMLDialogElement.prototype.showModal = function () { this.open = true; };
    dom.window.HTMLDialogElement.prototype.close = function () { this.open = false; this.dispatchEvent(new dom.window.Event('close')); };
    dom.window.eval(read('src/main/resources/static/js/acessibilidade-visual.js'));
    const close = dom.window.close.bind(dom.window);
    dom.window.close = () => { dom.window.dispatchEvent(new dom.window.Event('pagehide')); close(); };
    return dom;
}

test('leitura aumenta até 150%, preserva ícones e restaura estilos sem acumular ampliação', () => {
    const dom = visualPage();
    const doc = dom.window.document, text = doc.getElementById('texto');
    const increase = doc.querySelector('[data-aumentar]'), decrease = doc.querySelector('[data-diminuir]');
    assert.equal(decrease.disabled, true);
    increase.click();
    assert.equal(text.style.fontSize, '18.4px');
    increase.click();
    assert.equal(text.style.fontSize, '20.8px');
    increase.click(); increase.click();
    assert.equal(text.style.fontSize, '24px');
    assert.equal(text.style.lineHeight, '36px');
    assert.equal(increase.disabled, true);
    assert.equal(doc.querySelector('.material-symbols-outlined').style.fontSize, '20px');
    doc.querySelector('[data-restaurar]').click();
    assert.equal(text.style.fontSize, '16px');
    assert.equal(text.style.lineHeight, '24px');
    assert.equal(doc.documentElement.dataset.textScale, '100');
    dom.window.close();
});

test('contraste e texto são persistidos, preferências inválidas são ignoradas e fechar devolve foco', () => {
    const dom = visualPage({ size: 130, contrast: true });
    const doc = dom.window.document;
    assert.equal(doc.documentElement.dataset.contrast, 'alto');
    assert.equal(doc.getElementById('texto').style.fontSize, '20.8px');
    doc.querySelector('[data-contraste]').click();
    assert.equal(doc.documentElement.dataset.contrast, 'padrao');
    assert.equal(doc.querySelector('[data-contraste]').getAttribute('aria-pressed'), 'false');
    assert.deepEqual(JSON.parse(dom.window.localStorage.getItem('manancial-acessibilidade-visual')), { size: 130, contrast: false });
    doc.querySelector('.acessibilidade-abrir').click();
    assert.equal(doc.querySelector('dialog').open, true);
    doc.querySelector('[data-fechar]').click();
    assert.equal(doc.querySelector('dialog').open, false);
    assert.equal(doc.activeElement, doc.querySelector('.acessibilidade-abrir'));
    dom.window.close();
    const invalid = visualPage({ size: 999, contrast: 'true' });
    assert.equal(invalid.window.document.documentElement.dataset.textScale, '100');
    assert.equal(invalid.window.document.documentElement.dataset.contrast, 'padrao');
    invalid.window.close();
});

test('novos textos recebem a ampliação sem aumentar novamente os existentes', async () => {
    const dom = visualPage({ size: 150, contrast: false });
    const doc = dom.window.document;
    doc.body.insertAdjacentHTML('beforeend', '<p id="novo" style="font-size: 12px">Mensagem carregada depois</p>');
    await new Promise(resolve => dom.window.setTimeout(resolve, 60));
    assert.equal(doc.getElementById('novo').style.fontSize, '18px');
    assert.equal(doc.getElementById('texto').style.fontSize, '24px');
    dom.window.close();
});

test('as imagens dos formulários usam arquivos locais existentes e todas as telas oferecem ajustes visuais', () => {
    const dir = path.join(root, 'src/main/resources/templates');
    for (const file of fs.readdirSync(dir).filter(name => name.endsWith('.html'))) {
        const dom = new JSDOM(read(`src/main/resources/templates/${file}`));
        assert.ok(dom.window.document.querySelector('script[src="/js/acessibilidade-visual.js"]'), file);
        if (['login.html', 'cadastro-usuario.html', 'recuperar-senha.html'].includes(file)) {
            for (const img of dom.window.document.querySelectorAll('img')) {
                const src = img.getAttribute('src');
                assert.ok(src.startsWith('/institucional/'), `${file}: ${src}`);
                assert.ok(fs.existsSync(path.join(root, 'src/main/resources/static', src)), `${file}: ${src}`);
            }
        }
        dom.window.close();
    }
});

function managementPage() {
    const dom = new JSDOM(read('src/main/resources/templates/gerenciamento.html'), { runScripts: 'outside-only', url: 'http://localhost/gerenciamento' });
    dom.window.Date = class extends Date { constructor(...args) { super(...(args.length ? args : ['2026-10-12T12:00:00-03:00'])); } };
    // Executa os scripts internos para detectar conflitos com a lógica do layout.
    for (const script of dom.window.document.querySelectorAll('script:not([src])')) {
        if (!script.textContent.includes('tailwind.config')) dom.window.eval(script.textContent);
    }
    dom.window.eval(read('src/main/resources/static/js/gerenciamento-filtros.js'));
    const close = dom.window.close.bind(dom.window);
    dom.window.close = () => { dom.window.dispatchEvent(new dom.window.Event('pagehide')); close(); };
    return dom;
}
const visibleNames = doc => [...doc.querySelectorAll('#tableBody tr[data-name]')].filter(row => !row.hidden).map(row => row.dataset.name);

test('gerenciamento busca nomes sem acentos e CPF com ou sem máscara, combinando filtros', () => {
    const dom = managementPage(), doc = dom.window.document;
    const search = doc.getElementById('searchInput'), status = doc.getElementById('statusFilter'), service = doc.getElementById('serviceFilter');
    assert.equal(visibleNames(doc).length, 6);
    assert.equal(doc.getElementById('metricTotal').textContent, '6');
    assert.ok(doc.getElementById('drawerBackdrop').classList.contains('hidden'));
    search.value = 'LUCAS GABRIEL'; search.dispatchEvent(new dom.window.Event('input'));
    assert.equal(visibleNames(doc).length, 1);
    search.value = '45288910812'; search.dispatchEvent(new dom.window.Event('input'));
    assert.equal(visibleNames(doc).length, 1);
    search.value = '452.889.108-12'; search.dispatchEvent(new dom.window.Event('input'));
    assert.equal(visibleNames(doc).length, 1);
    dom.window.resetAllFilters();
    service.value = 'Cestas Básicas'; service.dispatchEvent(new dom.window.Event('change'));
    assert.equal(visibleNames(doc).length, 2);
    status.value = 'Ativo'; status.dispatchEvent(new dom.window.Event('change'));
    assert.deepEqual(visibleNames(doc), ['Maria Clara Ferreira dos Santos']);
    assert.match(doc.getElementById('appliedFilters').textContent, /Ativo/);
    dom.window.resetAllFilters();
    search.value = 'zzzz'; search.dispatchEvent(new dom.window.Event('input'));
    assert.equal(doc.getElementById('emptyStateView').hidden, false);
    assert.equal(doc.getElementById('tableView').hidden, true);
    dom.window.resetAllFilters();
    assert.equal(visibleNames(doc).length, 6);
    assert.equal(doc.getElementById('emptyStateView').hidden, true);
    dom.window.close();
});

test('faixa etária usa aniversário e data existente; limpar e simular lista vazia funcionam', async () => {
    const dom = managementPage(), doc = dom.window.document;
    const rows = doc.querySelectorAll('#tableBody tr');
    rows[0].dataset.birth = '13/10/2008'; // Ainda tem 17 anos na data do teste.
    rows[1].dataset.birth = '12/10/2008'; // Completa 18 hoje.
    rows[2].dataset.birth = '31/02/2008'; // Data inválida não entra nas faixas.
    await new Promise(resolve => dom.window.setTimeout(resolve, 0));
    doc.querySelector('[data-faixa-etaria="menores"]').click();
    assert.deepEqual(visibleNames(doc), ['Lucas Gabriel Martins Ribeiro']);
    assert.equal(doc.querySelector('[data-faixa-etaria="menores"]').getAttribute('aria-pressed'), 'true');
    doc.querySelector('[data-faixa-etaria="adultos"]').click();
    assert.equal(visibleNames(doc).length, 4);
    assert.ok(visibleNames(doc).includes('Maria Clara Ferreira dos Santos'));
    dom.window.toggleEmptyState();
    assert.equal(visibleNames(doc).length, 0);
    assert.equal(doc.getElementById('simulateEmptyBtn').getAttribute('aria-pressed'), 'true');
    dom.window.toggleEmptyState();
    assert.equal(visibleNames(doc).length, 4);
    dom.window.resetAllFilters();
    assert.equal(visibleNames(doc).length, 6);
    assert.equal(doc.getElementById('searchInput').value, '');
    assert.equal(doc.querySelector('[data-faixa-etaria="todos"]').getAttribute('aria-pressed'), 'true');
    dom.window.close();
});
