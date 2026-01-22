<template>
  <div class="navbar">
    <div class="left">
      <Hamburger :is-active="isCollapse" @toggle-click="toggleSidebar" />
      <Breadcrumb />
    </div>
    <div class="right">
      <Search />
      <FullScreen />
      <Notice />
      <Settings />
      <LanguageSwitcher />
      <UserDropdown />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Breadcrumb from "./Breadcrumb.vue";
import Search from "./Search.vue";
import FullScreen from "./FullScreen.vue";
import Notice from "./Notice.vue";
import Settings from "./Settings.vue";
import LanguageSwitcher from "./LanguageSwitcher.vue";
import UserDropdown from "./UserDropdown.vue";
import Hamburger from "./Hamburger.vue";

const isCollapse = ref(false);

const emit = defineEmits<{
  sidebarToggle: [isCollapse: boolean];
}>();

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value;
  emit("sidebarToggle", isCollapse.value);
};
</script>

<style scoped lang="scss">
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 16px;

  .left {
    display: flex;
    align-items: center;
  }

  .right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}
</style>
