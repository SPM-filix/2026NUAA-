<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { targetTypeText } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const form = reactive({ description: '' })

const targetType = computed(() => String(route.query.targetType || ''))
const targetId = computed(() => String(route.query.targetId || ''))
const targetNo = computed(() => String(route.query.targetNo || ''))
const hasTarget = computed(() => ['DELIVERY', 'EXCHANGE'].includes(targetType.value) && Boolean(targetId.value))

async function submit() {
  const text = form.description.trim()
  if (text.length < 10 || text.length > 500) {
    ElMessage.warning('问题描述需为 10–500 个字符')
    return
  }
  submitting.value = true
  try {
    await userApi.createAppeal({
      targetType: targetType.value,
      targetId: Number(targetId.value),
      description: text,
    })
    ElMessage.success('申诉已提交，请等待管理员处理')
    router.replace('/me/appeals')
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="提交申诉" />
    <div class="page-body">
      <EmptyState v-if="!hasTarget" text="未找到关联的投递记录或订单">
        <el-button round size="small" @click="router.replace('/me')">返回我的</el-button>
      </EmptyState>

      <template v-else>
        <div class="card">
          <div class="detail-row">
            <span class="label">申诉对象</span>
            <span class="value">{{ targetTypeText[targetType] || targetType }}</span>
          </div>
          <div class="detail-row">
            <span class="label">关联单号</span>
            <span class="value">{{ targetNo || targetId }}</span>
          </div>
        </div>

        <div class="card" style="margin-top: 14px">
          <p style="margin: 0 0 10px; font-size: 14px; font-weight: 600">问题描述</p>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="请说明遇到的问题，至少 10 个字，便于管理员核实处理"
          />
          <p class="muted small" style="margin: 10px 0 0">
            提交后会进入管理员待办，处理结果可在「我的申诉」中查看。
          </p>
        </div>

        <el-button
          type="primary"
          size="large"
          round
          style="width: 100%; margin-top: 18px"
          :loading="submitting"
          @click="submit"
        >
          提交申诉
        </el-button>
      </template>
    </div>
  </div>
</template>
