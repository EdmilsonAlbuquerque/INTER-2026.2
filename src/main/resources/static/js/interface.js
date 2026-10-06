document.body.classList.add('app-shell');
const pageHeader = document.querySelector('body > header');
if (pageHeader) new ResizeObserver(() => {
    const main = document.querySelector('body > main');
    if (main && getComputedStyle(pageHeader).position === 'fixed') main.style.paddingTop = `${pageHeader.getBoundingClientRect().height}px`;
}).observe(pageHeader);
window.exportarRelatorio = (endpoint, params = new URLSearchParams()) => {
    let modal = document.getElementById('exportar-modal');
    if (!modal) {
        modal = document.createElement('dialog'); modal.id = 'exportar-modal';
        modal.innerHTML = `<form id="exportar-form" class="space-y-4"><h2 class="text-xl font-semibold">Exportar relatório</h2><p class="text-sm text-slate-500">O relatório inclui todos os registros dos filtros selecionados.</p><label class="block">Formato<select name="formato" class="campo"><option value="pdf">PDF</option><option value="xlsx">Excel (.xlsx)</option></select></label><div class="flex justify-end gap-3"><button type="button" data-fechar>Cancelar</button><button type="submit" class="bg-blue-800 text-white px-4 py-2 rounded-lg">Baixar relatório</button></div></form>`;
        document.body.append(modal);modal.querySelector('[data-fechar]').onclick=()=>modal.close();
    }
    const form=modal.querySelector('form');
    form.onsubmit=async event=>{
        event.preventDefault();const button=form.querySelector('[type=submit]');button.disabled=true;
        try {
            const query=new URLSearchParams(params);query.set('formato',form.elements.formato.value);
            const response=await fetch(`${endpoint}?${query}`);
            if(!response.ok){const error=await response.json();throw new Error(error.message || 'Não foi possível exportar.');}
            const url=URL.createObjectURL(await response.blob());const a=document.createElement('a');a.href=url;
            a.download=`relatorio.${form.elements.formato.value}`;document.body.append(a);a.click();a.remove();
            setTimeout(()=>URL.revokeObjectURL(url),30000);modal.close();
        }catch(error){window.acesso.message(form,error.message,true);}finally{button.disabled=false;}
    };
    modal.showModal();
};
