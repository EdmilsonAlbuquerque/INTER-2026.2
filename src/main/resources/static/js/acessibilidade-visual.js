/* Preferências de leitura locais: tamanho de texto e contraste. */
(() => {
    const storageKey = 'manancial-acessibilidade-visual';
    const sizes = [100, 115, 130, 150];
    let preferences = { size: 100, contrast: false };
    try {
        const saved = JSON.parse(localStorage.getItem(storageKey));
        if (saved) preferences = {
            size: sizes.includes(saved.size) ? saved.size : 100,
            contrast: saved.contrast === true
        };
    } catch { /* A página continua funcionando se o armazenamento estiver indisponível. */ }

    const launcher = document.createElement('button');
    launcher.type = 'button';
    launcher.className = 'acessibilidade-abrir';
    launcher.textContent = 'Acessibilidade';
    launcher.setAttribute('aria-haspopup', 'dialog');
    launcher.setAttribute('aria-controls', 'acessibilidade-visual');

    const dialog = document.createElement('dialog');
    dialog.id = 'acessibilidade-visual';
    dialog.className = 'acessibilidade-painel';
    dialog.setAttribute('aria-labelledby', 'acessibilidade-titulo');
    dialog.innerHTML = `
        <h2 id="acessibilidade-titulo">Ajustar a leitura</h2>
        <p>Aumente o texto ou mude as cores para facilitar a leitura.</p>
        <div class="acessibilidade-tamanho">
            <button type="button" data-diminuir aria-label="Diminuir o tamanho do texto">A−</button>
            <span data-tamanho role="status" aria-live="polite" aria-atomic="true"></span>
            <button type="button" data-aumentar aria-label="Aumentar o tamanho do texto">A+</button>
        </div>
        <button type="button" data-contraste aria-pressed="false">Alto contraste</button>
        <p>O alto contraste usa fundo preto, texto branco e links amarelos.</p>
        <p>Suas escolhas ficam salvas neste navegador.</p>
        <div class="acessibilidade-acoes">
            <button type="button" data-restaurar>Restaurar padrão</button>
            <button type="button" data-fechar>Fechar</button>
        </div>`;
    document.body.append(launcher, dialog);

    // Guarda estilos inline para poder restaurá-los sem perder a aparência original.
    const originalStyles = new Map();
    function resizeText() {
        for (const [element, styles] of originalStyles) {
            for (const [property, original] of Object.entries(styles)) {
                if (original.value) element.style.setProperty(property, original.value, original.priority);
                else element.style.removeProperty(property);
            }
        }
        originalStyles.clear();
        if (preferences.size === 100) return;
        const factor = preferences.size / 100;
        // Mede todos os textos antes de alterar qualquer um, evitando ampliação acumulada.
        const measurements = [...document.body.querySelectorAll('*')].filter(element => {
            if (element.closest('.acessibilidade-painel, .acessibilidade-abrir, svg, script, style, .material-symbols-outlined')) return false;
            return element.matches('input, select, textarea') || [...element.childNodes].some(node => node.nodeType === 3 && node.textContent.trim());
        }).map(element => {
            const computed = getComputedStyle(element);
            return { element, font: parseFloat(computed.fontSize), line: parseFloat(computed.lineHeight) };
        });
        for (const { element, font, line } of measurements) {
            if (!Number.isFinite(font)) continue;
            const properties = ['font-size', 'line-height'];
            originalStyles.set(element, Object.fromEntries(properties.map(property => [property, {
                value: element.style.getPropertyValue(property),
                priority: element.style.getPropertyPriority(property)
            }])));
            element.style.setProperty('font-size', `${font * factor}px`, 'important');
            element.style.setProperty('line-height', `${(Number.isFinite(line) ? line : font * 1.5) * factor}px`, 'important');
        }
    }
    const decrease = dialog.querySelector('[data-diminuir]');
    const increase = dialog.querySelector('[data-aumentar]');
    const contrast = dialog.querySelector('[data-contraste]');
    function apply(save = true) {
        document.documentElement.dataset.textScale = String(preferences.size);
        document.documentElement.dataset.contrast = preferences.contrast ? 'alto' : 'padrao';
        resizeText();
        decrease.disabled = preferences.size === sizes[0];
        increase.disabled = preferences.size === sizes.at(-1);
        contrast.setAttribute('aria-pressed', String(preferences.contrast));
        dialog.querySelector('[data-tamanho]').textContent = `Texto: ${preferences.size}%`;
        if (save) {
            try { localStorage.setItem(storageKey, JSON.stringify(preferences)); }
            catch { /* Os ajustes ainda valem para a página atual. */ }
        }
    }
    launcher.addEventListener('click', () => dialog.showModal());
    dialog.querySelector('[data-fechar]').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => launcher.focus());
    decrease.addEventListener('click', () => { preferences.size = sizes[Math.max(0, sizes.indexOf(preferences.size) - 1)]; apply(); });
    increase.addEventListener('click', () => { preferences.size = sizes[Math.min(sizes.length - 1, sizes.indexOf(preferences.size) + 1)]; apply(); });
    contrast.addEventListener('click', () => { preferences.contrast = !preferences.contrast; apply(); });
    dialog.querySelector('[data-restaurar]').addEventListener('click', () => { preferences = { size: 100, contrast: false }; apply(); });

    // Mantém o botão acima da barra fixa de ações do cadastro de pessoas.
    const bottomBar = document.querySelector('aside.fixed.bottom-0:not(.top-0)');
    if (bottomBar && typeof ResizeObserver !== 'undefined') {
        new ResizeObserver(() => {
            launcher.style.bottom = `${bottomBar.getBoundingClientRect().height + 16}px`;
        }).observe(bottomBar);
    }
    apply(false);
    let pending = false;
    let frame;
    const observer = new MutationObserver(records => {
        if (preferences.size === 100 || pending || !records.some(record => {
            const element = record.target.nodeType === 1 ? record.target : record.target.parentElement;
            return !element?.closest('.acessibilidade-painel, .acessibilidade-abrir, script, style');
        })) return;
        pending = true;
        frame = requestAnimationFrame(() => { pending = false; resizeText(); });
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
    window.addEventListener('pagehide', () => { observer.disconnect(); cancelAnimationFrame(frame); });
})();
