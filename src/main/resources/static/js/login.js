document.addEventListener('DOMContentLoaded', () => {
    const button = document.getElementById('toggle-password');
    const input = document.getElementById('password');
    const icon = document.getElementById('eye-icon-span');
    if (button && input) {
        button.setAttribute('aria-controls', input.id);
        button.setAttribute('aria-pressed', String(input.type === 'text'));
        button.setAttribute('aria-label', 'Mostrar senha');
        icon?.setAttribute('aria-hidden', 'true');
    }
    button?.addEventListener('click', () => {
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        icon.textContent = show ? 'visibility_off' : 'visibility';
        button.setAttribute('aria-pressed', String(show));
    });
});
