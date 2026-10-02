<template>
  <div class="ll-page">
    <div class="ll-panel" style="padding: 12px 14px; margin-bottom: 12px;">
      <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center;">
        <span class="ll-section-title">洗涤批次隧道式流转</span>
        <el-button type="primary" @click="openCreate">新建批次（排产）</el-button>
        <el-button @click="load">刷新</el-button>
        <el-button v-if="quarantinedTotal > 0" type="danger" plain @click="goContamination">
          污染隔离待处置 {{ quarantinedTotal }} 缸
        </el-button>
        <span class="ll-sub">点卡片底部「下一工序」推进；工序必须按顺序，不能跳步；污染隔离批次（红框）不能推进或按正常完工处理</span>
      </div>
      <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px;">
        <el-tag
          v-for="s in stepStats"
          :key="s.stepCode"
          size="small"
          :type="s.batchCount > 0 ? 'primary' : 'info'"
          effect="plain"
        >
          {{ s.stepName }} {{ s.batchCount }} 缸 / {{ s.totalWeight }}kg
        </el-tag>
        <el-tag v-if="quarantinedTotal > 0" size="small" type="danger" effect="dark">
          污染隔离 {{ quarantinedTotal }} 缸（隧道内红框展示，不计入在产站位）
        </el-tag>
        <span class="ll-sub">在产 {{ summary.runningBatch || 0 }} 缸 · 在产重量 {{ summary.runningWeight || 0 }}kg · 已完工 {{ summary.doneBatch || 0 }} 缸</span>
      </div>
    </div>

    <div class="ll-tunnel-viewport" ref="viewportRef">
      <div class="ll-tunnel-track" :style="{ transform: `translateX(${offset}px)` }">
        <div v-for="(lane, index) in lanes" :key="lane.stepCode" class="ll-tunnel-lane">
          <div class="ll-lane-head" :class="{ 'is-focus': focusIndex === index }">
            <span>{{ index + 1 }}. {{ lane.stepName }}</span>
            <span class="ll-lane-count">{{ lane.batches.length }} 缸</span>
          </div>

          <div v-if="!lane.batches.length" class="ll-sub" style="padding: 10px 6px;">— 无批次停留 —</div>

          <div
            v-for="b in lane.batches"
            :key="b.id"
            class="ll-batch-card"
            :class="{
              'is-done': b.status === 'DONE',
              'is-quarantine': b.status === 'QUARANTINED' && !b.rewashOrderId,
              'is-rewash': b.rewashOrderId
            }"
          >
            <div class="ll-batch-no">
              <span>
                {{ b.batchNo }}
                <el-tag v-if="b.rewashOrderId" size="small" type="warning" effect="dark">复洗</el-tag>
              </span>
              <el-tag size="small" :type="batchTagType(b)">
                {{ batchStatusName(b.status) }}
              </el-tag>
            </div>

            <!-- 污染来源横幅 -->
            <div v-if="b.quarantineEventNo" class="ll-contam-flag" @click="goContamination">
              <span>☣ 污染来源 {{ b.quarantineEventNo }}</span>
            </div>
            <div v-if="b.rewashStatus && b.sourceBatchNo" class="ll-rewash-flag">
              复洗自 {{ b.sourceBatchNo }} · {{ rewashStatusName(b.rewashStatus) }}
            </div>
            <div v-else-if="b.rewashBatchNo && b.quarantineEventNo" class="ll-rewash-flag is-out">
              已发起复洗 → {{ b.rewashBatchNo }}
            </div>

            <div class="ll-batch-meta">
              {{ b.categoryName }} · {{ b.weightKg }}kg · {{ b.waterTemp }}℃<br />
              洗涤剂 {{ b.detergentMl }}ml · {{ b.machineName || '未指定设备' }} · {{ b.operator || '未指派' }}
            </div>

            <div class="ll-step-dots">
              <span
                v-for="(step, i) in stepNames"
                :key="step"
                class="ll-step-dot"
                :class="{
                  'is-done': i + 1 < b.currentStepOrder || b.status === 'DONE',
                  'is-current': i + 1 === b.currentStepOrder && b.status !== 'DONE'
                }"
                :title="step"
              >
                {{ i + 1 < b.currentStepOrder || b.status === 'DONE' ? '✓' : step.charAt(0) }}
              </span>
            </div>

            <el-button
              v-if="b.status === 'QUARANTINED'"
              type="danger"
              size="small"
              style="width: 100%"
              @click="goContamination"
            >
              ☣ 污染隔离 · 去处置
            </el-button>
            <el-button
              v-else-if="b.status !== 'DONE'"
              type="primary"
              size="small"
              style="width: 100%"
              :loading="advancingId === b.id"
              @click="advance(b)"
            >
              下一工序 → {{ nextStepName(b) }}
            </el-button>
            <el-button v-else size="small" style="width: 100%" disabled>
              {{ b.rewashOrderId ? '复洗完成，已入库' : '折叠完成，已入库' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建批次 -->
    <el-dialog v-model="createVisible" title="新建洗涤批次（排产）" width="460px">
      <el-form label-width="92px" size="small">
        <el-form-item label="布草品类">
          <el-select v-model="createForm.categoryId" placeholder="选择品类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name + '（' + c.spec + '）'" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="重量 kg">
          <el-input-number v-model="createForm.weightKg" :min="1" :max="100" :step="0.5" :precision="1" />
        </el-form-item>
        <el-form-item label="水洗机">
          <el-select v-model="createForm.machineId" placeholder="选择设备" style="width: 100%">
            <el-option
              v-for="m in washers"
              :key="m.id"
              :label="m.name + ' · 容量 ' + m.capacityKg + 'kg · ' + machineStatusName(m.status)"
              :value="m.id"
              :disabled="m.status === 'FAULT'"
            />
          </el-select>
          <div class="ll-sub">故障设备不允许排产</div>
        </el-form-item>
        <el-form-item label="水温 ℃">
          <el-input-number v-model="createForm.waterTemp" :min="0" :max="180" />
        </el-form-item>
        <el-form-item label="洗涤剂 ml">
          <el-input-number v-model="createForm.detergentMl" :min="0" :max="5000" :step="50" />
        </el-form-item>
        <el-form-item label="操作员">
          <el-input v-model="createForm.operator" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">排产</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { batchApi, categoryApi, machineApi } from '../api'

const emit = defineEmits(['kpi-change'])
const router = useRouter()

const LANE_WIDTH = 216

const batches = ref([])
const stepNames = ref([])
const stepStats = ref([])
const summary = ref({})
const categories = ref([])
const machines = ref([])
const focusIndex = ref(0)
const advancingId = ref(null)
const viewportRef = ref(null)
const viewportWidth = ref(1000)

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  categoryId: null,
  weightKg: 60,
  machineId: null,
  waterTemp: 35,
  detergentMl: 800,
  operator: '王建国'
})

const washers = computed(() => machines.value.filter((m) => m.machineType === 'WASHER'))

// 隧道展示在产 + 污染隔离批次（隔离批次按当前停留工序进入对应站位，但红框标识且不能推进）
const tunnelBatches = computed(() =>
  batches.value.filter((b) => b.status === 'RUNNING' || b.status === 'QUARANTINED')
)

const quarantinedTotal = computed(() => batches.value.filter((b) => b.status === 'QUARANTINED').length)

const lanes = computed(() => {
  const names = stepNames.value.length
    ? stepNames.value
    : ['预洗', '主洗', '漂洗', '中和', '脱水', '烘干', '烫平', '折叠']
  const codes = ['PRE_WASH', 'MAIN_WASH', 'RINSE', 'NEUTRALIZE', 'EXTRACT', 'DRY', 'IRON', 'FOLD']
  return names.map((stepName, index) => ({
    stepName,
    stepCode: codes[index],
    batches: tunnelBatches.value.filter((b) => b.currentStepOrder === index + 1)
  }))
})

function batchStatusName(status) {
  return { RUNNING: '在产', DONE: '已完工', HOLD: '挂起', QUARANTINED: '污染隔离' }[status] || status
}
function batchTagType(b) {
  if (b.status === 'DONE') return 'info'
  if (b.status === 'QUARANTINED') return 'danger'
  return 'success'
}
function rewashStatusName(s) {
  return { ACTIVE: '复洗在产', DONE: '复洗完成', FAILED: '复洗失败', DELETED: '已删除' }[s] || s
}
function goContamination() {
  router.push('/contamination')
}

const visibleLanes = computed(() => Math.max(1, Math.floor(viewportWidth.value / LANE_WIDTH)))

const offset = computed(() => {
  const maxIndex = Math.max(0, lanes.value.length - visibleLanes.value)
  const centered = focusIndex.value - Math.floor((visibleLanes.value - 1) / 2)
  return -Math.min(maxIndex, Math.max(0, centered)) * LANE_WIDTH
})

function nextStepName(batch) {
  const names = stepNames.value.length ? stepNames.value : []
  return names[batch.currentStepOrder] || '折叠完工'
}

function machineStatusName(status) {
  return { RUNNING: '运行', IDLE: '待机', FAULT: '故障' }[status] || status
}

async function load(keepFocus = false) {
  // 不带状态过滤：隧道需要同时展示在产与污染隔离批次（隔离批次红框、不可推进）
  const params = {}
  const [list, names, stats, sum] = await Promise.all([
    batchApi.list(params),
    batchApi.steps(),
    batchApi.stepStats(),
    batchApi.summary()
  ])
  batches.value = list
  stepNames.value = names
  stepStats.value = stats
  summary.value = sum
  if (!keepFocus) {
    focusToDefault()
  }
}

function focusToDefault() {
  if (!batches.value.length) {
    focusIndex.value = 0
    return
  }
  const running = batches.value.filter((b) => b.status === 'RUNNING')
  const target = running.length ? running : batches.value
  const maxOrder = Math.max(...target.map((b) => b.currentStepOrder || 1))
  focusIndex.value = maxOrder - 1
}

async function advance(batch) {
  advancingId.value = batch.id
  try {
    const updated = await batchApi.advance(batch.id, {
      operator: batch.operator,
      waterTemp: batch.waterTemp,
      detergentMl: batch.detergentMl
    })
    ElMessage.success(`${batch.batchNo} 已推进到「${stepNames.value[(updated.currentStepOrder || 1) - 1] || '完工'}」`)
    focusIndex.value = Math.min(lanes.value.length - 1, (updated.currentStepOrder || 1) - 1)
    await load(true)
    emit('kpi-change')
  } finally {
    advancingId.value = null
  }
}

async function openCreate() {
  const [cats, machs] = await Promise.all([categoryApi.list(), machineApi.list()])
  categories.value = cats
  machines.value = machs
  createVisible.value = true
}

async function submitCreate() {
  creating.value = true
  try {
    await batchApi.create({ ...createForm })
    ElMessage.success('排产成功，批次已进入预洗工序')
    createVisible.value = false
    focusIndex.value = 0
    await load()
    emit('kpi-change')
  } finally {
    creating.value = false
  }
}

function measure() {
  if (viewportRef.value) {
    viewportWidth.value = viewportRef.value.clientWidth
  }
}

onMounted(async () => {
  await load()
  await nextTick(measure)
  window.addEventListener('resize', measure)
})
onUnmounted(() => {
  window.removeEventListener('resize', measure)
})
</script>
