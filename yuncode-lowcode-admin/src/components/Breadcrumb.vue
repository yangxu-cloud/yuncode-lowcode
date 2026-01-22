<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index === levelList.length - 1" class="no-redirect">
          {{ item.meta.title }}
        </span>
        <a v-else @click.prevent="handleLink(item)">
          {{ item.meta.title }}
        </a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const levelList = ref<any[]>([]);

getBreadcrumb();

watch(
  () => route.path,
  () => getBreadcrumb()
);

function getBreadcrumb() {
  let matched = route.matched.filter((item) => item.meta && item.meta.title);
  const first = matched[0];

  if (!isDashboard(first)) {
    matched = [{ path: "/home", meta: { title: "首页" } } as any].concat(matched);
  }

  levelList.value = matched.filter((item) => item.meta && item.meta.title && item.meta.breadcrumb !== false);
}

function isDashboard(route: any) {
  const name = route && route.name;
  if (!name) {
    return false;
  }
  return name.toString().trim().toLocaleLowerCase() === "Home".toLocaleLowerCase();
}

function handleLink(item: any) {
  const { redirect, path } = item;
  if (redirect) {
    router.push(redirect.toString());
    return;
  }
  router.push(path);
}
</script>

<style lang="scss" scoped>
.breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }

  a {
    color: #666;
    cursor: pointer;

    &:hover {
      color: #409eff;
    }
  }
}
</style>
