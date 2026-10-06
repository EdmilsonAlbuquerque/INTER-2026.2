// Comportamento de navegação sem alterar as classes e estilos dos HTMLs originais.
document.querySelectorAll('header .material-symbols-outlined').forEach(icon => {
    if (icon.textContent.trim() !== 'person') return;
    const control = icon.parentElement;
    control.setAttribute('role', 'link');
    control.setAttribute('tabindex', '0');
    control.setAttribute('aria-label', 'Meu perfil');
    control.addEventListener('click', () => location.assign('/meu-perfil'));
    control.addEventListener('keydown', event => {
        if (event.key === 'Enter') location.assign('/meu-perfil');
    });
});

document.querySelectorAll('button').forEach(button => {
    if (button.textContent.trim() === 'Cancelar' && !button.closest('dialog')) {
        button.addEventListener('click', () => location.assign('/gerenciamento'));
    }
});

// Os modelos fornecidos são demonstrativos e ainda não submetem dados à API.
// Evita o feedback de salvamento fictício que existia no modelo de cadastro.
const showPendingIntegration = event => {
    event.preventDefault();
    window.alert('Esta tela é uma prévia visual. O envio dos dados ainda não está disponível.');
};
document.querySelectorAll('#formCadastroSocial').forEach(form => {
    form.addEventListener('submit', showPendingIntegration);
});
const save = document.getElementById('btnSalvar');
if (save && save.type !== 'submit') save.addEventListener('click', showPendingIntegration);
