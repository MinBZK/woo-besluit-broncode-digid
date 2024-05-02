import i18next from 'i18next';
import resources from './resources';
import LanguageDetector from 'i18next-browser-languagedetector';

i18next.use(LanguageDetector).init({
  fallbackLng: 'nl',
  resources,
});

export default i18next;
