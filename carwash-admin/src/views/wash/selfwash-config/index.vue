<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon class="tip">
      <template #title>
        自助洗车按<b>计时收费</b>（无感洗车按次/套餐收费，见「服务项」里的入口场景）。
        修改后立即生效，只影响之后新建的自助订单；已建的订单仍用当时的价格快照。
      </template>
    </el-alert>

    <el-card shadow="never" v-loading="loading">
      <el-form ref="pricingRef" :model="form" :rules="rules" label-width="180px" style="max-width: 620px">
        <el-divider content-position="left">基础收费</el-divider>
        <el-form-item label="基础金额(元)" prop="baseYuan">
          <el-input-number v-model="form.baseYuan" :min="0" :precision="2" :step="1" style="width: 220px" />
          <span class="hint">下单即收，含 {{ form.baseMinutes }} 分钟</span>
        </el-form-item>
        <el-form-item label="基础时长(分钟)" prop="baseMinutes">
          <el-input-number v-model="form.baseMinutes" :min="1" :step="5" style="width: 220px" />
        </el-form-item>

        <el-divider content-position="left">超时收费</el-divider>
        <el-form-item label="超时金额(元)" prop="overtimeYuan">
          <el-input-number v-model="form.overtimeYuan" :min="0" :precision="2" :step="1" style="width: 220px" />
          <span class="hint">每 {{ form.overtimeMinutes }} 分钟一档，不足一档按一档</span>
        </el-form-item>
        <el-form-item label="超时时长(分钟)" prop="overtimeMinutes">
          <el-input-number v-model="form.overtimeMinutes" :min="1" :step="5" style="width: 220px" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm">保 存</el-button>
          <el-button @click="load">重 置</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions :column="2" border size="small" class="preview" title="C 端展示效果">
        <el-descriptions-item label="基础收费">
          ¥{{ yuan(form.baseYuan) }} / {{ form.baseMinutes }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="超时收费">
          ¥{{ yuan(form.overtimeYuan) }} / {{ form.overtimeMinutes }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="示例：45 分钟">
          ¥{{ yuan(form.baseYuan) }} + ¥{{ yuan(form.overtimeYuan) }} = ¥{{ yuan(example45) }}
        </el-descriptions-item>
        <el-descriptions-item label="示例：60 分钟">
          ¥{{ yuan(form.baseYuan) }} + {{ overtimeBlocks(60) }} 档 = ¥{{ yuan(example60) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="WashSelfwashConfig">
import { getSelfwashPricing, saveSelfwashPricing } from '@/api/wash/selfwash-config'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const data = reactive({
  form: {
    baseYuan: 20,
    baseMinutes: 30,
    overtimeYuan: 8,
    overtimeMinutes: 10
  },
  rules: {
    baseYuan: [{ required: true, message: '基础金额不能为空', trigger: 'blur' }],
    baseMinutes: [{ required: true, message: '基础时长不能为空', trigger: 'blur' }],
    overtimeYuan: [{ required: true, message: '超时金额不能为空', trigger: 'blur' }],
    overtimeMinutes: [{ required: true, message: '超时时长不能为空', trigger: 'blur' }]
  }
})

const { form, rules } = toRefs(data)

/** 金额：后端存分，页面用元，只在提交/回显时换算 */
function yuan(v) {
  return Number(v || 0).toFixed(2)
}

/** 超时分钟数 → 档数（不足一档按一档） */
function overtimeBlocks(totalMinutes) {
  const base = Number(form.value.baseMinutes) || 0
  const per = Number(form.value.overtimeMinutes) || 0
  if (base <= 0 || per <= 0) return 0
  const extra = Math.max(0, totalMinutes - base)
  return Math.ceil(extra / per)
}

/** 45 分钟总价（分→元前用元算，展示用） */
const example45 = computed(() => {
  const blocks = overtimeBlocks(45)
  return Number(form.value.baseYuan || 0) + blocks * Number(form.value.overtimeYuan || 0)
})

const example60 = computed(() => {
  const blocks = overtimeBlocks(60)
  return Number(form.value.baseYuan || 0) + blocks * Number(form.value.overtimeYuan || 0)
})

function load() {
  loading.value = true
  getSelfwashPricing().then(res => {
    const d = res.data || {}
    form.value = {
      baseYuan: Number(((d.baseAmount || 0) / 100).toFixed(2)),
      baseMinutes: d.baseMinutes || 30,
      overtimeYuan: Number(((d.overtimeAmount || 0) / 100).toFixed(2)),
      overtimeMinutes: d.overtimeMinutes || 10
    }
    loading.value = false
  })
}

function submitForm() {
  proxy.$refs.pricingRef.validate(valid => {
    if (!valid) return
    // 元 → 分：金额一律以「分」传给后端
    saveSelfwashPricing({
      baseAmount: Math.round((form.value.baseYuan || 0) * 100),
      baseMinutes: form.value.baseMinutes,
      overtimeAmount: Math.round((form.value.overtimeYuan || 0) * 100),
      overtimeMinutes: form.value.overtimeMinutes
    }).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      load()
    })
  })
}

load()
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}

.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.preview {
  margin-top: 16px;
  max-width: 620px;
}
</style>
