/* Classificador supervisionado Multinomial Naive Bayes com suavização de Laplace. */
(function (root) {
    'use strict';
    const exemplos = [
        ['Educação', 'reforço escolar matemática português leitura alunos'],
        ['Educação', 'alfabetização adultos escrita leitura educação'],
        ['Educação', 'curso informática computadores tecnologia aprendizagem'],
        ['Educação', 'aulas apoio estudos escola ensino crianças'],
        ['Saúde', 'atendimento médico consulta prevenção saúde'],
        ['Saúde', 'acompanhamento psicológico terapia apoio emocional'],
        ['Saúde', 'orientação nutricional alimentação saudável nutrição'],
        ['Saúde', 'campanha vacinação cuidados enfermagem saúde'],
        ['Assistência social', 'distribuição cestas básicas alimentos famílias vulnerabilidade'],
        ['Assistência social', 'acolhimento social benefícios direitos cidadania'],
        ['Assistência social', 'doação roupas agasalhos apoio famílias'],
        ['Assistência social', 'orientação cadastro benefícios assistência social'],
        ['Cultura e esporte', 'oficina música dança teatro artes cultura'],
        ['Cultura e esporte', 'treino futebol esporte atividade física jovens'],
        ['Cultura e esporte', 'aulas pintura desenho artesanato oficina'],
        ['Cultura e esporte', 'prática vôlei basquete jogos recreação esporte']
    ];
    const ignorar = new Set('a o as os de da do das dos e em para com um uma no na nos nas por ao aos'.split(' '));
    function tokens(texto) {
        return String(texto || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .match(/[a-z]+/g)?.filter(word => word.length > 2 && !ignorar.has(word)) || [];
    }
    function treinar(dados) {
        const classes = new Map(), vocabulario = new Set();
        for (const [categoria, texto] of dados) {
            if (!classes.has(categoria)) classes.set(categoria, { documentos: 0, total: 0, palavras: new Map() });
            const classe = classes.get(categoria);
            classe.documentos++;
            for (const word of tokens(texto)) {
                vocabulario.add(word);
                classe.total++;
                classe.palavras.set(word, (classe.palavras.get(word) || 0) + 1);
            }
        }
        return { classes, vocabulario, documentos: dados.length };
    }
    const modelo = treinar(exemplos);
    function sugerir(texto) {
        const conhecidas = tokens(texto).filter(word => modelo.vocabulario.has(word));
        const termos = [...new Set(conhecidas)];
        if (termos.length < 2) return { categoria: null, motivo: 'Descreva melhor o serviço. Não há termos suficientes reconhecidos pelo modelo.' };
        const ranking = [...modelo.classes].map(([categoria, classe]) => {
            let score = Math.log(classe.documentos / modelo.documentos);
            for (const word of conhecidas) score += Math.log(((classe.palavras.get(word) || 0) + 1) / (classe.total + modelo.vocabulario.size));
            return { categoria, score };
        }).sort((a, b) => b.score - a.score);
        const max = ranking[0].score;
        const soma = ranking.reduce((total, item) => total + Math.exp(item.score - max), 0);
        ranking.forEach(item => item.pontuacao = Math.exp(item.score - max) / soma);
        if (ranking[0].pontuacao < 0.5 || ranking[0].pontuacao - ranking[1].pontuacao < 0.15)
            return { categoria: null, motivo: 'O texto mistura categorias ou é pouco específico. Acrescente detalhes ou escolha a categoria manualmente.' };
        const classe = modelo.classes.get(ranking[0].categoria);
        return {
            categoria: ranking[0].categoria,
            termos: termos.filter(word => classe.palavras.has(word)),
            pontuacao: ranking[0].pontuacao,
            exemplos: modelo.documentos
        };
    }
    const api = { sugerir, tokens, exemplos };
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    else root.iaCategorias = api;
})(typeof window !== 'undefined' ? window : globalThis);
