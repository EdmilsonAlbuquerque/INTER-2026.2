/* ============================================================
   Instituto Manancial — interações da página
   A seção de doação aqui é só a interface (ilustrativa).
   A geração real do Pix (QR Code + copia e cola) deve ser feita
   pelo back-end, usando a chave Pix oficial da ONG.
   ============================================================ */

document.addEventListener("DOMContentLoaded", () => {

  // Ano no rodapé
  const yearEl = document.getElementById("year");
  if (yearEl) yearEl.textContent = new Date().getFullYear();

  // Menu mobile
  const navToggle = document.getElementById("navToggle");
  const siteNav = document.getElementById("siteNav");
  if (navToggle && siteNav) {
    navToggle.addEventListener("click", () => {
      const isOpen = siteNav.classList.toggle("is-open");
      navToggle.setAttribute("aria-expanded", String(isOpen));
    });
    siteNav.querySelectorAll("a").forEach(link => {
      link.addEventListener("click", () => {
        siteNav.classList.remove("is-open");
        navToggle.setAttribute("aria-expanded", "false");
      });
    });
  }

  // Seleção de valor de doação (apenas visual — não gera cobrança real)
  const amountButtons = document.querySelectorAll(".amount-btn");
  const customAmount = document.getElementById("customAmount");

  amountButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      amountButtons.forEach(b => b.classList.remove("is-active"));
      btn.classList.add("is-active");
      if (customAmount) customAmount.value = "";
    });
  });

  if (customAmount) {
    customAmount.addEventListener("input", () => {
      if (customAmount.value.trim() !== "") {
        amountButtons.forEach(b => b.classList.remove("is-active"));
      }
    });
  }

  // Botão "Copiar" do Pix Copia e Cola (ilustrativo)
  const copyBtn = document.getElementById("copyPixBtn");
  const pixInput = document.getElementById("pixCode");

  if (copyBtn && pixInput) {
    copyBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(pixInput.value);
      } catch (err) {
        // Fallback para navegadores sem suporte à Clipboard API
        pixInput.removeAttribute("readonly");
        pixInput.select();
        document.execCommand("copy");
        pixInput.setAttribute("readonly", "true");
      }
      const original = copyBtn.textContent;
      copyBtn.textContent = "Copiado!";
      setTimeout(() => { copyBtn.textContent = original; }, 1800);
    });
  }

});
