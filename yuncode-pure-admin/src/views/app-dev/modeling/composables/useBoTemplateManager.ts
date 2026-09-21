import { ref } from "vue";
import { ElMessage } from "element-plus";
import type { BoField } from "@/api/bo-table";

/** 模板管理 composable */
export function useBoTemplateManager() {
  const showTemplateDialog = ref(false);
  const selectedTemplate = ref("");

  const TEMPLATES = {
    bo_default: {
      name: "BO 默认字段",
      fields: [
        { fieldName: "ID", fieldTitle: "主键ID", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "PROCESSINSTID", fieldTitle: "流程实例ID", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "ORGID", fieldTitle: "组织ID", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "CREATEDATE", fieldTitle: "创建日期", fieldType: "日期", fieldLength: 0, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "CREATEUSER", fieldTitle: "创建用户", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "UPDATEDATE", fieldTitle: "更新日期", fieldType: "日期", fieldLength: 0, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "UPDATEUSER", fieldTitle: "更新用户", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "PROCESSDEFID", fieldTitle: "流程定义ID", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "ISEND", fieldTitle: "是否结束", fieldType: "文本", fieldLength: 10, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "TASKINST_HANDLEUSER", fieldTitle: "处理人", fieldType: "文本", fieldLength: 500, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "TASKINST_NODENAME", fieldTitle: "节点名称", fieldType: "文本", fieldLength: 200, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "DELETE_BY", fieldTitle: "删除人", fieldType: "文本", fieldLength: 64, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "DELETE_FLAG", fieldTitle: "删除标记", fieldType: "数字", fieldLength: 1, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "DELETE_TIME", fieldTitle: "删除时间", fieldType: "日期", fieldLength: 0, component: "隐藏", required: 0, visible: 0, readonly: 0 },
        { fieldName: "TENANT_ID", fieldTitle: "租户ID", fieldType: "数字", fieldLength: 20, component: "隐藏", required: 0, visible: 0, readonly: 0 },
      ],
    },
    contact: {
      name: "联系人",
      fields: [
        { fieldName: "CONTACT_NAME", fieldTitle: "联系人姓名", fieldType: "文本", fieldLength: 50, component: "单行文本", required: 0, visible: 1, readonly: 0 },
        { fieldName: "CONTACT_PHONE", fieldTitle: "联系电话", fieldType: "文本", fieldLength: 20, component: "单行文本", required: 0, visible: 1, readonly: 0 },
        { fieldName: "CONTACT_EMAIL", fieldTitle: "邮箱", fieldType: "文本", fieldLength: 100, component: "单行文本", required: 0, visible: 1, readonly: 0 },
        { fieldName: "CONTACT_ADDR", fieldTitle: "地址", fieldType: "文本", fieldLength: 200, component: "单行文本", required: 0, visible: 1, readonly: 0 },
      ],
    },
    address: {
      name: "地址",
      fields: [
        { fieldName: "PROVINCE", fieldTitle: "省", fieldType: "文本", fieldLength: 50, component: "下拉选择", required: 0, visible: 1, readonly: 0 },
        { fieldName: "CITY", fieldTitle: "市", fieldType: "文本", fieldLength: 50, component: "下拉选择", required: 0, visible: 1, readonly: 0 },
        { fieldName: "DISTRICT", fieldTitle: "区", fieldType: "文本", fieldLength: 50, component: "下拉选择", required: 0, visible: 1, readonly: 0 },
        { fieldName: "DETAIL_ADDR", fieldTitle: "详细地址", fieldType: "文本", fieldLength: 200, component: "多行文本", required: 0, visible: 1, readonly: 0 },
      ],
    },
  };

  function openTemplateDialog() {
    showTemplateDialog.value = true;
  }

  function applyTemplate(existingFields: BoField[]): BoField[] {
    if (!selectedTemplate.value) {
      ElMessage.warning("请选择模板");
      return existingFields;
    }
    const template = TEMPLATES[selectedTemplate.value as keyof typeof TEMPLATES];
    if (!template) return existingFields;

    const existingNames = new Set(existingFields.map(f => f.fieldName));
    const newFields = template.fields
      .filter(f => !existingNames.has(f.fieldName))
      .map(f => ({ ...f } as BoField));

    if (newFields.length === 0) {
      ElMessage.warning("模板字段已全部存在");
      return existingFields;
    }

    ElMessage.success(`已添加 ${newFields.length} 个模板字段`);
    showTemplateDialog.value = false;
    selectedTemplate.value = "";
    return [...existingFields, ...newFields];
  }

  return {
    showTemplateDialog,
    selectedTemplate,
    TEMPLATES,
    openTemplateDialog,
    applyTemplate,
  };
}
