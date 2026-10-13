(() => {
    const form = document.getElementById('servico-form');
    const suggest = document.getElementById('sugerir-categoria');
    const apply = document.getElementById('aplicar-categoria');
    const feedback = document.getElementById('ia-resultado');
    let categoria;
    function clear() {
        categoria = null;
        feedback.textContent = '';
        apply.hidden = true;
    }
    suggest.addEventListener('click', () => {
        const result = window.iaCategorias.sugerir(`${form.elements.nome.value} ${form.elements.descricao.value}`);
        categoria = result.categoria;
        apply.hidden = !categoria;
        feedback.textContent = categoria
            ? `Categoria sugerida: ${categoria}. Termos reconhecidos: ${result.termos.join(', ')}. Revise antes de aplicar.`
            : result.motivo;
    });
    apply.addEventListener('click', () => {
        if (!categoria) return;
        form.elements.categoria.value = categoria;
        form.elements.categoria.dispatchEvent(new Event('input', { bubbles: true }));
        feedback.textContent = `Categoria ${categoria} aplicada ao campo. Você pode editá-la antes de salvar.`;
        apply.hidden = true;
        form.elements.categoria.focus();
    });
    for (const name of ['nome', 'descricao']) form.elements[name].addEventListener('input', clear);
    form.addEventListener('reset', clear);
})();
