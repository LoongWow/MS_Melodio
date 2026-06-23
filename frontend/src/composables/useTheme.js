import { ref } from 'vue';

const theme = ref(localStorage.getItem('melodio_theme') || 'dark');

export const useTheme = () => {
  const toggleTheme = (t) => {
    theme.value = t;
    localStorage.setItem('melodio_theme', t);
  };
  return { theme, toggleTheme };
};
