const { api, message, submit } = window.acesso;
const roles = ['ADMINISTRADOR', 'OPERADOR', 'LEITOR'];
const panel = document.getElementById('admin-panel');
const invite = document.getElementById('convite-form');
submit(invite, async fields => {
    const result = await api('/admin/api/convites', {method: 'POST', body: JSON.stringify({email: fields.get('email') || null, perfil: fields.get('perfil')})});
    document.getElementById('convite-link').value = result.url;
    message(invite, 'Convite criado. Envie o link à pessoa convidada. Válido por 24 horas, para um único cadastro.');
});
async function loadUsers() {
    try {
        const users = await api('/usuarios');
        const list = document.getElementById('usuarios-lista'); list.replaceChildren();
        for (const user of users) {
            const row = document.createElement('tr'); row.className = 'border-t border-slate-100';
            const name = document.createElement('td'); name.className = 'p-4'; name.textContent = user.nomeCompleto;
            const email = document.createElement('td'); email.className = 'p-4'; email.textContent = user.email;
            const roleCell = document.createElement('td'); roleCell.className = 'p-4';
            const role = document.createElement('select'); role.className = 'rounded-lg border-slate-200 text-sm'; role.setAttribute('aria-label', `Perfil de ${user.nomeCompleto}`);
            roles.forEach(value => { const option = document.createElement('option'); option.value = value; option.textContent = value; role.append(option); }); role.value = user.perfil;
            roleCell.append(role);
            const status = document.createElement('td'); status.className = 'p-4';
            const active = document.createElement('input'); active.type = 'checkbox'; active.checked = user.ativo; active.setAttribute('aria-label', `Conta ativa de ${user.nomeCompleto}`); status.append(active);
            const actions = document.createElement('td'); actions.className = 'p-4 space-x-3';
            const save = document.createElement('button'); save.textContent = 'Salvar'; save.className = 'text-blue-800 font-semibold';
            save.onclick = async () => {
                save.disabled = true;
                try { await api(`/admin/api/usuarios/${user.id}/acesso`, {method: 'PATCH', body: JSON.stringify({perfil: role.value, ativo: active.checked})}); message(panel, 'Permissões atualizadas. As sessões anteriores foram revogadas.'); await loadUsers(); }
                catch (error) { message(panel, error.message, true); } finally { save.disabled = false; }
            };
            const reset = document.createElement('button'); reset.textContent = 'Redefinir senha'; reset.className = 'text-blue-800';
            reset.onclick = async () => {
                try { const result = await api(`/admin/api/usuarios/${user.id}/redefinir`, {method: 'POST'}); document.getElementById('redefinicao-link').value = result.url; message(panel, 'Link de redefinição criado. Envie ao titular da conta; expira em 30 minutos.'); }
                catch (error) { message(panel, error.message, true); }
            };
            actions.append(save, reset); row.append(name, email, roleCell, status, actions); list.append(row);
        }
    } catch (error) { message(panel, error.message, true); }
}
loadUsers();
