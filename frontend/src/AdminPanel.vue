<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from './api'

const props = defineProps({
  knowledgeBaseId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['close'])

const users = ref([])
const totalPages = ref(0)
const totalElements = ref(0)
const page = ref(0)
const size = ref(10)
const username = ref('')
const password = ref('')
const nickname = ref('')
const role = ref('USER')
const status = ref('ACTIVE')
const searchUsername = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const grantPermission = ref('READ')
const batchText = ref('')
const csvInput = ref(null)
const message = ref('')
const selectedUserIds = ref([])
const grants = ref([])
const batchGrantPermission = ref('READ')

const grantedMap = computed(() => {
  const map = {}
  for (const grant of grants.value) {
    map[grant.userId] = grant.permission
  }
  return map
})

const allSelected = computed(() => {
  return (
    users.value.length > 0 &&
    users.value.every((user) => selectedUserIds.value.includes(user.id))
  )
})

onMounted(() => {
  loadUsers()
  loadGrants()
})

watch(
  () => props.knowledgeBaseId,
  () => {
    selectedUserIds.value = []
    loadGrants()
  }
)

async function loadUsers() {
  try {
    const response = await api.get('/users', {
      params: {
        page: page.value,
        size: size.value,
        username: searchUsername.value || undefined,
        role: filterRole.value || undefined,
        status: filterStatus.value || undefined
      }
    })
    const data = response.data || {}
    users.value = data.content || []
    totalPages.value = data.totalPages || 0
    totalElements.value = data.totalElements || 0
  } catch (error) {
    message.value = error.message
  }
}

function previousPage() {
  if (page.value > 0) {
    page.value--
    loadUsers()
  }
}

function nextPage() {
  if (page.value < totalPages.value - 1) {
    page.value++
    loadUsers()
  }
}

async function createUser() {
  if (!username.value.trim() || !password.value.trim()) {
    message.value = '请输入用户名和密码'
    return
  }

  try {
    await api.post('/users', {
      username: username.value.trim(),
      password: password.value,
      nickname: nickname.value.trim(),
      role: role.value,
      status: status.value
    })
    username.value = ''
    password.value = ''
    nickname.value = ''
    role.value = 'USER'
    status.value = 'ACTIVE'
    await loadUsers()
  } catch (error) {
    message.value = error.message
  }
}

async function batchCreateUsers() {
  const lines = batchText.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  if (lines.length === 0) {
    message.value = '请粘贴用户数据'
    return
  }

  const usersPayload = lines.map((line) => {
    const parts = line.split(/[,，\t]/).map((item) => item.trim())
    return {
      username: parts[0],
      password: parts[1] || '123456',
      nickname: parts[2] || '',
      role: parts[3] || 'USER',
      status: parts[4] || 'ACTIVE'
    }
  })

  try {
    const response = await api.post('/users/batch', { users: usersPayload })
    message.value = `成功 ${response.data.successCount} 条，失败 ${response.data.failureCount} 条`
    if (response.data.failures?.length) {
      message.value += '；' + response.data.failures.join('；')
    }
    batchText.value = ''
    await loadUsers()
  } catch (error) {
    message.value = error.message
  }
}

function onCsvChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    batchText.value = String(reader.result || '')
  }
  reader.readAsText(file)
}

async function updateUser(user, nextRole, nextStatus) {
  try {
    await api.put(`/users/${user.id}`, {
      role: nextRole,
      status: nextStatus
    })
    await loadUsers()
  } catch (error) {
    message.value = error.message
  }
}

async function deleteUser(user) {
  if (!window.confirm(`确定删除用户「${user.username}」吗？`)) return
  try {
    await api.delete(`/users/${user.id}`)
    await loadUsers()
  } catch (error) {
    message.value = error.message
  }
}

async function grant(user) {
  if (!props.knowledgeBaseId) {
    message.value = '请先选择知识库'
    return
  }
  try {
    await api.post(
      `/knowledge-bases/${props.knowledgeBaseId}/grants/${user.id}`,
      null,
      { params: { permission: grantPermission.value } }
    )
    await loadUsers()
    await loadGrants()
  } catch (error) {
    message.value = error.message
  }
}

async function revoke(user) {
  if (!props.knowledgeBaseId) {
    message.value = '请先选择知识库'
    return
  }
  try {
    await api.delete(`/knowledge-bases/${props.knowledgeBaseId}/grants/${user.id}`)
    await loadUsers()
    await loadGrants()
  } catch (error) {
    message.value = error.message
  }
}

async function loadGrants() {
  if (!props.knowledgeBaseId) {
    grants.value = []
    return
  }
  try {
    const response = await api.get(`/knowledge-bases/${props.knowledgeBaseId}/grants`)
    grants.value = response.data || []
  } catch (error) {
    message.value = error.message
  }
}

function toggleSelect(userId) {
  const index = selectedUserIds.value.indexOf(userId)
  if (index >= 0) {
    selectedUserIds.value.splice(index, 1)
  } else {
    selectedUserIds.value.push(userId)
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    const currentPageIds = new Set(users.value.map((user) => user.id))
    selectedUserIds.value = selectedUserIds.value.filter(
      (id) => !currentPageIds.has(id)
    )
  } else {
    const merged = new Set([...selectedUserIds.value, ...users.value.map((user) => user.id)])
    selectedUserIds.value = Array.from(merged)
  }
}

async function batchGrant() {
  if (!props.knowledgeBaseId) {
    message.value = '请先选择知识库'
    return
  }
  if (selectedUserIds.value.length === 0) {
    message.value = '请先勾选用户'
    return
  }

  try {
    const response = await api.post(
      `/knowledge-bases/${props.knowledgeBaseId}/permissions/batch`,
      {
        userIds: selectedUserIds.value,
        permission: batchGrantPermission.value
      }
    )
    message.value = `已授权 ${response.data.grantedCount} 个用户`
    if (response.data.invalidCount > 0) {
      message.value += `，${response.data.invalidCount} 个无效用户被忽略`
    }
    selectedUserIds.value = []
    await loadGrants()
  } catch (error) {
    message.value = error.message
  }
}

async function batchRevoke() {
  if (!props.knowledgeBaseId) {
    message.value = '请先选择知识库'
    return
  }
  if (selectedUserIds.value.length === 0) {
    message.value = '请先勾选用户'
    return
  }

  try {
    const response = await api.delete(
      `/knowledge-bases/${props.knowledgeBaseId}/permissions/batch`,
      { data: { userIds: selectedUserIds.value } }
    )
    message.value = `已取消授权 ${response.data.revokedCount} 个用户`
    selectedUserIds.value = []
    await loadGrants()
  } catch (error) {
    message.value = error.message
  }
}
</script>

<template>
  <div class="admin-mask">
    <div class="admin-panel">
      <div class="admin-head">
        <h2>用户与知识库授权</h2>
        <button @click="emit('close')">关闭</button>
      </div>

      <div class="admin-form">
        <input v-model="username" placeholder="用户名" />
        <input v-model="password" type="password" placeholder="密码，至少 6 位" />
        <input v-model="nickname" placeholder="昵称" />
        <select v-model="role">
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <select v-model="status">
          <option value="ACTIVE">ACTIVE</option>
          <option value="DISABLED">DISABLED</option>
        </select>
        <button class="primary" @click="createUser">新增用户</button>
      </div>

      <div class="admin-form">
        <input v-model="searchUsername" placeholder="按用户名搜索" @input="loadUsers" />
        <select v-model="filterRole" @change="page = 0; loadUsers()">
          <option value="">全部角色</option>
          <option value="ADMIN">ADMIN</option>
          <option value="USER">USER</option>
        </select>
        <select v-model="filterStatus" @change="page = 0; loadUsers()">
          <option value="">全部状态</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </div>

      <div class="admin-form">
        <textarea v-model="batchText" placeholder="批量新增：每行 用户名,密码,昵称,角色,状态" rows="3"></textarea>
        <button @click="batchCreateUsers">批量新增</button>
        <button @click="csvInput?.click()">上传 CSV</button>
        <input ref="csvInput" type="file" accept=".csv" hidden @change="onCsvChange" />
      </div>

      <div v-if="message" class="admin-message">{{ message }}</div>

      <div class="admin-head">
        <span>共 {{ totalElements }} 个用户</span>
        <div>
          <button :disabled="page <= 0" @click="previousPage">上一页</button>
          <span>第 {{ page + 1 }} / {{ Math.max(totalPages, 1) }} 页</span>
          <button :disabled="page >= totalPages - 1" @click="nextPage">下一页</button>
        </div>
      </div>

      <div class="batch-grant-toolbar">
        <label class="select-all">
          <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
          全选当前结果
        </label>
        <span>已选 {{ selectedUserIds.length }} 个 · 已授权 {{ grants.length }} 人</span>
        <select v-model="batchGrantPermission">
          <option value="READ">只读</option>
          <option value="EDIT">可编辑</option>
        </select>
        <button @click="batchGrant">批量授权</button>
        <button @click="batchRevoke">批量取消授权</button>
      </div>

      <div class="user-list">
        <div v-for="user in users" :key="user.id" class="user-card">
          <div class="user-check">
            <input
              type="checkbox"
              :checked="selectedUserIds.includes(user.id)"
              @change="toggleSelect(user.id)"
            />
          </div>
          <div>
            <div>{{ user.username }}</div>
            <small>{{ user.nickname || '未设置昵称' }} · {{ user.role }} · {{ user.status }}</small>
            <span v-if="grantedMap[user.id]" class="grant-badge">
              {{ grantedMap[user.id] === 'EDIT' ? '已授权(可编辑)' : '已授权(只读)' }}
            </span>
          </div>
          <div class="user-actions">
            <select :value="user.role" @change="updateUser(user, $event.target.value, user.status)">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <select :value="user.status" @change="updateUser(user, user.role, $event.target.value)">
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
            <select v-model="grantPermission">
              <option value="READ">只读</option>
              <option value="EDIT">可编辑</option>
            </select>
            <button @click="grant(user)">授权当前 KB</button>
            <button @click="revoke(user)">取消授权</button>
            <button @click="deleteUser(user)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-mask {
  position: fixed;
  inset: 0;
  background: rgba(24, 42, 73, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.admin-panel {
  width: 760px;
  height: 640px;
  min-width: 560px;
  max-width: 92vw;
  min-height: 360px;
  max-height: 92vh;
  overflow: auto;
  resize: both;
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 12px 40px rgba(55, 75, 120, 0.18);
}

.admin-head,
.user-card,
.user-actions,
.admin-form {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-head {
  justify-content: space-between;
}

.admin-form {
  flex-wrap: wrap;
  margin: 14px 0;
  padding: 12px;
  background: #f8faff;
  border: 1px solid #e3eaf6;
  border-radius: 12px;
}

.admin-form input {
  min-width: 180px;
  flex: 1;
}

input,
select,
textarea {
  padding: 8px 10px;
  border: 1px solid #dce4f0;
  border-radius: 8px;
}

textarea {
  width: 100%;
  resize: vertical;
}

.user-card {
  justify-content: space-between;
  border: 1px solid #e3eaf6;
  border-radius: 10px;
  padding: 10px 12px;
  margin-top: 8px;
}

.admin-message {
  color: #b9556c;
  margin: 8px 0;
}

.batch-grant-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
  padding: 10px 12px;
  background: #f8faff;
  border: 1px solid #e3eaf6;
  border-radius: 12px;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #33415c;
  cursor: pointer;
}

.user-check {
  flex-shrink: 0;
}

.grant-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 7px;
  font-size: 11px;
  border-radius: 999px;
  background: #eef1fa;
  color: #4358b3;
}
</style>
