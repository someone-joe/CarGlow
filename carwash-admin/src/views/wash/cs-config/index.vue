<template>
  <div class="app-container">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" v-loading="loading">
      <el-divider content-position="left">基础信息</el-divider>

      <el-form-item label="平台客服电话" prop="platformPhone">
        <el-input v-model="form.platformPhone" placeholder="未填则 C 端兜底 10086" maxlength="20" />
        <div class="tip">任何情况下 C 端都拨得出去；留空时小程序用兜底值 10086。</div>
      </el-form-item>

      <el-form-item label="站点专属电话" prop="stationPhone">
        <el-input v-model="form.stationPhone" placeholder="选填，仅本站点客服" maxlength="20" />
      </el-form-item>

      <el-form-item label="夜间提示" prop="nightTip">
        <el-input v-model="form.nightTip" type="textarea" :rows="2" placeholder="选填，例如：夜间 22:00-07:00 仅急单受理" maxlength="200" />
      </el-form-item>

      <el-form-item label="企微二维码URL" prop="wecomQrcodeUrl">
        <el-input v-model="form.wecomQrcodeUrl" placeholder="选填，填了才展示「专属管家」卡片" maxlength="500" />
      </el-form-item>

      <el-divider content-position="left">您可能会遇到的问题</el-divider>

      <el-form-item label-width="0">
        <div class="faq-box">
          <div v-for="(item, index) in form.faqs" :key="index" class="faq-row">
            <span class="idx">{{ index + 1 }}</span>
            <el-input v-model="item.question" placeholder="问题，例如：无法打开柜门" maxlength="255" class="q" />
            <el-input v-model="item.answer" placeholder="解答，选填；留空则小程序弹通用占位" maxlength="1000" class="a" />
            <el-button type="danger" link icon="Delete" @click="removeFaq(index)" />
          </div>
          <el-button type="primary" plain icon="Plus" @click="addFaq">新增一条</el-button>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submitForm" v-hasPermi="['wash:cs:edit']">保存配置</el-button>
        <el-button @click="load">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup name="CsConfig">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCsConfig, saveCsConfig } from '@/api/wash/cs-config'

const loading = ref(false)
const formRef = ref()
const form = reactive({
  platformPhone: '',
  stationPhone: '',
  nightTip: '',
  wecomQrcodeUrl: '',
  faqs: []
})

const rules = {
  platformPhone: [{ pattern: /^[\d\-+\s]{0,20}$/, message: '电话格式不合法', trigger: 'blur' }],
  stationPhone: [{ pattern: /^[\d\-+\s]{0,20}$/, message: '电话格式不合法', trigger: 'blur' }]
}

function load() {
  loading.value = true
  getCsConfig().then((res) => {
    const data = res.data || {}
    form.platformPhone = data.platformPhone || ''
    form.stationPhone = data.stationPhone || ''
    form.nightTip = data.nightTip || ''
    form.wecomQrcodeUrl = data.wecomQrcodeUrl || ''
    form.faqs = Array.isArray(data.faqs) ? data.faqs.map((f) => ({ question: f.question || '', answer: f.answer || '' })) : []
  }).finally(() => {
    loading.value = false
  })
}

function addFaq() {
  form.faqs.push({ question: '', answer: '' })
}

function removeFaq(index) {
  form.faqs.splice(index, 1)
}

function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return
    saveCsConfig({
      platformPhone: form.platformPhone,
      stationPhone: form.stationPhone,
      nightTip: form.nightTip,
      wecomQrcodeUrl: form.wecomQrcodeUrl,
      faqs: form.faqs
    }).then(() => {
      ElMessage.success('保存成功')
    })
  })
}

onMounted(load)
</script>

<style scoped>
.tip {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin-top: 4px;
}
.faq-box {
  width: 100%;
}
.faq-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.faq-row .idx {
  width: 24px;
  text-align: center;
  color: #888;
}
.faq-row .q {
  width: 320px;
}
.faq-row .a {
  flex: 1;
}
</style>
