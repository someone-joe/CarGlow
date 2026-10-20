<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { ensureLogin } from "@/api/auth";
import { refreshPhoneBound } from "@/store/auth";
import PhoneAuthModal from "@/components/PhoneAuthModal.vue";

onLaunch(() => {
  // 启动即静默登录（失败不阻塞，接口层发现未登录会自动重试一次）
  ensureLogin().catch((e) => {
    console.warn("[启动] 静默登录失败：", e);
  });
  // 刷新手机号绑定态，供按需授权门控判断是否放行（不弹窗）
  refreshPhoneBound();
});
</script>

<template>
  <view>
    <PhoneAuthModal />
  </view>
</template>

<style></style>
