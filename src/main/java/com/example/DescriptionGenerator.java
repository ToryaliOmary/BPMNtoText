package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.util.*;
import java.util.stream.Collectors;

public class DescriptionGenerator {
    private int processedElementCount = 0;
    private List<FlowNode> missingElements = new ArrayList<>();
    private Map<String, Set<String>> nodeToStartEventIdsMap = new HashMap<>();

    public int getProcessedElementCount() {
        return processedElementCount;
    }

    public List<FlowNode> getMissingElements() {
        return missingElements;
    }

    public static int countTotalElements(BpmnModelInstance modelInstance) {
        ModelElementType flowNodeType = modelInstance.getModel().getType(FlowNode.class);
        return modelInstance.getModelElementsByType(flowNodeType).size();
    }

    public String generateProcessDescription(BpmnModelInstance modelInstance, StartEvent startEvent) {
        StringBuilder text = new StringBuilder();
        ModelElementType flowNodeType = modelInstance.getModel().getType(FlowNode.class);
        Set<FlowNode> allFlowNodes = modelInstance.getModelElementsByType(flowNodeType).stream()
                .filter(FlowNode.class::isInstance)
                .map(FlowNode.class::cast)
                .collect(Collectors.toSet());

        ModelElementType startEventType = modelInstance.getModel().getType(StartEvent.class);
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(startEventType).stream()
                .filter(StartEvent.class::isInstance)
                .map(StartEvent.class::cast)
                .collect(Collectors.toList());

        if (startEvents.isEmpty()) {
            return "Keine Start-Events gefunden.";
        }

        for (StartEvent se : startEvents) {
            text.append(generateProcessDescriptionFromStartEvent(modelInstance, se, allFlowNodes));
        }

        return text.toString();
    }

    private String generateProcessDescriptionFromStartEvent(BpmnModelInstance modelInstance, StartEvent startEvent, Set<FlowNode> allFlowNodes) {
        StringBuilder text = new StringBuilder();
        Set<FlowNode> visitedNodes = new HashSet<>();
        Stack<FlowNode> nodesToProcess = new Stack<>();
        nodesToProcess.push(startEvent);

        while (!nodesToProcess.isEmpty()) {
            FlowNode currentNode = nodesToProcess.pop();
            if (visitedNodes.contains(currentNode)) {
                continue;
            }
            visitedNodes.add(currentNode);
            processedElementCount++;

            nodeToStartEventIdsMap.computeIfAbsent(currentNode.getId(), k -> new HashSet<>()).add(startEvent.getId());
            if (nodeToStartEventIdsMap.get(currentNode.getId()).size() > 1) {
                text.append("\n An dieser Stelle überschneidet sich der Prozess mit dem zuvor beschriebenen.\n");
                break;
            }

            Lane lane = findLane(modelInstance, currentNode);
            if (lane != null) {
                ElementProcessor.processElement(currentNode, lane, text);
            } else {
                ElementProcessor.processElement(currentNode, text);
            }

            List<FlowNode> nextNodes = currentNode.getOutgoing().stream()
                    .map(SequenceFlow::getTarget)
                    .collect(Collectors.toList());
            Collections.reverse(nextNodes);
            nodesToProcess.addAll(nextNodes);
        }

        Set<FlowNode> missing = new HashSet<>(allFlowNodes);
        missing.removeAll(visitedNodes);
        missingElements.addAll(missing);

        return text.toString();
    }

    private static Lane findLane(BpmnModelInstance modelInstance, FlowNode flowNode) {
        ModelElementType laneType = modelInstance.getModel().getType(Lane.class);
        for (ModelElementInstance instance : modelInstance.getModelElementsByType(laneType)) {
            Lane lane = (Lane) instance;
            if (lane.getFlowNodeRefs().contains(flowNode)) {
                return lane;
            }
        }
        return null;
    }
}