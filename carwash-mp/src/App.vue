<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { ensureLogin } from "@/api/auth";

onLaunch(() => {
  // 启动即静默登录（失败不阻塞，接口层发现未登录会自动重试一次）
  ensureLogin().catch((e) => {
    console.warn("[启动] 静默登录失败：", e);
  });
});
</script>
<style></style>
