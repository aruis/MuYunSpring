<script setup lang="ts">
import { DynamicModulePage, customerDescriptor } from '@muyun/dynamic-page-runtime';
</script>

<template>
  <section class="home-view">
    <header class="hero">
      <div>
        <p class="eyebrow">MuYun frontend skeleton</p>
        <h1>平台前端基础架构</h1>
        <p>
          当前骨架先固定 Vue 技术栈、MuYun 组件契约、Ant Design Vue 适配层和动态运行器边界。
          后续平台业务页面应优先复用这些组件，不直接依赖底层 UI 库。
        </p>
      </div>
    </header>

    <DynamicModulePage :descriptor="customerDescriptor" />
  </section>
</template>
