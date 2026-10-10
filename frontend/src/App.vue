<script setup>
import { computed, onMounted, ref } from 'vue'
import api from './api'
import AdminPanel from './AdminPanel.vue'

const knowledgeBases = ref([])
const conversations = ref([])
const messages = ref([])
const documents = ref([])

const selectedKnowledgeBaseId = ref(null)
const selectedConversationId = ref(null)
const selectedKnowledgeBase = ref(null)
const selectedConversation = ref(null)

const question = ref('')
const loading = ref(false)
const errorMessage = ref('')
const uploading = ref(false)
const fileInput = ref(null)
const reindexing = ref(false)
const reindexingDocumentId = ref(null)

const showKnowledgeBaseForm = ref(false)
const knowledgeBaseName = ref('')
const knowledgeBaseDescription = ref('')
const currentUser = ref(null)
const loginUsername = ref('')
const loginPassword = ref('')
const showAdminPanel = ref(false)
const editingConversationId = ref(null)
const editingTitle = ref('')
const menuConversationId = ref(null)
const uploadQueue = ref([])
const knowledgeBasesCollapsed = ref(false)
const knowledgeBaseFilter = ref('ALL')
const knowledgeBaseSearch = ref('')
const documentsCollapsed = ref(true)

let uploadSeq = 0
let conversationsRequestSeq = 0
let documentsRequestSeq = 0

const filteredKnowledgeBases = computed(() => {
  const keyword = knowledgeBaseSearch.value.trim().toLowerCase()
  return knowledgeBases.value.filter((kb) => {
    if (knowledgeBaseFilter.value === 'PUBLIC' && kb.visibility !== 'PUBLIC') {
      return false
    }
    if (knowledgeBaseFilter.value === 'PRIVATE' && kb.visibility !== 'PRIVATE') {
      return false
    }
    if (keyword && !(kb.name || '').toLowerCase().includes(keyword)) {
      return false
    }
    return true
  })
})

onMounted(async () => {
  await loadCurrentUser()
})

async function loadCurrentUser() {
  try {
    const response = await api.get('/auth/me')
    currentUser.value = response.code === 401 ? null : response.data
    if (currentUser.value) {
      await loadKnowledgeBases()
    }
  } catch (error) {
    currentUser.value = null
  }
}

async function login() {
  if (!loginUsername.value.trim() || !loginPassword.value) {
    showError('请输入用户名和密码')
    return
  }

  try {
    const response = await api.post('/auth/login', {
      username: loginUsername.value.trim(),
      password: loginPassword.value
    })
    currentUser.value = response.data
    loginPassword.value = ''
    await loadKnowledgeBases()
  } catch (error) {
    showError(error.message)
  }
}

async function logout() {
  try {
    await api.post('/auth/logout')
  } finally {
    currentUser.value = null
    selectedKnowledgeBaseId.value = null
    selectedKnowledgeBase.value = null
    selectedConversationId.value = null
    selectedConversation.value = null
    knowledgeBases.value = []
    conversations.value = []
    documents.value = []
    messages.value = []
    uploadQueue.value = []
  }
}

async function loadKnowledgeBases() {
  try {
    const response = await api.get('/knowledge-bases')
    knowledgeBases.value = response.data || []

    if (knowledgeBases.value.length > 0 && !selectedKnowledgeBaseId.value) {
      await selectKnowledgeBase(knowledgeBases.value[0])
    }
  } catch (error) {
    showError(error.message)
  }
}

async function selectKnowledgeBase(knowledgeBase) {
  selectedKnowledgeBase.value = knowledgeBase
  selectedKnowledgeBaseId.value = knowledgeBase.id
  selectedConversation.value = null
  selectedConversationId.value = null
  messages.value = []
  conversations.value = []
  documents.value = []
  await loadConversations()
  await loadDocuments()
}

async function loadConversations() {
  const kbId = selectedKnowledgeBaseId.value
  if (!kbId) {
    conversations.value = []
    return
  }

  const seq = ++conversationsRequestSeq
  try {
    const response = await api.get('/conversations')
    if (seq !== conversationsRequestSeq) {
      return
    }
    conversations.value = (response.data || []).filter(
      (conversation) => conversation.knowledgeBaseId === kbId
    )
  } catch (error) {
    if (seq === conversationsRequestSeq) {
      conversations.value = []
      showError(error.message)
    }
  }
}

async function loadDocuments() {
  const kbId = selectedKnowledgeBaseId.value
  if (!kbId) {
    documents.value = []
    return
  }

  const seq = ++documentsRequestSeq
  try {
    const response = await api.get(`/knowledge-bases/${kbId}/documents`)
    if (seq !== documentsRequestSeq) {
      return
    }
    documents.value = response.data || []
  } catch (error) {
    if (seq === documentsRequestSeq) {
      documents.value = []
      showError(error.message)
    }
  }
}

function formatFileSize(bytes) {
  if (!bytes) {
    return ''
  }
  if (bytes < 1024) {
    return bytes + ' B'
  }
  if (bytes < 1024 * 1024) {
    return (bytes / 1024).toFixed(1) + ' KB'
  }
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function onFilesSelected(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''

  if (!selectedKnowledgeBaseId.value) {
    showError('请先选择知识库')
    return
  }
  if (files.length === 0) {
    return
  }

  documentsCollapsed.value = false

  for (const file of files) {
    const isPdf = file.name.toLowerCase().endsWith('.pdf')
    uploadQueue.value.push({
      id: ++uploadSeq,
      knowledgeBaseId: selectedKnowledgeBaseId.value,
      fileName: file.name,
      file,
      size: formatFileSize(file.size),
      status: isPdf ? 'pending' : 'error',
      invalid: !isPdf,
      error: isPdf ? '' : '仅支持 PDF 文件'
    })
  }
}

async function uploadSingleFile(item) {
  if (item.status === 'success' || item.status === 'uploading') {
    return
  }

  item.status = 'uploading'
  item.error = ''

  const formData = new FormData()
  formData.append('file', item.file)

  try {
    const response = await api.post(
      `/knowledge-bases/${item.knowledgeBaseId}/documents`,
      formData
    )
    item.status = 'success'
    item.documentStatus = response.data.status || 'UPLOADED'
  } catch (error) {
    item.status = 'error'
    item.error = error.message
  }
}

async function startBatchUpload() {
  if (!selectedKnowledgeBaseId.value) {
    showError('请先选择知识库')
    return
  }

  uploading.value = true
  errorMessage.value = ''

  try {
    for (const item of uploadQueue.value) {
      if ((item.status === 'pending' || item.status === 'error') && !item.invalid) {
        await uploadSingleFile(item)
      }
    }
    await loadDocuments()
  } finally {
    uploading.value = false
  }
}

async function retryFile(item) {
  item.status = 'pending'
  item.error = ''
  uploading.value = true
  try {
    await uploadSingleFile(item)
    await loadDocuments()
  } finally {
    uploading.value = false
  }
}

function removeQueueItem(item) {
  uploadQueue.value = uploadQueue.value.filter((entry) => entry.id !== item.id)
}

function clearUploadQueue() {
  uploadQueue.value = uploadQueue.value.filter((entry) => entry.status === 'uploading')
}

function uploadStatusLabel(status) {
  if (status === 'pending') {
    return '待上传'
  }
  if (status === 'uploading') {
    return '上传中'
  }
  if (status === 'success') {
    return '成功'
  }
  if (status === 'error') {
    return '失败'
  }
  return status || '未知'
}

async function deleteDocument(document) {
  if (!window.confirm(`确定删除文档「${document.fileName}」吗？`)) {
    return
  }

  try {
    await api.delete(`/knowledge-bases/${selectedKnowledgeBaseId.value}/documents/${document.id}`)
    await loadDocuments()
  } catch (error) {
    showError(error.message)
  }
}

async function reindexKnowledgeBase() {
  if (!selectedKnowledgeBaseId.value) {
    showError('请先选择知识库')
    return
  }

  reindexing.value = true
  errorMessage.value = ''

  try {
    await api.post(`/knowledge-bases/${selectedKnowledgeBaseId.value}/reindex`)
    showError('索引任务已提交，请稍后提问')
  } catch (error) {
    showError(error.message)
  } finally {
    reindexing.value = false
  }
}

function statusLabel(status) {
  if (status === 'UPLOADED') {
    return '已上传'
  }
  if (status === 'PARSING') {
    return '解析中'
  }
  if (status === 'INDEXING') {
    return '索引中'
  }
  if (status === 'COMPLETED') {
    return '已完成'
  }
  if (status === 'PROCESSING') {
    return '处理中'
  }
  if (status === 'FAILED') {
    return '失败'
  }
  return status || '未知'
}

async function reindexDocument(document) {
  if (!selectedKnowledgeBaseId.value) {
    showError('请先选择知识库')
    return
  }

  reindexingDocumentId.value = document.id
  errorMessage.value = ''

  try {
    await api.post(
      `/knowledge-bases/${selectedKnowledgeBaseId.value}/documents/${document.id}/reindex`
    )
    showError('文档重新索引任务已提交')
    await loadDocuments()
  } catch (error) {
    showError(error.message)
  } finally {
    reindexingDocumentId.value = null
  }
}

async function createKnowledgeBase() {
  if (!knowledgeBaseName.value.trim()) {
    showError('请输入知识库名称')
    return
  }

  try {
    const response = await api.post('/knowledge-bases', {
      name: knowledgeBaseName.value.trim(),
      description: knowledgeBaseDescription.value.trim()
    })

    showKnowledgeBaseForm.value = false
    knowledgeBaseName.value = ''
    knowledgeBaseDescription.value = ''
    selectedKnowledgeBaseId.value = null
    await loadKnowledgeBases()
    await selectKnowledgeBase(response.data)
  } catch (error) {
    showError(error.message)
  }
}

async function deleteKnowledgeBase(knowledgeBase) {
  if (
    !window.confirm(
      '确定删除该知识库吗？该操作会同时删除其中的所有文档及索引数据，且无法恢复。'
    )
  ) {
    return
  }

  try {
    await api.delete(`/knowledge-bases/${knowledgeBase.id}`)

    if (selectedKnowledgeBaseId.value === knowledgeBase.id) {
      selectedKnowledgeBaseId.value = null
      selectedKnowledgeBase.value = null
      selectedConversationId.value = null
      selectedConversation.value = null
      conversations.value = []
      documents.value = []
      messages.value = []
    }

    await loadKnowledgeBases()
  } catch (error) {
    showError(error.message)
  }
}

async function createConversation() {
  if (!selectedKnowledgeBaseId.value) {
    showError('请先选择知识库')
    return
  }

  try {
    const response = await api.post('/conversations', {
      knowledgeBaseId: selectedKnowledgeBaseId.value,
      title: '新对话'
    })

    await loadConversations()
    await selectConversation(response.data)
  } catch (error) {
    showError(error.message)
  }
}

async function selectConversation(conversation) {
  selectedConversation.value = conversation
  selectedConversationId.value = conversation.id
  messages.value = []
  await loadConversationDetail(conversation.id)
}

async function loadConversationDetail(conversationId) {
  try {
    const response = await api.get(`/conversations/${conversationId}`)
    selectedConversation.value = response.data
    messages.value = response.data.messages || []
  } catch (error) {
    showError(error.message)
  }
}

async function sendMessage() {
  const text = question.value.trim()
  if (!text) {
    return
  }
  if (!selectedConversationId.value) {
    showError('请先选择或新建会话')
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await api.post(`/conversations/${selectedConversationId.value}/chat`, {
      question: text,
      topK: 3
    })

    messages.value.push({
      role: 'user',
      content: text
    })
    messages.value.push({
      role: 'assistant',
      content: response.data.answer,
      sources: response.data.sources || []
    })

    question.value = ''
    await loadConversations()
    await loadConversationDetail(selectedConversationId.value)
  } catch (error) {
    showError(error.message)
  } finally {
    loading.value = false
  }
}

function startRename(conversation) {
  editingConversationId.value = conversation.id
  editingTitle.value = conversation.title || '新对话'
}

function cancelRename() {
  editingConversationId.value = null
  editingTitle.value = ''
}

async function confirmRename(conversation) {
  const title = editingTitle.value.trim()
  if (!title) {
    showError('标题不能为空')
    return
  }
  if (title.length > 50) {
    showError('标题不能超过 50 个字符')
    return
  }

  try {
    const response = await api.patch(`/conversations/${conversation.id}`, { title })
    conversation.title = response.data.title
    conversation.manualRenamed = response.data.manualRenamed
    if (selectedConversationId.value === conversation.id) {
      selectedConversation.value = {
        ...selectedConversation.value,
        title: response.data.title,
        manualRenamed: response.data.manualRenamed
      }
    }
    cancelRename()
    await loadConversations()
  } catch (error) {
    showError(error.message)
  }
}

function toggleConversationMenu(conversation) {
  menuConversationId.value =
    menuConversationId.value === conversation.id ? null : conversation.id
}

function closeConversationMenu() {
  menuConversationId.value = null
}

async function deleteConversation(conversation) {
  closeConversationMenu()

  if (
    !window.confirm(
      `确定删除对话「${conversation.title || '新对话'}」吗？该操作会同时删除该对话的聊天记录，且无法恢复。`
    )
  ) {
    return
  }

  try {
    await api.delete(`/conversations/${conversation.id}`)
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id)

    if (selectedConversationId.value === conversation.id) {
      selectedConversationId.value = null
      selectedConversation.value = null
      messages.value = []

      const remaining = conversations.value[0]
      if (remaining) {
        await selectConversation(remaining)
      }
    }
  } catch (error) {
    showError(error.message)
  }
}

function showError(message) {
  errorMessage.value = message || '请求失败，请稍后重试'
}
</script>

<template>
  <div v-if="!currentUser" class="login-page">
    <div class="login-card">
      <h1>企业智能知识库</h1>
      <p>请登录后使用知识库与 RAG 问答</p>
      <input v-model="loginUsername" placeholder="用户名" />
      <input
        v-model="loginPassword"
        type="password"
        placeholder="密码"
        @keydown.enter="login"
      />
      <button class="primary" @click="login">登录</button>
      <div v-if="errorMessage" class="login-error">{{ errorMessage }}</div>
    </div>
  </div>

  <template v-else>
  <div class="layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1>企业知识库</h1>
        <div class="current-user">
          <span>{{ currentUser?.nickname || currentUser?.username }}</span>
          <small>{{ currentUser?.role }}</small>
        </div>
        <button
          v-if="currentUser?.role === 'ADMIN'"
          class="secondary"
          @click="showAdminPanel = true"
        >
          用户管理
        </button>
        <button
          class="primary"
          @click="showKnowledgeBaseForm = !showKnowledgeBaseForm"
        >
          新建知识库
        </button>
        <button @click="logout">退出登录</button>
      </div>

      <div v-if="showKnowledgeBaseForm" class="form-card">
        <input v-model="knowledgeBaseName" placeholder="知识库名称" />
        <input v-model="knowledgeBaseDescription" placeholder="描述（可选）" />
        <div class="form-actions">
          <button class="primary" @click="createKnowledgeBase">创建</button>
          <button @click="showKnowledgeBaseForm = false">取消</button>
        </div>
      </div>

      <section class="panel">
        <div
          class="panel-title panel-title-toggle"
          @click="knowledgeBasesCollapsed = !knowledgeBasesCollapsed"
        >
          <span>知识库</span>
          <span class="collapse-icon">{{ knowledgeBasesCollapsed ? '▸' : '▾' }}</span>
        </div>

        <template v-if="!knowledgeBasesCollapsed">
          <div class="kb-toolbar">
            <input
              v-model="knowledgeBaseSearch"
              class="kb-search"
              placeholder="搜索知识库"
            />
            <select v-model="knowledgeBaseFilter" class="kb-filter">
              <option value="ALL">全部</option>
              <option value="PUBLIC">公有</option>
              <option value="PRIVATE">私有</option>
            </select>
          </div>
          <div v-if="knowledgeBases.length === 0" class="empty">暂无知识库</div>
          <div v-else-if="filteredKnowledgeBases.length === 0" class="empty">
            没有匹配的知识库
          </div>
          <div
            v-for="kb in filteredKnowledgeBases"
            :key="kb.id"
            class="kb-row"
            :class="{ active: selectedKnowledgeBaseId === kb.id }"
          >
            <button class="list-item" @click="selectKnowledgeBase(kb)">
              {{ kb.name }}
              <span class="kb-visibility">
                {{ kb.visibility === 'PUBLIC' ? '公有' : '私有' }}
              </span>
            </button>
            <button
              v-if="currentUser?.role === 'ADMIN' || kb.ownerId === currentUser?.id"
              class="icon-danger"
              @click="deleteKnowledgeBase(kb)"
            >
              删除
            </button>
          </div>
        </template>
      </section>

      <section class="panel">
        <div class="panel-title">会话</div>
        <button
          class="primary"
          :disabled="!selectedKnowledgeBaseId"
          @click="createConversation"
        >
          新建会话
        </button>
        <div v-if="conversations.length === 0" class="empty">暂无会话</div>
        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          class="conversation-row"
          :class="{ active: selectedConversationId === conversation.id }"
        >
          <template v-if="editingConversationId === conversation.id">
            <input
              v-model="editingTitle"
              class="conversation-edit-input"
              @keydown.enter.prevent="confirmRename(conversation)"
              @keydown.esc="cancelRename"
            />
            <button class="icon-confirm" @click="confirmRename(conversation)">保存</button>
            <button class="icon-danger" @click="cancelRename">取消</button>
          </template>
          <template v-else>
            <button class="list-item" @click="selectConversation(conversation)">
              {{ conversation.title || '新对话' }}
            </button>
            <div class="conversation-menu-wrap">
              <button class="icon-more" @click.stop="toggleConversationMenu(conversation)">⋯</button>
              <div v-if="menuConversationId === conversation.id" class="conversation-menu">
                <button @click.stop="startRename(conversation); closeConversationMenu()">
                  重命名
                </button>
                <button class="menu-danger" @click.stop="deleteConversation(conversation)">
                  删除
                </button>
              </div>
            </div>
          </template>
        </div>
      </section>
    </aside>

    <main class="chat">
      <header class="chat-header">
        <div>
          <div class="chat-title">
            {{ selectedConversation?.title || selectedKnowledgeBase?.name || 'RAG 问答' }}
          </div>
          <div class="chat-subtitle">
            {{ selectedKnowledgeBase?.name || '' }}
          </div>
        </div>
        <button
          class="primary"
          :disabled="!selectedKnowledgeBaseId || uploading"
          @click="fileInput?.click()"
        >
          {{ uploading ? '上传中...' : '上传 PDF' }}
        </button>
        <input
          ref="fileInput"
          type="file"
          accept=".pdf,application/pdf"
          hidden
          multiple
          @change="onFilesSelected"
        />
      </header>

      <section v-if="selectedKnowledgeBaseId" class="documents-panel">
        <div
          class="documents-title"
          :class="{ collapsed: documentsCollapsed }"
          @click="documentsCollapsed = !documentsCollapsed"
        >
          <span class="documents-title-main">
            <span class="collapse-icon">{{ documentsCollapsed ? '▸' : '▾' }}</span>
            文档
          </span>
          <div class="documents-actions">
            <span class="documents-count">{{ documents.length }} 个文档</span>
            <button
              class="secondary"
              :disabled="reindexing"
              @click.stop="reindexKnowledgeBase"
            >
              {{ reindexing ? '重建中...' : '重建索引' }}
            </button>
          </div>
        </div>
        <template v-if="!documentsCollapsed">
        <div v-if="uploadQueue.length > 0" class="upload-queue">
          <div class="upload-queue-head">
            <span>待上传文件（{{ uploadQueue.length }}）</span>
            <div class="upload-queue-head-actions">
              <button
                class="secondary"
                :disabled="uploading"
                @click="startBatchUpload"
              >
                {{ uploading ? '上传中...' : '开始上传' }}
              </button>
              <button class="secondary" :disabled="uploading" @click="clearUploadQueue">
                清空
              </button>
            </div>
          </div>
          <div v-for="item in uploadQueue" :key="item.id" class="upload-queue-item">
            <div class="upload-queue-file">
              <span class="upload-queue-name">📄 {{ item.fileName }}</span>
              <span class="upload-queue-meta">
                <span v-if="item.size">{{ item.size }} · </span>
                <span :class="'queue-status-' + item.status">
                  {{ uploadStatusLabel(item.status) }}
                </span>
              </span>
            </div>
            <div class="upload-queue-actions">
              <span v-if="item.error" class="upload-queue-error">{{ item.error }}</span>
              <button
                v-if="item.status === 'error' && !item.invalid"
                class="secondary"
                :disabled="uploading"
                @click="retryFile(item)"
              >
                重试
              </button>
              <button
                v-if="item.status !== 'uploading'"
                class="icon-danger"
                :disabled="uploading"
                @click="removeQueueItem(item)"
              >
                移除
              </button>
            </div>
          </div>
        </div>
        <div v-if="documents.length === 0" class="documents-empty">
          还没有文档，点击右上角「上传 PDF」开始添加。
        </div>
        <div v-else class="document-list">
          <div v-for="document in documents" :key="document.id" class="document-card">
            <div class="document-info">
              <div class="document-name">📄 {{ document.fileName }}</div>
              <div class="document-meta">
                <span>状态：{{ statusLabel(document.status) }}</span>
                <span v-if="document.pageCount">页数：{{ document.pageCount }}</span>
                <span v-if="document.errorMessage">失败原因：{{ document.errorMessage }}</span>
              </div>
            </div>
            <div class="document-actions">
              <button
                class="secondary"
                :disabled="reindexingDocumentId === document.id"
                @click="reindexDocument(document)"
              >
                {{ reindexingDocumentId === document.id ? '重建中...' : '重新索引' }}
              </button>
              <button class="danger" @click="deleteDocument(document)">删除</button>
            </div>
          </div>
        </div>
        </template>
      </section>

      <div class="message-list">
        <div v-if="messages.length === 0" class="empty-chat">
          请选择或新建会话，然后开始提问
        </div>

        <div
          v-for="(message, index) in messages"
          :key="`${message.id || index}-${message.role}`"
          class="message"
          :class="message.role"
        >
          <div class="message-role">
            {{ message.role === 'user' ? '用户' : 'AI 助手' }}
          </div>
          <div class="message-content">{{ message.content }}</div>
          <div v-if="message.sources && message.sources.length" class="sources">
            <div class="sources-title">引用来源</div>
            <div v-for="source in message.sources" :key="source.chunkId" class="source">
              <span>第 {{ source.pageNumber || '未知' }} 页</span>
              <span>相似度 {{ source.score ?? 0 }}</span>
              <p>{{ source.content }}</p>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading">正在生成回答...</div>
      </div>

      <footer class="chat-input">
        <textarea
          v-model="question"
          placeholder="请输入你的问题..."
          rows="3"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <button
          class="primary send"
          :disabled="loading || !selectedConversationId || !question.trim()"
          @click="sendMessage"
        >
          {{ loading ? '发送中...' : '发送' }}
        </button>
      </footer>

      <div v-if="errorMessage" class="error">{{ errorMessage }}</div>
    </main>
    <AdminPanel
      v-if="showAdminPanel"
      :knowledge-base-id="selectedKnowledgeBaseId"
      @close="showAdminPanel = false"
    />
  </div>
  </template>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

body {
  margin: 0;
}

.layout {
  display: flex;
  height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  background: #f5f7fa;
}

.sidebar {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e3e8ef;
  padding: 18px 14px;
  overflow-y: auto;
}

.sidebar-header h1 {
  margin: 0 0 14px;
  font-size: 20px;
}

button {
  cursor: pointer;
  border: 1px solid #d5dbe3;
  background: #fff;
  color: #24313f;
  padding: 8px 12px;
  border-radius: 8px;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.form-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 12px 0;
  padding: 12px;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  background: #f9fbfd;
}

.form-card input {
  padding: 9px 10px;
  border: 1px solid #d5dbe3;
  border-radius: 7px;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.panel {
  margin-top: 18px;
}

.panel-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: #576474;
}

.list-item {
  display: block;
  width: 100%;
  text-align: left;
  margin: 6px 0;
  border-color: transparent;
}

.list-item.active {
  background: #eaf1ff;
  border-color: #bfd3ff;
  color: #1d4ed8;
}

.empty {
  color: #93a0af;
  font-size: 13px;
  padding: 8px 0;
}

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 18px 22px;
  background: #fff;
  border-bottom: 1px solid #e3e8ef;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
}

.chat-subtitle {
  color: #64748b;
  font-size: 13px;
  margin-top: 4px;
}

.documents-panel {
  margin: 18px 22px 0;
  background: #fff;
  border: 1px solid #e3e8ef;
  border-radius: 12px;
  padding: 14px 16px;
}

.documents-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
  color: #24313f;
}

.documents-count {
  color: #64748b;
  font-size: 13px;
  font-weight: 400;
}

.documents-empty {
  color: #93a0af;
  font-size: 13px;
  padding: 12px 0;
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.document-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f9fbfd;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
}

.document-info {
  min-width: 0;
}

.document-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.document-name {
  color: #24313f;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-meta {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  gap: 14px;
}

.danger {
  color: #dc2626;
  border-color: #fecaca;
  background: #fff5f5;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
}

.empty-chat {
  color: #93a0af;
  text-align: center;
  margin-top: 80px;
}

.message {
  max-width: 760px;
  margin: 0 auto 18px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e3e8ef;
}

.message.user {
  background: #eaf1ff;
  border-color: #bfd3ff;
}

.message-role {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  margin-bottom: 6px;
}

.message.user .message-role {
  color: #1d4ed8;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #d5dbe3;
}

.sources-title {
  font-size: 13px;
  color: #576474;
  margin-bottom: 6px;
}

.source {
  background: #f9fbfd;
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 6px;
}

.source span {
  display: inline-block;
  margin-right: 10px;
  color: #64748b;
  font-size: 12px;
}

.source p {
  margin: 6px 0 0;
  color: #334155;
  font-size: 13px;
}

.loading {
  text-align: center;
  color: #2563eb;
  margin: 12px 0;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 20px;
  background: #fff;
  border-top: 1px solid #e3e8ef;
}

.chat-input textarea {
  flex: 1;
  resize: none;
  padding: 10px 12px;
  border: 1px solid #d5dbe3;
  border-radius: 10px;
}

.send {
  min-width: 88px;
}

.error {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  background: #dc2626;
  color: #fff;
  padding: 10px 16px;
  border-radius: 8px;
}
</style>

<style scoped>
:global(body) {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif;
  background: #f4f7fc;
  color: #182a49;
}

.layout {
  background:
    radial-gradient(circle at 8% 8%, rgba(123, 131, 216, 0.12), transparent 30%),
    radial-gradient(circle at 92% 0%, rgba(83, 105, 201, 0.10), transparent 28%),
    linear-gradient(135deg, #f4f7fc 0%, #eef3fa 48%, #f5f6fc 100%);
  color: #182a49;
}

.sidebar {
  width: 300px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(18px);
  border-right: 1px solid #dce4f0;
  box-shadow: 8px 0 30px rgba(55, 75, 120, 0.05);
}

.sidebar-header h1 {
  color: #182a49;
  letter-spacing: -0.02em;
}

button {
  border: 1px solid #dce4f0;
  background: rgba(255, 255, 255, 0.72);
  color: #33415c;
  border-radius: 10px;
  font-size: 13px;
  transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease;
}

button:hover:not(:disabled) {
  border-color: #cbd6ea;
  background: #f8faff;
}

.primary {
  background: #5369c9;
  border-color: #5369c9;
  color: #fff;
  box-shadow: 0 6px 16px rgba(83, 105, 201, 0.18);
}

.primary:hover:not(:disabled) {
  background: #4358b3;
  border-color: #4358b3;
  color: #fff;
}

.primary:disabled {
  background: #9ba7cc;
  border-color: #9ba7cc;
  box-shadow: none;
}

.form-card {
  background: rgba(238, 243, 250, 0.78);
  border: 1px solid #dce4f0;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(55, 75, 120, 0.06);
}

.form-card input {
  background: #fff;
  border: 1px solid #dce4f0;
  color: #182a49;
}

.form-card select {
  width: 100%;
  padding: 9px 10px;
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 7px;
  color: #182a49;
}

.form-card input:focus {
  outline: none;
  border-color: #7b83d8;
  box-shadow: 0 0 0 3px rgba(123, 131, 216, 0.16);
}

.panel-title {
  color: #65738b;
}

.panel-title-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
}

.collapse-icon {
  font-size: 12px;
  color: #8a96ad;
}

.kb-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.kb-search {
  flex: 1;
  min-width: 0;
  padding: 7px 9px;
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 8px;
  color: #182a49;
  font-size: 12px;
}

.kb-search:focus {
  outline: none;
  border-color: #7b83d8;
  box-shadow: 0 0 0 3px rgba(123, 131, 216, 0.16);
}

.kb-filter {
  flex-shrink: 0;
  padding: 7px 8px;
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 8px;
  color: #33415c;
  font-size: 12px;
}

.list-item {
  position: relative;
  background: transparent;
  border: 1px solid transparent;
  color: #33415c;
  border-radius: 10px;
  padding: 10px 12px;
}

.list-item:hover {
  background: rgba(232, 236, 250, 0.72);
  border-color: #dce4f0;
}

.list-item.active {
  background: #e8ecfa;
  color: #25365b;
  border-color: #dce4f0;
  box-shadow: inset 3px 0 0 #5369c9;
}

.kb-row {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 10px;
}

.kb-row .list-item {
  flex: 1;
}

.kb-row.active {
  background: #e8ecfa;
  box-shadow: inset 3px 0 0 #5369c9;
}

.kb-visibility {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 7px;
  font-size: 11px;
  line-height: 1.4;
  border-radius: 999px;
  background: #eef1fa;
  color: #65738b;
  vertical-align: middle;
}

.conversation-row {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 10px;
  padding: 2px 4px;
}

.conversation-row .list-item {
  flex: 1;
}

.conversation-row.active {
  background: #e8ecfa;
  box-shadow: inset 3px 0 0 #5369c9;
}

.conversation-menu-wrap {
  position: relative;
  flex-shrink: 0;
}

.icon-more {
  flex-shrink: 0;
  width: 26px;
  padding: 6px 0;
  font-size: 15px;
  line-height: 1;
  color: #65738b;
  border-color: transparent;
  background: transparent;
}

.icon-more:hover {
  background: #eef1fa;
  border-color: #dce4f0;
  color: #4358b3;
}

.conversation-menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  z-index: 30;
  min-width: 96px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(55, 75, 120, 0.14);
}

.conversation-menu button {
  text-align: left;
  padding: 7px 10px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #33415c;
}

.conversation-menu button:hover {
  background: #eef1fa;
  color: #25365b;
}

.conversation-menu .menu-danger {
  color: #b9556c;
}

.conversation-menu .menu-danger:hover {
  background: #fcf1f4;
  color: #9f4358;
}

.conversation-edit-input {
  flex: 1;
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid #cbd6ea;
  border-radius: 8px;
}

.icon-edit,
.icon-confirm {
  flex-shrink: 0;
  padding: 6px 8px;
  font-size: 12px;
  color: #4358b3;
  border-color: transparent;
  background: transparent;
}

.icon-edit:hover,
.icon-confirm:hover {
  background: #eef1fa;
  border-color: #dce4f0;
}

.icon-danger {
  flex-shrink: 0;
  padding: 6px 8px;
  font-size: 12px;
  background: transparent;
  color: #b9556c;
  border-color: transparent;
}

.icon-danger:hover:not(:disabled) {
  background: #fcf1f4;
  border-color: #e9cbd4;
}

.empty,
.documents-empty,
.empty-chat {
  color: #8a96ad;
}

.chat {
  background: rgba(244, 247, 252, 0.62);
}

.chat-header {
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid #dce4f0;
  box-shadow: 0 5px 24px rgba(55, 75, 120, 0.05);
}

.chat-title {
  color: #182a49;
}

.chat-subtitle {
  color: #65738b;
}

.documents-panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #dce4f0;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(55, 75, 120, 0.08);
}

.documents-title {
  color: #182a49;
  cursor: pointer;
  user-select: none;
}

.documents-title-main {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.documents-count {
  color: #65738b;
}

.documents-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-queue {
  margin-bottom: 14px;
  padding: 12px;
  background: #f8fafe;
  border: 1px solid #e3eaf6;
  border-radius: 12px;
}

.upload-queue-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #33415c;
}

.upload-queue-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-queue-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  margin-top: 6px;
  background: #fff;
  border: 1px solid #e3eaf6;
  border-radius: 10px;
}

.upload-queue-file {
  min-width: 0;
}

.upload-queue-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #182a49;
  font-size: 13px;
}

.upload-queue-meta {
  display: block;
  margin-top: 3px;
  color: #65738b;
  font-size: 12px;
}

.upload-queue-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.upload-queue-error {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #b9556c;
  font-size: 12px;
}

.queue-status-pending {
  color: #65738b;
}

.queue-status-uploading {
  color: #5369c9;
}

.queue-status-success {
  color: #4d9d6e;
}

.queue-status-error {
  color: #b9556c;
}

.secondary {
  background: #eef1fa;
  border-color: #dce4f0;
  color: #4358b3;
  font-size: 12px;
}

.secondary:hover:not(:disabled) {
  background: #e5eaf8;
  border-color: #cbd6ea;
}

.document-card {
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 12px;
  box-shadow: 0 3px 14px rgba(55, 75, 120, 0.06);
}

.document-name {
  color: #182a49;
}

.document-meta {
  color: #65738b;
}

.danger {
  background: #fcf1f4;
  border-color: #e9cbd4;
  color: #b9556c;
}

.message {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid #dce4f0;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(55, 75, 120, 0.05);
}

.message.user {
  background: #e9eefb;
  border-color: #d7e0f5;
}

.message-role {
  color: #5369c9;
}

.message.user .message-role {
  color: #4358b3;
}

.message-content {
  color: #2c3c5a;
}

.sources {
  border-top: 1px solid #dce4f0;
}

.sources-title {
  color: #65738b;
}

.source {
  background: #f8fafe;
  border: 1px solid #e3eaf6;
  border-radius: 10px;
}

.source span {
  color: #65738b;
}

.source p {
  color: #3a4a68;
}

.loading {
  color: #5369c9;
}

.chat-input {
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px);
  border-top: 1px solid #dce4f0;
  box-shadow: 0 -6px 24px rgba(55, 75, 120, 0.05);
}

.chat-input textarea {
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 12px;
  color: #182a49;
  box-shadow: inset 0 1px 3px rgba(55, 75, 120, 0.04);
}

.chat-input textarea:focus {
  outline: none;
  border-color: #7b83d8;
  box-shadow: 0 0 0 3px rgba(123, 131, 216, 0.18);
}

.send {
  min-width: 88px;
}

.error {
  background: #b9556c;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(120, 60, 80, 0.18);
}

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 15% 15%, rgba(123, 131, 216, 0.14), transparent 30%),
    linear-gradient(135deg, #f4f7fc, #eef3fa);
}

.login-card {
  width: 360px;
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 12px 40px rgba(55, 75, 120, 0.12);
}

.login-card h1 {
  margin: 0 0 8px;
  color: #182a49;
}

.login-card p {
  color: #65738b;
  margin-bottom: 18px;
}

.login-card input {
  display: block;
  width: 100%;
  margin: 10px 0;
  padding: 10px 12px;
  border: 1px solid #dce4f0;
  border-radius: 10px;
}

.login-card button {
  width: 100%;
}

.login-error {
  margin-top: 10px;
  color: #b9556c;
  font-size: 13px;
}

.current-user {
  margin: 0 0 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #182a49;
}

.current-user small {
  color: #65738b;
}
</style>
