<template>
  <div class="page-wrapper dark">
    <div class="glow-wrapper">
      <div class="container">
        <div class="content" style="padding: 20px;">
          
          <div class="header">
            <span class="sys-status" style="font-size:16px;color:#00d4ff;font-family:monospace;cursor:pointer" @click="$router.back()">← BACK</span>
          </div>

          <div class="search-box">
            <input class="sys-input" placeholder="输入歌曲、歌手、情绪" v-model="keyword" @keyup.enter="search" />
            <button class="sys-btn primary" @click="search" :disabled="loading" style="margin-left: 10px; width: auto; padding: 0 20px;">
              {{ loading ? '...' : '搜索' }}
            </button>
          </div>
          
          <div class="scroll-view" style="flex:1; margin-top:20px;">
            <div v-for="item in songs" :key="item.id" class="song-item">
              <span>{{item.name}} - {{item.artist}}</span>
            </div>
            <div v-if="!songs.length && !loading" style="text-align:center;color:#666;margin-top:40px;font-size:12px;">
              暂无搜索结果
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const keyword = ref('')
const songs = ref([])
const loading = ref(false)

const search = async () => {
  if (!keyword.value.trim()) return

  const userId = localStorage.getItem('music_userId')
  if (!userId) {
    router.push('/login')
    return
  }

  loading.value = true
  try {
    const res = await request.get(`/api/music/search`, {
      params: { keywords: keyword.value, userId: Number(userId) }
    })
    songs.value = res.data?.songs || []
  } catch (error) {
    console.error('搜索失败:', error)
    alert('搜索失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-box {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}
.song-item {
  padding: 15px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 14px;
}
</style>
