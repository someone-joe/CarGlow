<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="bannerList">
      <el-table-column label="排序" prop="sortOrder" width="70" />
      <el-table-column label="标题" prop="title" min-width="120" show-overflow-tooltip />
      <el-table-column label="副标题" prop="subtitle" min-width="180" show-overflow-tooltip />
      <el-table-column label="图片" width="120">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width: 80px; height: 40px" fit="cover" :preview-src-list="[row.imageUrl]" preview-teleported />
          <el-tag v-else type="info" size="small">文字图</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跳转" width="140">
        <template #default="{ row }">
          <span v-if="row.linkType === 'SERVICE'">服务 #{{ row.linkValue }}</span>
          <span v-else>{{ linkTypeLabel(row.linkType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 'Y' ? 'success' : 'info'" size="small">
            {{ row.enabled === 'Y' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="bannerRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：无感" maxlength="64" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" placeholder="如：到店即走，无需排队等待（无图时展示）" maxlength="128" />
        </el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="留空则渲染「标题+副标题」文字图" />
        </el-form-item>
        <el-form-item label="跳转类型" prop="linkType">
          <el-select v-model="form.linkType" style="width: 100%">
            <el-option v-for="t in linkTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.linkType === 'SERVICE'" label="服务ID" prop="linkValue">
          <el-input v-model="form.linkValue" placeholder="跳转到该服务详情，如 101" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" style="width: 100%" />
          <div class="hint">数字越小越靠前</div>
        </el-form-item>
        <el-form-item label="上架状态" prop="enabled">
          <el-switch v-model="form.enabled" active-value="Y" inactive-value="N" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item label="生效时间" prop="timeRange">
          <el-date-picker v-model="form.timeRange" type="datetimerange" range-separator="~" start-placeholder="开始" end-placeholder="结束" value-format="x" :clearable="true" style="width: 100%" />
          <div class="hint">留空表示长期有效</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashBanner">
import { listBanner, addBanner, updateBanner, delBanner } from '@/api/wash/banner'

const { proxy } = getCurrentInstance()

const bannerList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined
  },
  rules: {
    title: [{ required: true, message: '标题不能为空', trigger: 'blur' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

const linkTypeOptions = [
  { value: 'NONE', label: '不跳转' },
  { value: 'RECHARGE', label: '储值充值' },
  { value: 'SERVICE', label: '服务详情' },
  { value: 'ACTIVITY', label: '活动页（待建）' }
]

function linkTypeLabel(t) {
  const hit = linkTypeOptions.find((x) => x.value === t)
  return hit ? hit.label : t || '-'
}

function getList() {
  loading.value = true
  listBanner(queryParams.value).then(res => {
    bannerList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    bannerId: undefined,
    title: undefined,
    subtitle: undefined,
    imageUrl: undefined,
    linkType: 'NONE',
    linkValue: undefined,
    sortOrder: 0,
    enabled: 'Y',
    timeRange: undefined
  }
  proxy.resetForm('bannerRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增 Banner'
}

function handleUpdate(row) {
  reset()
  form.value = {
    bannerId: row.bannerId,
    title: row.title,
    subtitle: row.subtitle,
    imageUrl: row.imageUrl,
    linkType: row.linkType || 'NONE',
    linkValue: row.linkValue,
    sortOrder: row.sortOrder ?? 0,
    enabled: row.enabled || 'Y',
    // 时间区间(毫秒) ↔ el-date-picker value-format="x"
    timeRange: row.startTime && row.endTime ? [row.startTime, row.endTime] : undefined
  }
  open.value = true
  title.value = '修改 Banner'
}

function submitForm() {
  proxy.$refs.bannerRef.validate(valid => {
    if (!valid) return
    const range = form.value.timeRange
    const payload = {
      bannerId: form.value.bannerId,
      title: form.value.title,
      subtitle: form.value.subtitle || null,
      imageUrl: form.value.imageUrl || null,
      linkType: form.value.linkType,
      linkValue: form.value.linkType === 'SERVICE' ? (form.value.linkValue || null) : null,
      sortOrder: form.value.sortOrder ?? 0,
      enabled: form.value.enabled,
      startTime: range ? range[0] : null,
      endTime: range ? range[1] : null
    }
    if (payload.bannerId) {
      updateBanner(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addBanner(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除 Banner「${row.title}」？`).then(() => {
    return delBanner(row.bannerId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

getList()
</script>

<style scoped>
.hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
