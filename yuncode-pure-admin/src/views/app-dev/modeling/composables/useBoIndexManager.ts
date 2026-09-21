import { ref } from "vue";
import { ElMessage } from "element-plus";

/** 索引管理 composable */
export function useBoIndexManager() {
  const indexTags = ref<string[]>(["ID"]);
  const showIndexModal = ref(false);
  const indexForm = ref({ type: "普通索引", field: "", remark: "" });

  const INDEX_FIELD_NAMES = [
    "ID", "PROCESSINSTID", "ORGID", "CREATEDATE", "CREATEUSER",
    "UPDATEDATE", "UPDATEUSER", "PROCESSDEFID", "ISEND",
    "TASKINST_HANDLEUSER", "TASKINST_NODENAME"
  ];

  function addIndex() {
    showIndexModal.value = true;
  }

  function confirmAddIndex(fieldNames: string[]) {
    if (!indexForm.value.field) {
      ElMessage.warning("请选择索引字段");
      return;
    }
    if (indexTags.value.includes(indexForm.value.field)) {
      ElMessage.warning("该字段已添加为索引");
      return;
    }
    indexTags.value.push(indexForm.value.field);
    indexForm.value = { type: "普通索引", field: "", remark: "" };
    showIndexModal.value = false;
  }

  function removeIndex(index: number) {
    indexTags.value.splice(index, 1);
  }

  return {
    indexTags,
    showIndexModal,
    indexForm,
    INDEX_FIELD_NAMES,
    addIndex,
    confirmAddIndex,
    removeIndex,
  };
}
