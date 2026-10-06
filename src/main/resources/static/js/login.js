document.addEventListener('DOMContentLoaded', () => {
    const button = document.getElementById('toggle-password');
    const input = document.getElementById('password');
    const icon = document.getElementById('eye-icon-span');
    button?.addEventListener('click', () => {
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        icon.textContent = show ? 'visibility_off' : 'visibility';
        button.setAttribute('aria-pressed', String(show));
    });
});
