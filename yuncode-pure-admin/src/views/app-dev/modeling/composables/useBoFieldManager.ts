import { ref, computed } from "vue";
import type { BoField } from "@/api/bo-table";

/** 字段管理 composable */
export function useBoFieldManager() {
  const fields = ref<BoField[]>([]);
  const searchKeyword = ref("");
  const selectAllMain = ref(false);
  const selectAllSystem = ref(false);

  const DEFAULT_FIELD_NAMES = [
    "ID", "PROCESSINSTID", "ORGID", "CREATEDATE", "CREATEUSER",
    "UPDATEDATE", "UPDATEUSER", "PROCESSDEFID", "ISEND",
    "TASKINST_HANDLEUSER", "TASKINST_NODENAME",
    "DELETE_BY", "DELETE_FLAG", "DELETE_TIME", "TENANT_ID"
  ];

  const mainFields = computed(() => {
    let list = fields.value.filter(f => !DEFAULT_FIELD_NAMES.includes(f.fieldName));
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase();
      list = list.filter(f =>
        f.fieldName.toLowerCase().includes(kw) || f.fieldTitle?.toLowerCase().includes(kw)
      );
    }
    return list;
  });

  const systemFields = computed(() => {
    let list = fields.value.filter(f => DEFAULT_FIELD_NAMES.includes(f.fieldName));
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase();
      list = list.filter(f =>
        f.fieldName.toLowerCase().includes(kw) || f.fieldTitle?.toLowerCase().includes(kw)
      );
    }
    return list;
  });

  function setFields(newFields: BoField[]) {
    fields.value = newFields;
  }

  function addField() {
    fields.value.push({
      _key: `field_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
      _checked: false,
      fieldName: "",
      fieldTitle: "",
      fieldType: "文本",
      fieldLength: 128,
      component: "单行文本",
      defaultValue: "",
      required: 0,
      visible: 1,
      readonly: 0,
      copyable: 0,
    } as BoField);
  }

  function toggleSelectAllMain() {
    mainFields.value.forEach(f => { (f as any)._checked = selectAllMain.value; });
  }

  function toggleSelectAllSystem() {
    systemFields.value.forEach(f => { (f as any)._checked = selectAllSystem.value; });
  }

  return {
    fields,
    searchKeyword,
    selectAllMain,
    selectAllSystem,
    mainFields,
    systemFields,
    DEFAULT_FIELD_NAMES,
    setFields,
    addField,
    toggleSelectAllMain,
    toggleSelectAllSystem,
  };
}
