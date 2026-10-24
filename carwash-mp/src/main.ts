import { createSSRApp } from "vue";
import App from "./App.vue";
import AppImage from "@/components/AppImage.vue";
import "@/styles/theme.css";
export function createApp() {
  const app = createSSRApp(App);
  // 全局注册带失败占位的图片组件，替换散落的 <image>（D2：图片加载失败无兜底）
  app.component("AppImage", AppImage);
  return {
    app,
  };
}
