import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';

mermaid.initialize({
  startOnLoad: true,
  theme: 'base', // use 'base' to apply custom themeVariables
  themeVariables: {
    background: '#f3f4f6',       // light card background
    primaryColor: '#ffffff',      // node background
    primaryTextColor: '#111827',  // dark text
    lineColor: '#3b82f6',         // vibrant blue for connectors
    secondaryColor: '#e5e7eb',    // light accent for sub-elements
    border1: '#d1d5db',           // subtle border
    fontFamily: 'Inter, Roboto, Arial, sans-serif'
  }
});