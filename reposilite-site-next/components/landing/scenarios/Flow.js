import { Box, Text, useColorModeValue } from '@chakra-ui/react'
import ReactFlow, { ReactFlowProvider } from 'react-flow-renderer'

const LockedReactFlow = ({ children, elements, style }) => (
  <ReactFlowProvider>
    <ReactFlow
      elements={elements}
      style={style}
      nodesDraggable={false}
      draggable={false}
      contentEditable={false}
      paneMoveable={false}
      preventScrolling={true}
      zoomOnScroll={false}
      zoomOnPinch={false}
      zoomOnDoubleClick={false}
      connectionMode={false}
      nodesConnectable={false}
      nodesDraggable={false}
      elementsSelectable={false}
    />
    <style jsx global>{`
      .react-flow__node-input, .react-flow__node-default {
        background: none !important;
        color: ${useColorModeValue('black', 'white')} !important;
        width: auto !important;
        border: none !important;
        box-shadow: none !important;
        cursor: default !important;
      }
      .react-flow__edge-textwrapper {}
      .react-flow__edge-textbg {
        fill: var(${useColorModeValue('--chakra-colors-gray-100', '--chakra-colors-gray-800')}) !important;
      }
      .react-flow__edge-text {
        fill: white !important;
      }
    `}</style>
  </ReactFlowProvider>
)

const StyledNode = ({ label, style, flow }) => {
  const title = label === undefined
    ? <></>
    : <Text>{label}</Text>
  const flowComponent = flow === undefined
    ? <></>
    : <LockedReactFlow
      elements={flow}
      style={style}
    />
  
  return (
    <Box paddingY={3} paddingX={2} borderRadius={50} backgroundColor={useColorModeValue('gray.100', 'gray.900')}>
      {title}
      {flowComponent}
    </Box>
  )
}

export {
  LockedReactFlow,
  StyledNode
}