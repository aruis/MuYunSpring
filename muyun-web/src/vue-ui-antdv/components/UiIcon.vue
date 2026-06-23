<script setup lang="ts">
import { computed } from 'vue';
import {
  AppstoreOutlined,
  BellOutlined,
  DownOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue';

defineOptions({ name: 'UiIcon' });

export type UiIconName = 'app' | 'down' | 'notification' | 'reload' | 'search' | 'settings';

const props = defineProps<{
  name: UiIconName;
}>();

const icons = {
  app: AppstoreOutlined,
  down: DownOutlined,
  notification: BellOutlined,
  reload: ReloadOutlined,
  search: SearchOutlined,
  settings: SettingOutlined,
} as const;

const icon = computed(() => icons[props.name]);
</script>

<template>
  <component :is="icon" />
</template>
