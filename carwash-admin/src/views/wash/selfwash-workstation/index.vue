<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="所属站点">
        <el-select v-model="query.siteId" placeholder="全部站点" clearable filterable style="width: 220px">
          <el-option v-for="s in siteOpts" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="工位编号 / 名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:selfwash:add']">新增</el-button>
    </el-row>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="工位编号" prop="code" width="120" />
      <el-table-column label="工位名称" prop="name" min-width="140" show-overflow-tooltip />
      <el-table-column label="所属站点" min-width="160">
        <template #default="{ row }">{{ siteName(row.siteId) }}</template>
      </el-table-column>
      <el-table-column label="启用" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 'Y' ? 'success' : 'info'">{{ row.enabled === 'Y' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)" v-hasPermi="['wash:selfwash:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDel(row)" v-hasPermi="['wash:selfwash:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="open" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属站点" prop="siteId">
          <el-select v-model="form.siteId" placeholder="请选择站点" filterable style="width: 100%">
            <el-option v-for="s in siteOpts" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工位编号" prop="code">
          <el-input v-model="form.code" placeholder="如 A1 / A2 / A3" maxlength="32" />
        </el-form-item>
        <el-form-item label="工位名称" prop="name">
          <el-input v-model="form.name" placeholder="选填，如 1 号高压工位" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio value="Y">启用</el-radio>
            <el-radio value="N">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SelfwashWorkstation">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWorkstations, addWorkstation, updateWorkstation, delWorkstation } from '@/api/wash/selfwash'
import { siteOptions as fetchSiteOptions } from '@/api/wash/site'

const loading = ref(false)
const list = ref([])
const siteOpts = ref([])
const open = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const editId = ref(null)

const query = reactive({ siteId: undefined, keyword: undefined })
const form = reactive({ siteId: undefined, code: '', name: '', enabled: 'Y', sortOrder: 0 })
const rules = {
  siteId: [{ required: true, message: '请选择所属站点', trigger: 'change' }],
  code: [{ required: true, message: '工位编号必填', trigger: 'blur' }]
}

function siteName(id) {
  return siteOpts.value.find((s) => s.value === id)?.label || `站点${id}`
}

function loadSites() {
  fetchSiteOptions()
    .then((res) => { siteOpts.value = res.data || [] })
    .catch(() => {})
}

function load() {
  loading.value = true
  listWorkstations({ siteId: query.siteId, keyword: query.keyword })
    .then((res) => { list.value = res.data || [] })
    .finally(() => { loading.value = false })
}

function handleQuery() {
  load()
}

function resetQuery() {
  query.siteId = undefined
  query.keyword = undefined
  load()
}

function handleAdd() {
  editId.value = null
  dialogTitle.value = '新增工位'
  form.siteId = query.siteId
  form.code = ''
  form.name = ''
  form.enabled = 'Y'
  form.sortOrder = 0
  open.value = true
}

function handleEdit(row) {
  editId.value = row.workstationId
  dialogTitle.value = '修改工位'
  form.siteId = row.siteId
  form.code = row.code
  form.name = row.name || ''
  form.enabled = row.enabled || 'Y'
  form.sortOrder = row.sortOrder || 0
  open.value = true
}

function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return
    const payload = { siteId: form.siteId, code: form.code, name: form.name, enabled: form.enabled, sortOrder: form.sortOrder }
    const done = () => { ElMessage.success('保存成功'); open.value = false; load() }
    if (editId.value) {
      updateWorkstation(editId.value, payload).then(done)
    } else {
      addWorkstation(payload).then(done)
    }
  })
}

function handleDel(row) {
  ElMessageBox.confirm(`确认删除工位「${row.code}」？`, '提示', { type: 'warning' })
    .then(() => delWorkstation(row.workstationId))
    .then(() => { ElMessage.success('删除成功'); load() })
    .catch(() => {})
}

onMounted(() => {
  loadSites()
  load()
})
</script>

<style scoped>
.query-form {
  margin-bottom: 8px;
}
.mb8 {
  margin-bottom: 12px;
}
</style>
