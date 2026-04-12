<template>
  <BaseEdge :path="edgePath" :style="edgeStyle" />
  <EdgeLabelRenderer>
    <div
      :style="{
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
        pointerEvents: 'all'
      }"
      class="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/60 whitespace-nowrap"
    >
      {{ data.condition }}
    </div>
  </EdgeLabelRenderer>
</template>

<script setup>
import { computed } from 'vue'
import { BaseEdge, EdgeLabelRenderer, getBezierPath } from '@vue-flow/core'

const props = defineProps({
  id: { type: String, required: true },
  sourceX: { type: Number, required: true },
  sourceY: { type: Number, required: true },
  targetX: { type: Number, required: true },
  targetY: { type: Number, required: true },
  sourcePosition: { type: String, default: 'bottom' },
  targetPosition: { type: String, default: 'top' },
  data: { type: Object, default: () => ({}) },
  markerEnd: { type: String, default: '' }
})

const edgePath = computed(() => {
  const [path] = getBezierPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    targetX: props.targetX,
    targetY: props.targetY,
    sourcePosition: props.sourcePosition,
    targetPosition: props.targetPosition
  })
  return path
})

const labelX = computed(() => (props.sourceX + props.targetX) / 2)
const labelY = computed(() => (props.sourceY + props.targetY) / 2)

const edgeStyle = {
  stroke: 'var(--color-primary)',
  strokeWidth: 2,
  strokeDasharray: '6 4',
  opacity: 0.6
}
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
