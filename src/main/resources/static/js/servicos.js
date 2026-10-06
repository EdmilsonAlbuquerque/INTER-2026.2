(() => {
    const {api,message,submit}=window.acesso;
    const panel=document.getElementById('servicos-panel'), form=document.getElementById('servico-form'), modal=document.getElementById('servico-modal');
    let registros=[], responsaveis=[], usuario;
    const busca=document.getElementById('busca-servico'),status=document.getElementById('status-servico');
    const query=()=>{const q=new URLSearchParams({busca:busca.value});if(status.value)q.set('ativo',status.value);return q;};
    function render(){
        const list=document.getElementById('servicos-lista');list.replaceChildren();
        const dados=registros.filter(s=>(`${s.nome} ${s.categoria||''}`).toLocaleLowerCase().includes(busca.value.trim().toLocaleLowerCase())&&(!status.value||String(s.ativo)===status.value));
        document.getElementById('servicos-contagem').textContent=`${dados.length} serviço(s) encontrado(s)`;
        for(const s of dados){const tr=document.createElement('tr');tr.className='border-t border-slate-100';
            for(const value of [s.nome,s.categoria||'—',s.responsavelNome,s.duracaoMinutos?`${s.duracaoMinutos} min`:'—',s.ativo?'Ativo':'Inativo']){const td=document.createElement('td');td.className='p-4';td.textContent=value;tr.append(td);}
            const actions=document.createElement('td');actions.className='p-4 space-x-3';
            if(usuario?.perfil!=='LEITOR'){const edit=document.createElement('button');edit.textContent='Editar';edit.className='text-blue-800';edit.onclick=()=>open(s);actions.append(edit);}
            if(usuario?.perfil==='ADMINISTRADOR'){const del=document.createElement('button');del.textContent='Excluir';del.className='text-red-700';del.onclick=async()=>{if(!confirm(`Excluir o serviço ${s.nome}?`))return;try{await api(`/api/servicos/${s.id}`,{method:'DELETE'});await load();}catch(e){message(panel,e.message,true);}};actions.append(del);}
            tr.append(actions);list.append(tr);
        }
        if(!dados.length){const tr=document.createElement('tr');const td=document.createElement('td');td.colSpan=6;td.className='p-8 text-center text-slate-500';td.textContent='Nenhum serviço encontrado.';tr.append(td);list.append(tr);}
    }
    function options(){form.elements.responsavelId.replaceChildren(new Option('Selecione um responsável',''));responsaveis.forEach(r=>form.elements.responsavelId.add(new Option(r.nome,String(r.id))));}
    function open(s={}){form.reset();form.querySelector('[data-feedback]')?.remove();document.getElementById('servico-titulo').textContent=s.id?'Editar serviço':'Novo serviço';for(const key of ['id','nome','categoria','descricao','duracaoMinutos','responsavelId'])form.elements[key].value=s[key]??'';form.elements.ativo.checked=s.ativo??true;modal.showModal();}
    async function load(){registros=await api('/api/servicos');render();}
    document.querySelectorAll('dialog [data-close]').forEach(b=>b.onclick=()=>b.closest('dialog').close());
    document.getElementById('novo-servico').onclick=()=>open();busca.oninput=render;status.onchange=render;
    document.getElementById('limpar-servico').onclick=()=>{busca.value='';status.value='';render();};
    document.getElementById('relatorio-servicos').onclick=()=>window.exportarRelatorio('/api/relatorios/servicos',query());
    document.getElementById('novo-responsavel').onclick=()=>document.getElementById('responsavel-modal').showModal();
    submit(form,async fields=>{const id=fields.get('id');await api('/api/servicos'+(id?`/${id}`:''),{method:id?'PUT':'POST',body:JSON.stringify({nome:fields.get('nome'),categoria:fields.get('categoria'),descricao:fields.get('descricao'),duracaoMinutos:fields.get('duracaoMinutos')?Number(fields.get('duracaoMinutos')):null,responsavelId:Number(fields.get('responsavelId')),ativo:form.elements.ativo.checked})});modal.close();await load();message(panel,'Serviço salvo.');});
    submit(document.getElementById('responsavel-form'),async fields=>{const r=await api('/api/responsaveis',{method:'POST',body:JSON.stringify(Object.fromEntries(fields))});responsaveis.push(r);options();form.elements.responsavelId.value=r.id;document.getElementById('responsavel-modal').close();});
    (async()=>{try{[usuario,responsaveis]=await Promise.all([api('/auth/me'),api('/api/responsaveis')]);document.getElementById('novo-servico').hidden=usuario.perfil==='LEITOR';options();await load();}catch(e){message(panel,e.message,true);}})();
})();
