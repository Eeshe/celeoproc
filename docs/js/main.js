/**
 * Boots the language: detects the browser language on first visit, honours a
 * previously chosen language stored in localStorage, and wires the toggle.
 */
(function () {
  const STORAGE_KEY = "celeoproc-lang";
  const supportedLanguages = ["en", "es"];

  function readStoredLanguage() {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch (error) {
      return null;
    }
  }

  function storeLanguage(lang) {
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch (error) {
      /* localStorage unavailable (private mode); the choice is not persisted. */
    }
  }

  function detectLanguage() {
    const stored = readStoredLanguage();
    if (stored && supportedLanguages.includes(stored)) {
      return stored;
    }
    const browserLanguage = (
      navigator.language ||
      navigator.userLanguage ||
      defaultLanguage
    ).toLowerCase();
    return browserLanguage.startsWith("es") ? "es" : defaultLanguage;
  }

  function setLanguage(lang) {
    const next = supportedLanguages.includes(lang) ? lang : defaultLanguage;
    storeLanguage(next);
    applyLanguage(next);
    return next;
  }

  document.addEventListener("DOMContentLoaded", function () {
    let current = setLanguage(detectLanguage());

    const toggle = document.getElementById("lang-toggle");
    if (toggle) {
      toggle.addEventListener("click", function () {
        current = setLanguage(current === "en" ? "es" : "en");
      });
    }
  });
})();
