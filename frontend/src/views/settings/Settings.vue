<template>
  <div class="page-wrapper dark">
    <div class="glow-wrapper">
      <div class="container">
        <div class="content" style="padding: 20px; display:flex; flex-direction:column; align-items:center; justify-content:center">
          
          <div class="header" style="position:absolute; top:20px; left:20px; width: 100%;">
            <span class="sys-status" style="font-size:16px;color:#00d4ff;font-family:monospace;cursor:pointer" @click="$router.back()">← BACK</span>
          </div>

          <button class="sys-btn primary" @click="$router.push('/login')" style="width: 200px;">
            去登录 / 绑定账号
          </button>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Settings functionality to be implemented
</script>

<style scoped>
</style>
