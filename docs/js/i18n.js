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
        scroll_hint: "Scroll down to see how Celeoproc works",
        usage_title: "Usage Guide",
        usage_postembed_desc:
            "Posts the live status embed in the current channel. Members use its buttons to report when their power goes out and comes back.",
        usage_activate_desc:
            "Turns on automatic alerts for the embed with the ID in the channel.",
        usage_deactivate_prefix: "Use",
        usage_deactivate_suffix: "to turn alerts off.",
        usage_poststats_desc:
            "Posts server totals, per-member stats and the outage graph.",
        usage_alt_embed:
            "Celeoproc status embed listing Marcos Pérez Jiménez with their electricity status, plus buttons to report power out, power in, remove participant and opt out.",
        usage_alt_alert:
            "Celeoproc alert messages reporting when Marcos Pérez Jiménez lost power and when it came back, with edit and delete buttons.",
        usage_alt_stats:
            "Celeoproc stats embed with general server stats and per-member stats for October 2026.",
        usage_alt_graph:
            "Outage graph for Marcos Pérez Jiménez in October 2026, plotting outage duration against days of the month.",
    },
    es: {
        title: "Celeoproc - Un rastreador de apagones para Discord",
        tagline: "Un seguidor de apagones para Discord",
        support: "Tu comunidad registra cuándo se les va y vuelve la luz, y Celeoproc guarda las estadísticas",
        install: "Añadir a Discord",
        footer: "Hecho para el tercer mundo",
        toggle: "Cambiar a inglés",
        github: "Celeoproc en GitHub",
        scroll_hint: "Desliza hacia abajo para ver cómo funciona Celeoproc",
        usage_title: "Guía de Uso",
        usage_postembed_desc:
            "Publica la cartelera de seguimiento en el canal actual. Los usuarios usan los botones para avisar cuándo se les va y vuelve la luz.",
        usage_activate_desc:
            "Activa las alertas automáticas de la cartelera con el ID en este canal.",
        usage_deactivate_prefix: "Usa",
        usage_deactivate_suffix: "para desactivar las alertas.",
        usage_poststats_desc:
            "Publica los totales del servidor, las estadísticas por usuario y el gráfico de idas de luz.",
        usage_alt_embed:
            "Cartelera de estado de Celeoproc con Marcos Pérez Jiménez, su estado eléctrico y botones para avisar que se fue la luz, que llegó, eliminar participante y no participar.",
        usage_alt_alert:
            "Mensajes de alerta de Celeoproc que avisan cuándo se le fue la luz a Marcos Pérez Jiménez y cuándo volvió, con botones de editar y eliminar.",
        usage_alt_stats:
            "Cartelera de estadísticas de Celeoproc con datos generales del servidor y por miembro para octubre de 2026.",
        usage_alt_graph:
            "Gráfico de apagones de Marcos Pérez Jiménez en octubre de 2026, con la duración de los apagones por día del mes.",
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

    document.querySelectorAll("[data-i18n-alt]").forEach((element) => {
        const key = element.getAttribute("data-i18n-alt");
        if (dict[key] !== undefined) {
            element.setAttribute("alt", dict[key]);
        }
    });

    const toggle = document.getElementById("lang-toggle");
    if (toggle) {
        // Show the language the button switches *to*.
        toggle.textContent = lang === "en" ? "ES" : "EN";
        toggle.setAttribute("aria-label", dict.toggle);
    }

    document.dispatchEvent(
        new CustomEvent("celeoproc:languagechange", { detail: { lang } })
    );
}
