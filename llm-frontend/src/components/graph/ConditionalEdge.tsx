import React from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, EdgeProps } from '@xyflow/react'

const ConditionalEdge: React.FC<EdgeProps> = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  data,
  markerEnd
}) => {
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition
  })

  const edgeStyle = {
    stroke: 'var(--color-primary)',
    strokeWidth: 2,
    strokeDasharray: '6 4',
    opacity: 0.6
  }

  return (
    <>
      <BaseEdge path={edgePath} style={edgeStyle} markerEnd={markerEnd} />
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
            pointerEvents: 'all'
          }}
          className="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/60 whitespace-nowrap"
        >
          {data?.condition as string}
        </div>
      </EdgeLabelRenderer>
    </>
  )
}

export default ConditionalEdge
