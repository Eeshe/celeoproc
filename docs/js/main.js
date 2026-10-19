/**
 * Boots the language: detects the browser language on first visit, honours a
 * previously chosen language stored in localStorage, and wires the toggle.
 *
 * Also powers the usage guide: commands type themselves out and screenshots
 * float up the first time each step scrolls into view.
 */
(function () {
  const STORAGE_KEY = "celeoproc-lang";
  const supportedLanguages = ["en", "es"];
  const TYPING_DELAY_MS = 38;
  const reducedMotionQuery = window.matchMedia(
    "(prefers-reduced-motion: reduce)"
  );

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

  function currentLanguage() {
    return document.documentElement.lang === "es" ? "es" : defaultLanguage;
  }

  function getUsageSteps() {
    return Array.from(document.querySelectorAll("[data-usage-step]"));
  }

  function commandFor(codeElement, lang) {
    return lang === "es"
      ? codeElement.dataset.commandEs
      : codeElement.dataset.commandEn;
  }

  function updateInlineCommands(lang) {
    document.querySelectorAll(".step__inline-command").forEach(function (element) {
      element.textContent =
        commandFor(element, lang) || commandFor(element, defaultLanguage) || "";
    });
  }

  function updateImages(lang) {
    document.querySelectorAll("[data-src-en][data-src-es]").forEach(function (img) {
      const next = lang === "es" ? img.dataset.srcEs : img.dataset.srcEn;
      const width = lang === "es" ? img.dataset.widthEs : img.dataset.widthEn;
      const height = lang === "es" ? img.dataset.heightEs : img.dataset.heightEn;

      if (next && img.getAttribute("src") !== next) {
        img.setAttribute("src", next);
      }
      if (width) {
        img.setAttribute("width", width);
      }
      if (height) {
        img.setAttribute("height", height);
      }
    });
  }

  function resolveCommand(step, lang) {
    const code = step.querySelector(".step__command-code");
    if (!code) {
      return null;
    }
    const typed = code.querySelector(".step__typed");
    const full =
      commandFor(code, lang) || commandFor(code, defaultLanguage) || "";
    return { code, typed, full };
  }

  function setFullCommand(step, lang) {
    const resolved = resolveCommand(step, lang);
    if (!resolved) {
      return null;
    }
    const screenReaderText = resolved.code.querySelector(".sr-only");
    if (screenReaderText) {
      screenReaderText.textContent = resolved.full;
    }
    return resolved;
  }

  function stopTyping(code) {
    if (code._celeoprocTypingTimer) {
      clearTimeout(code._celeoprocTypingTimer);
      code._celeoprocTypingTimer = null;
    }
    code.classList.remove("is-typing");
  }

  function typeCommand(step, lang) {
    const resolved = setFullCommand(step, lang);
    if (!resolved) {
      return;
    }
    const { code, typed, full } = resolved;
    stopTyping(code);

    if (reducedMotionQuery.matches) {
      if (typed) {
        typed.textContent = full;
      }
      step.classList.add("is-visible", "is-typed");
      return;
    }

    if (typed) {
      typed.textContent = "";
    }
    code.classList.add("is-typing");

    let index = 0;
    function tick() {
      index += 1;
      if (typed) {
        typed.textContent = full.slice(0, index);
      }
      if (index < full.length) {
        code._celeoprocTypingTimer = setTimeout(tick, TYPING_DELAY_MS);
      } else {
        stopTyping(code);
        step.classList.add("is-typed");
      }
    }

    tick();
  }

  function initUsage() {
    const usage = document.querySelector(".usage");
    const steps = getUsageSteps();
    if (!usage || steps.length === 0) {
      return;
    }

    updateInlineCommands(currentLanguage());
    updateImages(currentLanguage());

    steps.forEach(function (step) {
      const resolved = setFullCommand(step, currentLanguage());
      if (resolved && resolved.typed) {
        // Keep the full command in the DOM for no-JS and crawler fallbacks.
        // CSS hides it while the step is pending, so typing starts from empty
        // instead of flashing the full command first.
        resolved.typed.textContent = resolved.full;
      }
    });

    usage.classList.add("usage--animated");

    if (reducedMotionQuery.matches || !("IntersectionObserver" in window)) {
      steps.forEach(function (step) {
        typeCommand(step, currentLanguage());
      });
      return;
    }

    const observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) {
            return;
          }
          observer.unobserve(entry.target);
          entry.target.classList.add("is-visible");
          typeCommand(entry.target, currentLanguage());
        });
      },
      { threshold: 0.2, rootMargin: "0px 0px -8% 0px" }
    );

    steps.forEach(function (step) {
      observer.observe(step);
    });
  }

  // Commands are typed from the HTML data attributes, so a language switch
  // has to reset the already-revealed commands. Pending steps just swap the
  // stored text and type the new command when they enter the viewport.
  document.addEventListener("celeoproc:languagechange", function (event) {
    const lang =
      event.detail && event.detail.lang === "es" ? "es" : defaultLanguage;

    getUsageSteps().forEach(function (step) {
      if (step.classList.contains("is-visible")) {
        typeCommand(step, lang);
        return;
      }

      const resolved = setFullCommand(step, lang);
      if (resolved && resolved.typed) {
        resolved.typed.textContent = resolved.full;
      }
    });

    updateInlineCommands(lang);
    updateImages(lang);
  });

  document.addEventListener("DOMContentLoaded", function () {
    initUsage();
    let current = setLanguage(detectLanguage());

    const toggle = document.getElementById("lang-toggle");
    if (toggle) {
      toggle.addEventListener("click", function () {
        current = setLanguage(current === "en" ? "es" : "en");
      });
    }
  });
})();
