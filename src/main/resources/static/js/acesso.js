window.acesso = (() => {
    let csrf;
    async function api(url, options = {}) {
        if (!csrf) {
            const response = await fetch('/auth/csrf');
            if (!response.ok) throw new Error('Não foi possível iniciar a sessão. Atualize a página.');
            csrf = await response.json();
        }
        const response = await fetch(url, { ...options, headers: {
            ...(options.body instanceof FormData ? {} : {'Content-Type': 'application/json'}), [csrf.headerName]: csrf.token, ...options.headers
        }});
        const data = response.status === 204 ? {} : await response.json();
        if (!response.ok) {
            if (response.status === 401) location.assign('/login?expirou');
            const details = data.errors ? Object.values(data.errors).join(' ') : '';
            const error = new Error(details || data.message || 'Não foi possível concluir a operação.');
            error.errors = data.errors;
            throw error;
        }
        return data;
    }
    function message(form, text, error = false) {
        let box = form.querySelector('[data-feedback]');
        if (!box) {
            box = document.createElement('p'); box.dataset.feedback = '';
            box.setAttribute('role', 'status'); box.setAttribute('aria-live', 'polite');
            box.setAttribute('aria-atomic', 'true');
            form.prepend(box);
        }
        box.className = `text-sm p-3 rounded-lg ${error ? 'bg-red-50 text-red-700' : 'bg-blue-50 text-blue-900'}`;
        box.textContent = text;
    }
    function submit(form, handler) {
        if (!form) return;
        form.removeAttribute('onsubmit');
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const button = form.querySelector('[type="submit"]');
            if (button?.disabled) return;
            if (button) button.disabled = true;
            try { await handler(new FormData(form)); }
            catch (error) {
                message(form, error.message, true);
                window.formularios?.serverErrors(form, error.errors);
            }
            finally { if (button) button.disabled = false; }
        });
    }
    return { api, message, submit };
})();

(async () => {
    const { api, message, submit } = window.acesso;
    const params = new URLSearchParams(location.search);
    const login = document.querySelector('[data-purpose="credential-form"]');
    if (login) {
        login.removeAttribute('onsubmit');
        if (params.has('erro')) message(login, 'E-mail ou senha inválidos, ou conta desativada.', true);
        if (params.has('saiu')) message(login, 'Você saiu do sistema.');
        if (params.has('expirou')) message(login, 'Sua sessão expirou. Entre novamente.', true);
        if (params.has('cadastro')) message(login, 'Conta criada. Entre com seu e-mail e senha.');
        if (params.has('atualizado')) message(login, 'Dados atualizados. Entre novamente.');
    }
    const signup = document.querySelector('[data-purpose="signup-form"]');
    if (signup && params.has('convite')) signup.elements.invite_token.value = params.get('convite');
    submit(signup, async fields => {
        const birth = fields.get('birthdate').trim();
        const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(birth);
        const dataNascimento = match ? `${match[3]}-${match[2]}-${match[1]}` : birth;
        if (!/^\d{4}-\d{2}-\d{2}$/.test(dataNascimento)) throw new Error('Informe a data de nascimento como dd/mm/aaaa.');
        await api('/auth/cadastro', {method: 'POST', body: JSON.stringify({
            nomeCompleto: fields.get('fullname'), email: fields.get('email'), senha: fields.get('password'),
            cpf: fields.get('cpf').replace(/\D/g, ''), telefone: fields.get('phone').replace(/\D/g, ''),
            dataNascimento, tokenConvite: fields.get('invite_token')
        })});
        location.assign('/login?cadastro');
    });
    const recovery = document.querySelector('[data-purpose="recovery-form"]');
    submit(recovery, async fields => {
        const result = await api('/auth/recuperar', {method: 'POST', body: JSON.stringify({email: fields.get('email')})});
        message(recovery, result.message);
    });
    const reset = document.getElementById('redefinir-form');
    submit(reset, async fields => {
        if (fields.get('senha') !== fields.get('confirmacao')) throw new Error('As senhas precisam ser iguais.');
        await api('/auth/redefinir', {method: 'POST', body: JSON.stringify({token: params.get('token'), senha: fields.get('senha')})});
        location.assign('/login?atualizado');
    });
    if (!['/gerenciamento', '/cadastro-pessoa', '/meu-perfil', '/admin', '/servicos', '/financeiro', '/DASHFINACEIRO.html'].includes(location.pathname)) return;
    try {
        const me = await api('/auth/me');
        window.usuarioAtual = me;
        document.dispatchEvent(new CustomEvent('usuario:carregado', {detail: me}));
        const updatePhoto = url => {
            document.querySelectorAll('[data-foto-perfil]').forEach(img => {img.src = url || '/institucional/assets/logo-instituto-manancial.png';});
            document.querySelectorAll('header .material-symbols-outlined').forEach(icon => {
                if (icon.textContent.trim() !== 'person') return;
                const img = document.createElement('img'); img.dataset.fotoPerfil = '';
                img.src = url || '/institucional/assets/logo-instituto-manancial.png'; img.alt = 'Minha foto de perfil';
                img.className = 'w-full h-full rounded-full object-cover'; icon.replaceWith(img);
            });
        };
        updatePhoto(me.fotoUrl);
        document.querySelectorAll('header span').forEach(span => {
            if (span.textContent.trim() === 'Ana Souza') span.textContent = me.nomeCompleto;
        });
        const header = document.querySelector('body > header > div');
        if (header) {
            const actions = document.createElement('div'); actions.className = 'flex items-center gap-3 text-sm';
            for (const [url, text] of [['/servicos','Serviços'], ...(me.perfil === 'ADMINISTRADOR' ? [['/financeiro','Financeiro']] : [])]) {
                if (document.querySelector(`header a[href="${url}"]`)) continue;
                const link = document.createElement('a'); link.href = url; link.textContent = text; link.className = 'text-blue-800 font-semibold'; actions.append(link);
            }
            if (me.perfil === 'ADMINISTRADOR') {
                const admin = document.createElement('a'); admin.href = '/admin'; admin.textContent = 'Usuários e acessos';
                admin.className = 'text-blue-800 font-semibold'; actions.append(admin);
            }
            const role = document.createElement('span'); role.className = 'text-xs text-slate-500'; role.textContent = me.perfil;
            const logout = document.createElement('button'); logout.type = 'button'; logout.textContent = 'Sair';
            logout.className = 'text-blue-800 font-semibold';
            logout.addEventListener('click', async () => {
                try {
                    const token = await (await fetch('/auth/csrf')).json();
                    const form = document.createElement('form'); form.method = 'POST'; form.action = '/logout';
                    const input = document.createElement('input'); input.type = 'hidden'; input.name = token.parameterName; input.value = token.token;
                    form.append(input); document.body.append(form); form.submit();
                } catch { window.alert('Não foi possível sair. Atualize a página e tente novamente.'); }
            });
            actions.append(role, logout); header.append(actions);
        }
        if (me.perfil === 'LEITOR') {
            document.querySelectorAll('a[href="/cadastro-pessoa"]').forEach(link => link.style.display = 'none');
        }
        if (location.pathname === '/gerenciamento' && me.perfil === 'LEITOR') {
            document.querySelectorAll('button').forEach(button => {
                if (/editar|salvar|novo cadastro|adicionar|excluir|delete|edit/.test(button.textContent.toLowerCase())) button.style.display = 'none';
            });
        } else if (location.pathname === '/gerenciamento' && me.perfil === 'OPERADOR') {
            document.querySelectorAll('button').forEach(button => {
                if (/excluir|delete/.test(button.textContent.toLowerCase())) button.style.display = 'none';
            });
        }
        if (location.pathname === '/meu-perfil') {
            const photo = document.querySelector('img[alt="Foto de Perfil"]');
            if (photo) {photo.dataset.fotoPerfil = ''; updatePhoto(me.fotoUrl);}
            const picker = document.getElementById('foto-arquivo');
            const upload = document.getElementById('trocar-foto');
            const remove = document.getElementById('remover-foto');
            upload?.addEventListener('click', () => picker.click());
            picker?.addEventListener('change', async () => {
                const file = picker.files[0]; if (!file) return;
                const profile = document.querySelector('main form');
                upload.disabled = remove.disabled = true;
                try {
                    if (file.size > 5 * 1024 * 1024) throw new Error('A foto deve ter até 5 MB.');
                    const data = new FormData(); data.append('arquivo', file);
                    const result = await api('/auth/me/foto', {method:'POST', body:data});
                    me.fotoUrl = result.fotoUrl; updatePhoto(result.fotoUrl);
                    message(profile, 'Foto atualizada e salva.');
                } catch (error) { message(profile,error.message,true); }
                finally {upload.disabled = remove.disabled = false; picker.value = '';}
            });
            remove?.addEventListener('click', async () => {
                upload.disabled = remove.disabled = true;
                try {await api('/auth/me/foto', {method:'DELETE'});me.fotoUrl=null;updatePhoto(null);message(document.querySelector('main form'),'Foto removida.');}
                catch(error){message(document.querySelector('main form'),error.message,true);}
                finally {upload.disabled = remove.disabled = false;}
            });
            document.getElementById('name').value = me.nomeCompleto;
            document.getElementById('email').value = me.email;
            const profile = document.querySelector('main form');
            submit(profile, async () => {
                const novaSenha = document.getElementById('pwd_new').value;
                if (novaSenha !== document.getElementById('pwd_confirm').value) throw new Error('As novas senhas precisam ser iguais.');
                await api('/auth/me', {method: 'PUT', body: JSON.stringify({
                    nomeCompleto: document.getElementById('name').value,
                    email: document.getElementById('email').value,
                    senhaAtual: document.getElementById('pwd_current').value, novaSenha
                })});
                location.assign('/login?atualizado');
            });
        }
    } catch (error) { window.alert(error.message); }
})();
