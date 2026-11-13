<template>
  <div class="page">
    <PageHeader title="个人中心" description="查看账号资料与安全设置。姓名、工号等身份信息由管理员维护。" />

    <div class="profile-grid">
      <section class="panel profile-card">
        <div class="profile-card__hero">
          <span class="profile-avatar">{{ initial }}</span>
          <div>
            <h2>{{ auth.displayName }}</h2>
            <p class="muted">{{ auth.roleLabel }}<template v-if="auth.siteName"> · {{ auth.siteName }}</template></p>
          </div>
          <StatusTag v-if="auth.isRecycler" class="profile-status" :value="auth.profile?.status" kind="recycler" />
        </div>

        <el-descriptions :column="1" border class="profile-desc">
          <template v-if="auth.isAdmin">
            <el-descriptions-item label="管理员账号">{{ auth.profile?.username }}</el-descriptions-item>
            <el-descriptions-item label="账号角色">系统管理员</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(auth.profile?.createdAt) }}</el-descriptions-item>
          </template>
          <template v-else>
            <el-descriptions-item label="姓名">{{ auth.profile?.name }}</el-descriptions-item>
            <el-descriptions-item label="手机号">
              <span class="mono">{{ auth.profile?.phone }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="工号">
              <span class="mono">{{ auth.profile?.employeeNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="所属站点">{{ auth.profile?.siteName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <StatusTag :value="auth.profile?.status" kind="recycler" />
            </el-descriptions-item>
            <el-descriptions-item label="入职登记">{{ formatDateTime(auth.profile?.createdAt) }}</el-descriptions-item>
          </template>
        </el-descriptions>
      </section>

      <section class="panel security-card">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">账号与安全</h3>
            <p class="panel-sub">密码修改后所有旧令牌立即失效</p>
          </div>
        </div>

        <div class="security-list">
          <div class="security-item">
            <div>
              <h4>登录密码</h4>
              <p>建议定期更换，避免与他人共用账号。</p>
            </div>
            <el-button @click="router.push('/change-password')">修改密码</el-button>
          </div>

          <div class="security-item">
            <div>
              <h4>退出登录</h4>
              <p>服务端会将令牌版本 +1，当前账号所有已签发令牌立即失效。</p>
            </div>
            <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
          </div>
        </div>

        <div class="role-note">
          <template v-if="auth.isAdmin">
            <h4>职责分离提醒</h4>
            <p>
              管理员不参与称重录入与兑换核销，避免「自己录数据、自己发积分、自己核销」。
              管理员也不能直接修改用户密码，只能重置为一次性临时密码。
            </p>
          </template>
          <template v-else>
            <h4>回收员权限边界</h4>
            <p>
              只能查看本人经手的流水；不能修改积分规则、商品与库存，也不能管理账号。
              录入错误可在 5 分钟内自助撤销，超时需联系管理员授权。
            </p>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()
const initial = computed(() => String(auth.displayName || '?').slice(0, 1))

async function handleLogout() {
  try {
    await ElMessageBox.confirm('退出后当前令牌立即失效，需要重新登录。', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.85fr);
  gap: 20px;
  align-items: start;
}

.profile-card__hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 26px 26px 22px;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(180deg, #f7fbf9, #fff);
}

.profile-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background: linear-gradient(135deg, #2fbd7c, #16794c);
  color: #fff;
  font-size: 24px;
  font-weight: 650;
  box-shadow: 0 10px 22px -14px rgba(22, 121, 76, 0.8);
  flex: none;
}

.profile-card__hero h2 {
  font-size: 19px;
  font-weight: 650;
}

.profile-card__hero p {
  margin-top: 2px;
  font-size: 12.5px;
}

.profile-status {
  margin-left: auto;
}

.profile-desc {
  padding: 22px 26px 26px;
}

.profile-desc :deep(.el-descriptions__table) {
  border-radius: 10px;
  overflow: hidden;
}

.profile-desc :deep(.el-descriptions__label) {
  width: 110px;
}

.security-list {
  padding: 6px 24px;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}

.security-item:last-child {
  border-bottom: 0;
}

.security-item h4 {
  font-size: 13.5px;
  font-weight: 600;
}

.security-item p {
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 12px;
  line-height: 1.65;
  max-width: 340px;
}

.role-note {
  margin: 8px 24px 24px;
  padding: 16px 18px;
  border-radius: 12px;
  background: var(--brand-50);
  border: 1px solid #dceee4;
}

.role-note h4 {
  color: var(--brand-700);
  font-size: 12.5px;
  font-weight: 650;
}

.role-note p {
  margin-top: 5px;
  color: var(--ink-500);
  font-size: 12px;
  line-height: 1.75;
}

@media (max-width: 1100px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
