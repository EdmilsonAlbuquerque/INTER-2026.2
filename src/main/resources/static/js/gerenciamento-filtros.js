(() => {
    const body = document.getElementById('tableBody');
    const search = document.getElementById('searchInput');
    const status = document.getElementById('statusFilter');
    const service = document.getElementById('serviceFilter');
    const ages = [...document.querySelectorAll('[data-faixa-etaria]')];
    const clearSearch = document.getElementById('clearSearchBtn');
    let age = 'todos', simulateEmpty = false;
    const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
    const digits = value => String(value || '').replace(/\D/g, '');
    function ageOf(birth) {
        const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(birth || '');
        if (!match) return null;
        const [, day, month, year] = match.map(Number);
        const date = new Date(year, month - 1, day), now = new Date();
        if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || date > now) return null;
        return now.getFullYear() - year - (now.getMonth() < month - 1 || (now.getMonth() === month - 1 && now.getDate() < day) ? 1 : 0);
    }
    function matchesService(rowService, selected) {
        if (!selected) return true;
        const value = normalize(rowService), filter = normalize(selected);
        // Os exemplos usam tanto "Cestas Básicas & Apoio" quanto "Distribuição de Cestas".
        return filter === 'cestas basicas' ? value.includes('cestas') : value.includes(filter);
    }
    function render() {
        const rows = [...body.querySelectorAll('tr[data-name]')];
        const query = normalize(search.value), cpf = digits(search.value);
        let visible = 0;
        for (const row of rows) {
            const years = ageOf(row.dataset.birth);
            const matchSearch = !query || normalize(row.dataset.name).includes(query)
                || (cpf && !/[a-z]/i.test(query) && digits(row.dataset.cpf).includes(cpf));
            const matchAge = age === 'todos' || (years !== null && (age === 'menores' ? years < 18 : years >= 18));
            row.hidden = Boolean(simulateEmpty || !matchSearch || !matchAge
                || (status.value && normalize(row.dataset.status) !== normalize(status.value))
                || !matchesService(row.dataset.service, service.value));
            if (!row.hidden) visible++;
        }
        const table = document.getElementById('tableView'), empty = document.getElementById('emptyStateView');
        table.hidden = !visible;
        table.classList.toggle('hidden', !visible);
        empty.hidden = Boolean(visible);
        empty.classList.toggle('hidden', Boolean(visible));
        empty.classList.toggle('flex', !visible);
        clearSearch.hidden = !search.value;
        clearSearch.classList.toggle('hidden', !search.value);
        document.getElementById('resultsCounter').textContent = `${visible} de ${rows.length} pessoas encontradas`;
        const filters = [];
        if (search.value.trim()) filters.push(`Busca: ${search.value.trim()}`);
        if (status.value) filters.push(status.value);
        if (service.value) filters.push(service.options[service.selectedIndex].textContent);
        if (age !== 'todos') filters.push(age === 'menores' ? 'Menores de 18 anos' : '18 anos ou mais');
        if (simulateEmpty) filters.push('Simulação de lista vazia');
        document.getElementById('appliedFilters').textContent = filters.join(' • ') || 'Todos';
        document.getElementById('paginationSummary').textContent = `Mostrando ${visible} de ${rows.length} registros da tabela`;
        document.getElementById('toggleEmptyLabel').textContent = simulateEmpty ? 'Encerrar simulação' : 'Simular Busca Vazia';
        document.getElementById('simulateEmptyBtn').setAttribute('aria-pressed', String(simulateEmpty));
        for (const button of ages) button.setAttribute('aria-pressed', String(button.dataset.faixaEtaria === age));
        document.getElementById('metricTotal').textContent = rows.length;
        document.getElementById('metricActive').textContent = rows.filter(row => normalize(row.dataset.status) === 'ativo').length;
        document.getElementById('metricPending').textContent = rows.filter(row => normalize(row.dataset.status) === 'pendente').length;
        document.getElementById('metricMinor').textContent = rows.filter(row => { const years = ageOf(row.dataset.birth); return years !== null && years < 18; }).length;
    }
    const change = () => { simulateEmpty = false; render(); };
    window.resetAllFilters = () => {
        search.value = ''; status.value = ''; service.value = ''; age = 'todos'; simulateEmpty = false;
        render(); search.focus();
    };
    window.toggleEmptyState = () => { simulateEmpty = !simulateEmpty; render(); };
    search.addEventListener('input', change);
    status.addEventListener('change', change);
    service.addEventListener('change', change);
    ages.forEach(button => button.addEventListener('click', () => { age = button.dataset.faixaEtaria; change(); }));
    clearSearch.addEventListener('click', () => { search.value = ''; change(); search.focus(); });
    document.getElementById('resetFiltersBtn').addEventListener('click', window.resetAllFilters);
    // As edições feitas na prévia atualizam os filtros e os totais imediatamente.
    const observer = new MutationObserver(render);
    observer.observe(body, { childList: true, subtree: true, attributes: true, attributeFilter: ['data-name', 'data-cpf', 'data-status', 'data-service', 'data-birth'] });
    window.addEventListener('pagehide', () => observer.disconnect());
    render();
})();
