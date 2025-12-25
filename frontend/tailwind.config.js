/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cyber: {
          black: '#0a0a0f',
          dark: '#12121a',
          gray: '#1a1a2e',
          purple: '#b026ff',
          pink: '#ff2d95',
          cyan: '#00f5ff',
          blue: '#4d4dff',
          green: '#39ff14',
          yellow: '#ffff00',
        }
      },
      boxShadow: {
        'neon-purple': '0 0 5px #b026ff, 0 0 10px #b026ff, 0 0 20px #b026ff',
        'neon-pink': '0 0 5px #ff2d95, 0 0 10px #ff2d95, 0 0 20px #ff2d95',
        'neon-cyan': '0 0 5px #00f5ff, 0 0 10px #00f5ff, 0 0 20px #00f5ff',
        'neon-blue': '0 0 5px #4d4dff, 0 0 10px #4d4dff, 0 0 20px #4d4dff',
        'neon-green': '0 0 5px #39ff14, 0 0 10px #39ff14, 0 0 20px #39ff14',
      },
      animation: {
        'glow-pulse': 'glow-pulse 2s ease-in-out infinite alternate',
        'flicker': 'flicker 3s linear infinite',
      },
      keyframes: {
        'glow-pulse': {
          '0%': { filter: 'brightness(1)' },
          '100%': { filter: 'brightness(1.3)' },
        },
        'flicker': {
          '0%, 19%, 21%, 23%, 25%, 54%, 56%, 100%': { opacity: '1' },
          '20%, 24%, 55%': { opacity: '0.8' },
        },
      },
      spacing: {
        '0.25': '0.0625rem',   // 1px
        '0.5': '0.125rem',     // 2px
        '1': '0.25rem',         // 4px
        '1.5': '0.375rem',      // 6px
        '2': '0.5rem',          // 8px
        '2.5': '0.625rem',      // 10px
        '3': '0.75rem',         // 12px
        '4': '1rem',            // 16px
        '5': '1.25rem',         // 20px
        '6': '1.5rem',          // 24px
        '8': '2rem',            // 32px
        '10': '2.5rem',         // 40px
        '12': '3rem',           // 48px
        '16': '4rem',           // 64px
      },
    },
  },
  plugins: [],
}
