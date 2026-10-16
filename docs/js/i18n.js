/**
 * Translation dictionaries and the render function that applies them.
 *
 * Every visible string in index.html carries a `data-i18n="key"` attribute;
 * `applyLanguage` replaces its text with the matching entry below. Keep all
 * supported languages in sync when adding or renaming a key.
 */
const translations = {
    en: {
        title: "Celeoproc - A power outage tracker for Discord",
        tagline: "A power outage tracker for Discord",
        support:
            "Your server members log when the power goes out and comes back, and Celeoproc keeps the stats",
        install: "Add to Discord",
        footer: "Made for the third world",
        toggle: "Switch to Spanish",
        github: "Celeoproc on GitHub",
    },
    es: {
        title: "Celeoproc - Un rastreador de apagones para Discord",
        tagline: "Un seguidor de apagones para Discord",
        support: "Tu comunidad registra cuándo se les va y vuelve la luz, y Celeoproc guarda las estadísticas",
        install: "Añadir a Discord",
        footer: "Hecho para el tercer mundo",
        toggle: "Cambiar a inglés",
        github: "Celeoproc en GitHub",
    },
};

const defaultLanguage = "en";

/**
 * Applies `lang` to the document: updates <html lang>, the page title, every
 * `data-i18n` node, and the language toggle's label.
 */
function applyLanguage(lang) {
    const dict = translations[lang] || translations[defaultLanguage];

    document.documentElement.lang = lang;
    if (dict.title) {
        document.title = dict.title;
    }

    document.querySelectorAll("[data-i18n]").forEach((element) => {
        const key = element.getAttribute("data-i18n");
        if (dict[key] !== undefined) {
            element.textContent = dict[key];
        }
    });

    document.querySelectorAll("[data-i18n-aria-label]").forEach((element) => {
        const key = element.getAttribute("data-i18n-aria-label");
        if (dict[key] !== undefined) {
            element.setAttribute("aria-label", dict[key]);
        }
    });

    const toggle = document.getElementById("lang-toggle");
    if (toggle) {
        // Show the language the button switches *to*.
        toggle.textContent = lang === "en" ? "ES" : "EN";
        toggle.setAttribute("aria-label", dict.toggle);
    }
}
