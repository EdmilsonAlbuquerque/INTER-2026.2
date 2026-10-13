/* Validação acessível compartilhada, sem substituir a validação do servidor. */
(() => {
    let sequence = 0;
    const controls = form => [...form.querySelectorAll('input, select, textarea')]
        .filter(field => !['hidden', 'submit', 'button', 'reset', 'file'].includes(field.type));
    function describe(field, id, add) {
        const ids = new Set((field.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
        if (add) ids.add(id); else ids.delete(id);
        if (ids.size) field.setAttribute('aria-describedby', [...ids].join(' '));
        else field.removeAttribute('aria-describedby');
    }
    function clearField(field) {
        const id = `${field.id}-erro`;
        document.getElementById(id)?.remove();
        describe(field, id, false);
        field.removeAttribute('aria-invalid');
    }
    function fieldError(field, text) {
        if (!field.id) field.id = `campo-acessivel-${++sequence}`;
        let error = document.getElementById(`${field.id}-erro`);
        if (!error) {
            error = document.createElement('span');
            error.id = `${field.id}-erro`;
            error.className = 'campo-erro';
            // Fora do label, para que o erro não passe a fazer parte do nome do campo.
            const anchor = field.closest('label') || (field.parentElement.classList.contains('relative') ? field.parentElement : field);
            anchor.after(error);
        }
        error.textContent = text;
        field.setAttribute('aria-invalid', 'true');
        describe(field, error.id, true);
    }
    function customError(field) {
        const value = field.value.trim();
        if (field.required && !value && !['checkbox', 'radio'].includes(field.type)) return 'Preencha este campo.';
        if (field.dataset.matches) {
            const original = field.form.querySelector(field.dataset.matches);
            if (original && field.value !== original.value) return 'As senhas precisam ser iguais.';
        }
        if (!value) return '';
        if (field.dataset.format === 'cpf' && !/^(\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/.test(value))
            return 'Informe os 11 números do CPF, com ou sem pontos e traço.';
        if (field.dataset.format === 'telefone' && !/^(\d{10,11}|\(\d{2}\)\s?\d{4,5}-\d{4})$/.test(value))
            return 'Informe o DDD e o telefone, com 10 ou 11 números.';
        if (field.dataset.format === 'nascimento') {
            const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
            if (!match) return 'Informe a data no formato dd/mm/aaaa.';
            const [, day, month, year] = match.map(Number);
            const date = new Date(year, month - 1, day);
            if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || date > new Date())
                return 'Informe uma data de nascimento existente e que não seja futura.';
        }
        if (field.dataset.password === 'nova' && (field.value.length < 8 || new TextEncoder().encode(field.value).length > 72))
            return 'Use pelo menos 8 caracteres e no máximo 72 bytes; acentos ocupam mais de um byte.';
        return '';
    }
    function validationMessage(field) {
        const custom = customError(field);
        if (custom) return custom;
        const v = field.validity;
        if (v.valid) return '';
        if (v.valueMissing) return field.type === 'checkbox' ? 'Marque esta opção para continuar.' : 'Preencha ou selecione este campo.';
        if (v.typeMismatch && field.type === 'email') return 'Informe um e-mail válido, como nome@exemplo.com.';
        if (v.tooShort) return `Use pelo menos ${field.minLength} caracteres.`;
        if (v.tooLong) return `Use no máximo ${field.maxLength} caracteres.`;
        if (v.rangeUnderflow) return `Informe um valor maior ou igual a ${field.min}.`;
        if (v.rangeOverflow) return `Informe um valor menor ou igual a ${field.max}.`;
        return field.validationMessage || 'Confira o valor informado.';
    }
    function summary(form, count) {
        let box = form.querySelector('[data-validation-summary]');
        if (!box) {
            box = document.createElement('p');
            box.dataset.validationSummary = '';
            box.className = 'campo-erro';
            box.setAttribute('role', 'alert');
            box.setAttribute('aria-atomic', 'true');
            form.prepend(box);
        }
        box.textContent = `Revise ${count} campo(s) indicado(s) antes de continuar.`;
    }
    function clear(form) {
        controls(form).forEach(clearField);
        form.querySelector('[data-validation-summary]')?.remove();
    }
    function validate(form) {
        clear(form);
        const invalid = controls(form).filter(field => {
            if (!field.willValidate) return false;
            const text = validationMessage(field);
            if (text) fieldError(field, text);
            return Boolean(text);
        });
        if (invalid.length) { summary(form, invalid.length); invalid[0].focus(); }
        return !invalid.length;
    }
    function serverErrors(form, errors = {}) {
        let first;
        let count = 0;
        for (const [name, text] of Object.entries(errors)) {
            const field = controls(form).find(f => f.dataset.apiField === name || f.name === name || f.id === name);
            if (field && field.willValidate) { fieldError(field, text); first ||= field; count++; }
        }
        if (first) { summary(form, count); first.focus(); }
    }
    function prepare(form) {
        if (form.dataset.accessibleValidation) return;
        form.dataset.accessibleValidation = 'true';
        form.noValidate = true;
        controls(form).forEach(field => {
            if (!field.id) field.id = `campo-acessivel-${++sequence}`;
            // Mantém labels explícitos e implícitos existentes.
            if (!field.labels?.length && !field.hasAttribute('aria-label') && !field.hasAttribute('aria-labelledby')) {
                console.warn('Campo sem rótulo:', field.id);
            }
            const hint = field.dataset.hint;
            if (hint || field.required) {
                const help = document.createElement('span');
                help.id = `${field.id}-ajuda`;
                help.className = 'campo-ajuda';
                help.textContent = (field.required ? 'Campo obrigatório. ' : '') + (hint || '');
                const anchor = field.closest('label') || (field.parentElement.classList.contains('relative') ? field.parentElement : field);
                anchor.after(help);
                describe(field, help.id, true);
            }
        });
        form.addEventListener('submit', event => {
            if (!validate(form)) { event.preventDefault(); event.stopImmediatePropagation(); }
        }, true);
        form.addEventListener('input', event => {
            const field = event.target;
            if (!field.matches('input, select, textarea')) return;
            clearField(field);
            controls(form).filter(other => other.dataset.matches && form.querySelector(other.dataset.matches) === field).forEach(clearField);
            if (!form.querySelector('[aria-invalid="true"]')) form.querySelector('[data-validation-summary]')?.remove();
        });
        form.addEventListener('reset', () => clear(form));
    }
    window.formularios = { validate, serverErrors, clear, fieldError };
    document.querySelectorAll('form').forEach(prepare);
    // Inclui formulários de modais criados dinamicamente.
    new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => {
        if (node.nodeType !== 1) return;
        if (node.matches('form')) prepare(node);
        node.querySelectorAll('form').forEach(prepare);
    }))).observe(document.body, { childList: true, subtree: true });
})();
