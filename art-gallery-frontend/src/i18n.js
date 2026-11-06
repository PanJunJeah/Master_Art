import { createI18n } from 'vue-i18n'
import zh from './locales/zh-CN.js'
import en from './locales/en-US.js'
import {createApp} from "vue";
import App from "./App.vue";

const i18n = createI18n({
  legacy: false, // 如果你使用的是vue-i18n v9, 必须设置为false
  locale: 'zh', // 设置默认语言
  messages: {
    zh: zh,
    en: en
  }
})

export default i18n