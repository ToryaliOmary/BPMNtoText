package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.*;
import java.util.stream.Collectors;

public class DescriptionGenerator {
    private int processedElementCount = 0;
    private List<FlowNode> missingElements = new ArrayList<>();

    public int getProcessedElementCount() {
        return processedElementCount;
    }

    public List<FlowNode> getMissingElements() {
        return missingElements;
    }

    public static int countTotalElements(BpmnModelInstance modelInstance) {
        return modelInstance.getModelElementsByType(modelInstance.getModel().getType(FlowNode.class)).size();
    }

    public String generateProcessDescription(BpmnModelInstance modelInstance, StartEvent startEvent) {
        StringBuilder text = new StringBuilder();
        Set<FlowNode> visitedNodes = new HashSet<>();
        Queue<FlowNode> nodesToProcess = new LinkedList<>();
        nodesToProcess.add(startEvent);

        // Alle FlowNodes sammeln
        Set<FlowNode> allFlowNodes = new HashSet<>();
        for (ModelElementInstance instance : modelInstance.getModelElementsByType(modelInstance.getModel().getType(FlowNode.class))) {
            allFlowNodes.add((FlowNode) instance);
        }

        while (!nodesToProcess.isEmpty()) {
            FlowNode currentNode = nodesToProcess.poll();
            if (visitedNodes.contains(currentNode)) {
                continue;
            }
            visitedNodes.add(currentNode);
            processedElementCount++; // Zählt die aufgenommenen Elemente

            Lane lane = findLane(modelInstance, currentNode);
            if (lane != null) {
                ElementProcessor.processElement(currentNode, lane, text);
            } else {
                ElementProcessor.processElement(currentNode, text);
            }

            nodesToProcess.addAll(currentNode.getOutgoing().stream()
                    .map(SequenceFlow::getTarget)
                    .collect(Collectors.toList()));
        }

        // Fehlende Elemente identifizieren
        allFlowNodes.removeAll(visitedNodes);
        missingElements.addAll(allFlowNodes);

        return text.toString();
    }

    private static Lane findLane(BpmnModelInstance modelInstance, FlowNode flowNode) {
        for (ModelElementInstance instance : modelInstance.getModelElementsByType(modelInstance.getModel().getType(Lane.class))) {
            Lane lane = (Lane) instance;
            if (lane.getFlowNodeRefs().contains(flowNode)) {
                return lane;
            }
        }
        return null;
    }
}
