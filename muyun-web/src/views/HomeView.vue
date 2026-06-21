<template>
  <section class="home-view">
    <p class="home-eyebrow">Workspace</p>
    <h2>请选择左侧菜单</h2>
    <p>当前工作区已接入真实登录和菜单加载，业务页面将从后端菜单入口进入。</p>
  </section>
</template>

<style scoped>
.home-view {
  display: grid;
  gap: 8px;
  min-height: 240px;
  align-content: center;
  justify-items: center;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
  color: #5f6f83;
  text-align: center;
}

.home-eyebrow,
h2,
p {
  margin: 0;
}

.home-eyebrow {
  color: #6b7788;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  color: #1f2933;
  font-size: 18px;
}
</style>
