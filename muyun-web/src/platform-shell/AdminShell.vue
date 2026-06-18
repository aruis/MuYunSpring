<script setup lang="ts">
defineOptions({ name: 'AdminShell' });
</script>

<template>
  <main class="app-shell">
    <aside class="app-sidebar">
      <div class="brand">MuYun</div>
      <nav>
        <a>平台配置</a>
        <a>动态运行态</a>
        <a>身份权限</a>
      </nav>
    </aside>
    <section class="app-content">
      <slot />
    </section>
  </main>
</template>
