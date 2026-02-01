import { createI18n } from "vue-i18n";
import zhCN from "./zh-CN";
import enUS from "./en-US";

const messages = {
  "zh-CN": zhCN,
  "en-US": enUS
};

// 安全地获取 localStorage 中的语言设置
const getInitialLocale = () => {
  try {
    return localStorage.getItem("locale") || "zh-CN";
  } catch {
    return "zh-CN";
  }
};

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getInitialLocale(), // 默认语言
  fallbackLocale: "zh-CN", // 备用语言
  fallbackRoot: true, // 在找不到翻译时回退到根语言
  globalInjection: true, // 全局注入 $t、$rt、$d 等方法
  missingWarn: false, // 禁用缺失翻译警告
  fallbackWarn: false, // 禁用回退警告
  messages
});

export default i18n;
