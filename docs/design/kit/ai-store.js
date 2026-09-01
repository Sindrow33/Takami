// Единый стор ИИ-настроек — общий для Reader, Player и Settings.
// Ключ и провайдер живут здесь, а не в каждом экране.
// Используется через window.AiStore.get() / .set() / hook useAiStore().
(function () {
  const KEY = 'takami:ai-cfg';
  const load = () => {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw) return JSON.parse(raw);
    } catch (e) {}
    return { provider: 'openai', apiKey: '', model: 'gpt-4o-mini', endpoint: '' };
  };

  let state = load();
  const subs = new Set();
  const emit = () => { subs.forEach(fn => fn(state)); };

  window.AiStore = {
    get: () => state,
    set: (patch) => {
      state = { ...state, ...patch };
      try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) {}
      emit();
    },
    clear: () => {
      state = { provider: 'openai', apiKey: '', model: 'gpt-4o-mini', endpoint: '' };
      try { localStorage.removeItem(KEY); } catch (e) {}
      emit();
    },
    subscribe: (fn) => { subs.add(fn); return () => subs.delete(fn); },
    // Настроенный ли ключ ИИ
    isReady: () => !!(state.apiKey && state.apiKey.trim().length > 8),
    // Провайдеры со свойствами
    providers: {
      openai:    { n: 'OpenAI',    tag: 'GPT', hint: 'sk-...', endpoint: 'api.openai.com',      models: ['gpt-4o-mini', 'gpt-4o', 'gpt-3.5-turbo'] },
      anthropic: { n: 'Anthropic', tag: 'C',   hint: 'sk-ant-...', endpoint: 'api.anthropic.com', models: ['claude-3.5-haiku', 'claude-3.5-sonnet'] },
      google:    { n: 'Google',    tag: 'G',   hint: 'AIza...', endpoint: 'generativelanguage.googleapis.com', models: ['gemini-2.0-flash', 'gemini-1.5-pro'] },
      custom:    { n: 'Свой',      tag: '⚙',  hint: 'https://...', endpoint: '', models: [] }
    }
  };
})();

// React-хук — подписывается на изменения
window.useAiStore = function useAiStore() {
  const [s, setS] = React.useState(() => window.AiStore.get());
  React.useEffect(() => window.AiStore.subscribe(setS), []);
  return s;
};
