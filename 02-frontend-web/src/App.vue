<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <router-view v-if="authStore.isLoggedIn" />
    <router-view v-else />
  </div>
</template>