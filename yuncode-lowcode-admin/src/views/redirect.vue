<template>
  <div></div>
</template>

<script setup lang="ts">
import { onBeforeRouteEnter, useRoute } from "vue-router";
import { unref } from "vue";

onBeforeRouteEnter(to => {
  const { path, query, params } = to;
  const fullPath = unref(path).replace("/redirect", "");

  // 替换当前路由，去掉 /redirect 前缀
  return {
    path: fullPath,
    query,
    params,
    replace: true
  };
});
</script>
