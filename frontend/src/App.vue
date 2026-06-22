<template>
  <router-view v-slot="{ Component }">
    <keep-alive>
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup>
</script>

<style>
/* 全局基础样式转移到了 assets/styles/main.css */
</style>
