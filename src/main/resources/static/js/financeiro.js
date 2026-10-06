(() => {
    const {api,message,submit}=window.acesso;
    const filtros=document.getElementById('filtros'), form=document.getElementById('lancamento'), modal=document.getElementById('editar');
    const feedback=document.getElementById('feedback');
    const moeda=value=>Number(value).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
    let itens=[], request=0, catalogo=[], historicoTipo='RECEITA';
    const historico=document.getElementById('historico-modal'), categoriaModal=document.getElementById('categorias-modal'), categoriaForm=document.getElementById('categoria-form');
    filtros.elements.ano.value=new Date().getFullYear();
    const params=()=>new URLSearchParams([...new FormData(filtros)].filter(([,v])=>v.trim()));
    async function editar(item={}) {
        try { await carregarCatalogo(); } catch(e) {message(feedback,e.message,true);return;}
        historico.close();
        form.reset();form.querySelector('[data-feedback]')?.remove();
        form.elements.id.value=item.id||'';
        form.elements.data.value=item.data||new Date().toLocaleDateString('en-CA');
        for(const key of ['tipo','descricao','categoria','valor']) if(item[key]!=null) form.elements[key].value=item[key];
        modal.showModal();
    }
    async function carregar() {
        const seq=++request;
        try {
            const data=await api(`/api/financeiro?${params()}`);
            if(seq!==request)return;
            itens=data.lancamentos;
            for(const key of ['receitas','despesas','saldo'])document.getElementById(key).textContent=moeda(data[key]);
            document.getElementById('exemplos').hidden=!itens.some(i=>i.exemplo);
            recentes();renderHistorico();
            categorias();grafico();
        }catch(e){message(feedback,e.message,true);}
    }
    const dataFormatada=value=>value.split('-').reverse().join('/');
    function botao(label,action){const button=document.createElement('button');button.type='button';button.className='financeiro-link';button.textContent=label;button.onclick=action;return button;}
    function recentes(){
        for(const [tipo,id] of [['RECEITA','entradas-recentes'],['DESPESA','despesas-recentes']]){
            const root=document.getElementById(id);root.replaceChildren();
            const lista=itens.filter(i=>i.tipo===tipo).sort((a,b)=>b.data.localeCompare(a.data)||b.id-a.id).slice(0,3);
            for(const item of lista){
                const row=document.createElement('div'),details=document.createElement('div'),name=document.createElement('p'),date=document.createElement('p'),values=document.createElement('div'),value=document.createElement('strong'),badge=document.createElement('span');
                row.className='financeiro-recent-row';name.className='financeiro-recent-name';name.textContent=item.descricao;
                date.className='financeiro-recent-date';date.textContent=dataFormatada(item.data)+' • '+item.categoria;
                values.className='financeiro-recent-values';value.textContent=(tipo==='RECEITA'?'+ ':'− ')+moeda(item.valor);
                badge.className='financeiro-badge '+(tipo==='RECEITA'?'income':'expense');badge.textContent=item.exemplo?'Demonstração':'Registrado';
                details.append(name,date);values.append(value,badge);row.append(details,values);root.append(row);
            }
            if(!lista.length){const empty=document.createElement('p');empty.className='financeiro-empty';empty.textContent=tipo==='RECEITA'?'Nenhuma entrada nos filtros selecionados.':'Nenhuma despesa nos filtros selecionados.';root.append(empty);}
        }
    }
    function listaHistorico(){
        const busca=document.getElementById('historico-busca').value.trim().toLocaleLowerCase('pt-BR');
        return itens.filter(i=>i.tipo===historicoTipo&&(i.descricao+' '+i.categoria).toLocaleLowerCase('pt-BR').includes(busca));
    }
    function renderHistorico(){
        document.getElementById('historico-titulo').textContent=historicoTipo==='RECEITA'?'Histórico de entradas':'Histórico de despesas';
        const lista=listaHistorico(),body=document.getElementById('lancamentos');body.replaceChildren();
        document.getElementById('quantidade').textContent=lista.length+' lançamento(s) nos filtros selecionados';
        document.getElementById('historico-total').textContent='Total: '+moeda(lista.reduce((sum,item)=>sum+Number(item.valor),0));
        for(const item of lista){
            const row=document.createElement('tr');row.className='border-t';
            for(const text of [dataFormatada(item.data),item.descricao,item.categoria,item.tipo==='RECEITA'?'Receita':'Despesa',moeda(item.valor),item.exemplo?'Demonstração':'Real']){const cell=document.createElement('td');cell.className='py-4 pr-4';cell.textContent=text;row.append(cell);}
            const actions=document.createElement('td');actions.className='financeiro-history-actions';
            actions.append(botao('Editar',()=>editar(item)),botao('Excluir',async()=>{
                if(!confirm('Excluir este lançamento?'))return;
                try{await api('/api/financeiro/'+item.id,{method:'DELETE'});await carregar();}catch(e){message(historico,e.message,true);}
            }));row.append(actions);body.append(row);
        }
        if(!lista.length){const cell=body.insertRow().insertCell();cell.colSpan=7;cell.textContent='Nenhum lançamento encontrado.';cell.className='py-6';}
    }
    async function carregarCatalogo(){
        catalogo=await api('/api/financeiro/categorias');
        const select=form.elements.categoria,selected=select.value;select.replaceChildren(new Option('Selecione uma categoria',''));
        for(const categoria of catalogo)select.append(new Option(categoria.nome,categoria.nome));
        select.value=selected;
    }
    function limparCategoria(){categoriaForm.reset();categoriaForm.elements.id.value='';document.getElementById('cancelar-categoria').hidden=true;}
    function renderCatalogo(){
        const root=document.getElementById('categorias-lista');root.replaceChildren();
        for(const categoria of catalogo){
            const row=document.createElement('div'),name=document.createElement('span'),actions=document.createElement('div');row.className='financeiro-category-management-row';name.textContent=categoria.nome;
            actions.append(botao('Editar',()=>{categoriaForm.elements.id.value=categoria.id;categoriaForm.elements.nome.value=categoria.nome;document.getElementById('cancelar-categoria').hidden=false;categoriaForm.elements.nome.focus();}),botao('Remover',async()=>{
                if(!confirm('Remover a categoria '+categoria.nome+'?'))return;
                try{await api('/api/financeiro/categorias/'+categoria.id,{method:'DELETE'});if(categoriaForm.elements.id.value===String(categoria.id))limparCategoria();await carregarCatalogo();renderCatalogo();message(document.getElementById('categoria-feedback'),'Categoria removida.');}catch(e){message(document.getElementById('categoria-feedback'),e.message,true);}
            }));row.append(name,actions);root.append(row);
        }
        if(!catalogo.length)root.textContent='Nenhuma categoria cadastrada.';
    }
    document.getElementById('gerenciar-categorias').onclick=async()=>{
        limparCategoria();categoriaForm.querySelector('[data-feedback]')?.remove();document.getElementById('categoria-feedback').replaceChildren();categoriaModal.showModal();
        try{await carregarCatalogo();renderCatalogo();}catch(e){message(document.getElementById('categoria-feedback'),e.message,true);}
    };
    document.getElementById('cancelar-categoria').onclick=limparCategoria;
    submit(categoriaForm,async fields=>{
        const id=fields.get('id');await api('/api/financeiro/categorias'+(id?'/'+id:''),{method:id?'PUT':'POST',body:JSON.stringify({nome:fields.get('nome')})});
        limparCategoria();await carregarCatalogo();renderCatalogo();await carregar();message(document.getElementById('categoria-feedback'),'Categoria salva.');
    });
    document.querySelectorAll('[data-fechar-dialog]').forEach(button=>button.onclick=()=>document.getElementById(button.dataset.fecharDialog).close());
    document.querySelectorAll('[data-historico]').forEach(button=>button.onclick=()=>{historicoTipo=button.dataset.historico;document.getElementById('historico-busca').value='';renderHistorico();historico.showModal();});
    document.getElementById('historico-busca').oninput=renderHistorico;
    document.getElementById('historico-exportar').onclick=()=>{
        const query=params();query.set('tipo',historicoTipo);
        const local=document.getElementById('historico-busca').value.trim(),global=filtros.elements.busca.value.trim();
        // Both searches must match the exported rows as well as the visible history.
        if(local)query.set('buscaHistorico',local);
        if(global)query.set('busca',global);
        window.exportarRelatorio('/api/financeiro/relatorio',query);
    };
    function categorias(){
        const root=document.getElementById('categorias');root.replaceChildren();
        const totals=new Map();for(const i of itens.filter(i=>i.tipo==='DESPESA'))totals.set(i.categoria,(totals.get(i.categoria)||0)+Number(i.valor));
        const sum=[...totals.values()].reduce((a,b)=>a+b,0);
        document.getElementById('categorias-total').textContent=moeda(sum);
        const colors=['#00288e','#0060ac','#64a8fe','#16a34a'];
        [...totals].sort((a,b)=>b[1]-a[1]).forEach(([nome,valor],index)=>{
            const box=document.createElement('div'),label=document.createElement('div'),name=document.createElement('span'),value=document.createElement('span'),amount=document.createElement('small'),track=document.createElement('div'),bar=document.createElement('div');
            const percent=sum?valor/sum*100:0;
            label.className='financeiro-category-label';name.textContent=nome;
            value.className='financeiro-category-value';value.textContent=percent.toLocaleString('pt-BR',{maximumFractionDigits:1})+'% ';
            amount.textContent='('+moeda(valor)+')';value.append(amount);label.append(name,value);
            track.className='financeiro-category-track';bar.style.width=percent+'%';bar.style.background=colors[index%colors.length];
            track.append(bar);box.append(label,track);root.append(box);
        });
        if(!totals.size)root.textContent='Nenhuma despesa no período.';
    }
    function grafico(){
        const months=new Map();
        if(filtros.elements.ano.value)for(let m=1;m<=12;m++)months.set(filtros.elements.ano.value+'-'+String(m).padStart(2,'0'),[0,0]);
        for(const i of itens){const key=i.data.slice(0,7);if(!months.has(key))months.set(key,[0,0]);months.get(key)[i.tipo==='RECEITA'?0:1]+=Number(i.valor);}
        const series=[...months].sort((a,b)=>a[0].localeCompare(b[0]));
        const root=document.getElementById('grafico');root.replaceChildren();
        if(!itens.length){root.textContent='Nenhum lançamento no período.';return;}
        const max=Math.max(1,...series.flatMap(([,v])=>v));
        const ns='http://www.w3.org/2000/svg',width=Math.max(700,series.length*55),bottom=250;
        function node(tag,attrs={},text){const el=document.createElementNS(ns,tag);for(const [key,value] of Object.entries(attrs))el.setAttribute(key,value);if(text!=null)el.textContent=text;return el;}
        const svg=node('svg',{viewBox:'0 0 '+width+' 295',role:'img','aria-label':'Fluxo mensal: entradas em verde e despesas em vermelho. Valores detalhados no histórico.'});
        const defs=node('defs');svg.append(defs);
        for(const y of [30,103,176,bottom])svg.append(node('line',{x1:0,x2:width,y1:y,y2:y,stroke:y===bottom?'#e2e8f0':'#f1f5f9','stroke-dasharray':y===bottom?'0':'4 4'}));
        const x=index=>series.length===1?width/2:15+index*(width-30)/(series.length-1);
        const colors=['#16a34a','#dc2626'];
        colors.forEach((color,j)=>{
            const gradient=node('linearGradient',{id:'financeiro-area-'+j,x1:0,x2:0,y1:0,y2:1});
            gradient.append(node('stop',{offset:'0%','stop-color':color,'stop-opacity':j?'.08':'.14'}),node('stop',{offset:'100%','stop-color':color,'stop-opacity':0}));defs.append(gradient);
            const points=series.map(([,values],index)=>[x(index),bottom-values[j]/max*210]);
            let path='M '+points[0].join(' ');
            for(let i=1;i<points.length;i++){const a=points[i-1],b=points[i],middle=(a[0]+b[0])/2;path+=' C '+middle+' '+a[1]+', '+middle+' '+b[1]+', '+b.join(' ');}
            svg.append(node('path',{d:path+' L '+points.at(-1)[0]+' '+bottom+' L '+points[0][0]+' '+bottom+' Z',fill:'url(#financeiro-area-'+j+')'}));
            svg.append(node('path',{d:path,fill:'none',stroke:color,'stroke-width':j?2:2.5,'stroke-linecap':'round','vector-effect':'non-scaling-stroke'}));
            points.forEach(([cx,cy],i)=>{
                const dot=node('circle',{cx,cy,r:i===points.length-1?5:3,fill:i===points.length-1?color:'white',stroke:i===points.length-1?'white':color,'stroke-width':2,tabindex:0});
                const detail=series[i][0]+' — '+(j?'Despesas: ':'Entradas: ')+moeda(series[i][1][j]);
                dot.setAttribute('aria-label',detail);dot.append(node('title',{},detail));svg.append(dot);
            });
        });
        const names=['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'];
        const step=Math.max(1,Math.ceil(series.length/7));
        series.forEach(([mes],index)=>{if(index%step!==0&&index!==series.length-1)return;const label=names[Number(mes.slice(5))-1]+(filtros.elements.ano.value?'':'/'+mes.slice(2,4));svg.append(node('text',{x:x(index),y:284,'text-anchor':index===0?'start':index===series.length-1?'end':'middle','font-size':11,fill:'#64748b'},label));});
        root.append(svg);
    }
    document.getElementById('novo').onclick=()=>editar();
    document.getElementById('cancelar').onclick=()=>modal.close();
    filtros.onsubmit=e=>{e.preventDefault();carregar();};
    document.getElementById('limpar-filtros').onclick=()=>{filtros.reset();carregar();};
    document.getElementById('exportar').onclick=()=>window.exportarRelatorio('/api/financeiro/relatorio',params());
    document.getElementById('limpar-exemplos').onclick=async()=>{
        if(!confirm('Remover todos os lançamentos de demonstração de todos os períodos? Os registros reais serão mantidos.'))return;
        try{await api('/api/financeiro/exemplos',{method:'DELETE'});await carregar();}catch(e){message(feedback,e.message,true);}
    };
    submit(form,async()=>{
        const {id,...dados}=Object.fromEntries(new FormData(form));dados.valor=Number(dados.valor);
        await api(`/api/financeiro${id?'/'+id:''}`,{method:id?'PUT':'POST',body:JSON.stringify(dados)});
        modal.close();await carregar();message(feedback,'Lançamento salvo no banco.');
    });
    carregar();
})();
