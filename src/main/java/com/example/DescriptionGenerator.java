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
        Collection<ModelElementInstance> elements = modelInstance.getModelElementsByType(flowNodeType);
        return elements.size();
    }

    public String generateProcessDescription(BpmnModelInstance modelInstance) {
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

        // Kapitel 1 und 2: Beschreibung der Pfade von jedem Start-Event bis zum ersten gemeinsamen Element
        Map<FlowNode, Set<StartEvent>> convergenceMap = new HashMap<>();
        int chapterCount = 1;
        for (StartEvent se : startEvents) {
            text.append("Kapitel ").append(chapterCount).append(": Von Start-Event '").append(se.getName()).append("' bis zum ersten gemeinsamen Element\n");
            text.append(generatePartialDescription(modelInstance, se, allFlowNodes, convergenceMap));
            text.append("\n\n");
            chapterCount++;
        }

        // Kapitel 3: Beschreibung ab dem ersten gemeinsamen Element
        FlowNode firstCommonElement = findFirstCommonElement(convergenceMap);
        if (firstCommonElement != null) {
            text.append("Kapitel ").append(chapterCount).append(": Ab dem ersten gemeinsamen Element '").append(firstCommonElement.getName()).append("'\n");
            text.append("----------------\n");
            text.append(generateProcessDescriptionFromStartEvent(modelInstance, firstCommonElement, allFlowNodes));
        }

        // Aktualisieren der fehlenden Elemente
        Set<FlowNode> missing = new HashSet<>(allFlowNodes);
        missing.removeAll(nodeToStartEventIdsMap.keySet().stream()
                .map(id -> modelInstance.getModelElementById(id))
                .filter(FlowNode.class::isInstance)
                .map(FlowNode.class::cast)
                .collect(Collectors.toSet()));
        missingElements.addAll(missing);

        return text.toString();
    }

    private String generatePartialDescription(BpmnModelInstance modelInstance, StartEvent startEvent, Set<FlowNode> allFlowNodes, Map<FlowNode, Set<StartEvent>> convergenceMap) {
        StringBuilder text = new StringBuilder();
        Set<FlowNode> visitedNodes = new HashSet<>();
        Stack<FlowNode> nodesToProcess = new Stack<>();
        nodesToProcess.push(startEvent);

        boolean commonElementFound = false;

        while (!nodesToProcess.isEmpty() && !commonElementFound) {
            FlowNode currentNode = nodesToProcess.pop();
            if (visitedNodes.contains(currentNode)) {
                continue;
            }
            visitedNodes.add(currentNode);
            processedElementCount++;

            convergenceMap.computeIfAbsent(currentNode, k -> new HashSet<>()).add(startEvent);
            if (convergenceMap.get(currentNode).size() > 1) {
                text.append("\n Das erste gemeinsame Element ist '").append(currentNode.getName()).append("'.\n");
                commonElementFound = true;
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

        return text.toString();
    }

    private String generateProcessDescriptionFromStartEvent(BpmnModelInstance modelInstance, FlowNode startNode, Set<FlowNode> allFlowNodes) {
        StringBuilder text = new StringBuilder();
        Set<FlowNode> visitedNodes = new HashSet<>();
        Stack<FlowNode> nodesToProcess = new Stack<>();
        nodesToProcess.push(startNode);

        while (!nodesToProcess.isEmpty()) {
            FlowNode currentNode = nodesToProcess.pop();
            if (visitedNodes.contains(currentNode)) {
                continue;
            }
            visitedNodes.add(currentNode);
            processedElementCount++;

            nodeToStartEventIdsMap.computeIfAbsent(currentNode.getId(), k -> new HashSet<>()).add(startNode.getId());
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

        return text.toString();
    }

    private FlowNode findFirstCommonElement(Map<FlowNode, Set<StartEvent>> convergenceMap) {
        for (Map.Entry<FlowNode, Set<StartEvent>> entry : convergenceMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                return entry.getKey();
            }
        }
        return null;
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
