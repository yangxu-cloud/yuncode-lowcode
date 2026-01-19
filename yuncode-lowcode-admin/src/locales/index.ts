import { createI18n } from "vue-i18n";
import zhCN from "./zh-CN";
import enUS from "./en-US";

const messages = {
  "zh-CN": zhCN,
  "en-US": enUS
};

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: localStorage.getItem("locale") || "zh-CN", // 默认语言
  fallbackLocale: "zh-CN", // 备用语言
  messages
});

export default i18n;
