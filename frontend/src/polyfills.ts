// Ensure dependencies expecting a Node-style global can run in the browser.
const globalScope: Record<string, unknown> =
  typeof globalThis !== 'undefined'
    ? globalThis
    : typeof self !== 'undefined'
      ? self
      : typeof window !== 'undefined'
        ? window
        : {};

if (typeof globalScope.global === 'undefined') {
  globalScope.global = globalScope;
}

export {};
