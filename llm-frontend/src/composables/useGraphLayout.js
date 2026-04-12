import dagre from '@dagrejs/dagre'

/**
 * 使用 Dagre 算法对 Vue Flow 节点进行自动布局
 * @param {Array} nodes - Vue Flow nodes（position 会被覆盖）
 * @param {Array} edges - Vue Flow edges
 * @param {Object} options - 布局选项
 * @returns {Array} 带计算后 position 的 nodes 副本
 */
export function useGraphLayout(nodes, edges, options = {}) {
  const {
    direction = 'TB',
    nodeWidth = 200,
    nodeHeight = 100,
    nodesep = 80,
    ranksep = 120
  } = options

  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: direction, nodesep, ranksep })

  nodes.forEach(node => {
    g.setNode(node.id, { width: nodeWidth, height: nodeHeight })
  })

  edges.forEach(edge => {
    g.setEdge(edge.source, edge.target)
  })

  dagre.layout(g)

  return nodes.map(node => {
    const pos = g.node(node.id)
    return {
      ...node,
      position: {
        x: pos.x - nodeWidth / 2,
        y: pos.y - nodeHeight / 2
      }
    }
  })
}
