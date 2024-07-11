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
    private Map<FlowNode, Set<StartEvent>> convergenceMap = new HashMap<>();
    private Set<FlowNode> visitedElements = new HashSet<>();

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

        // Berechnung der Konvergenz im Voraus
        for (StartEvent se : startEvents) {
            generateConvergenceMap(modelInstance, se, allFlowNodes, convergenceMap);
        }

        // Kapitel 1: Beschreibung der Pfade von einem Start-Event bis zum ersten gemeinsamen Element
        StartEvent firstStartEvent = startEvents.iterator().next();
        text.append("Kapitel 1: Von Start-Event '").append(firstStartEvent.getName()).append("' bis zum ersten gemeinsamen Element\n");
        text.append("----------------\n");
        FlowNode firstCommonElement = generatePartialDescription(modelInstance, firstStartEvent, allFlowNodes, text, true);
        text.append("\n\n");

        // Kapitel 2: Beschreibung der Pfade von den anderen Start-Events bis zum ersten gemeinsamen Element
        int chapterCount = 2;
        for (StartEvent se : startEvents) {
            if (se.equals(firstStartEvent)) continue;
            text.append("Kapitel ").append(chapterCount).append(": Von Start-Event '").append(se.getName()).append("' bis zum ersten gemeinsamen Element\n");
            text.append("----------------\n");
            generatePartialDescription(modelInstance, se, allFlowNodes, text, true);
            text.append("\n\n");
            chapterCount++;
        }

        // Kapitel 3: Beschreibung ab dem ersten gemeinsamen Element
        if (firstCommonElement != null) {
            text.append("Kapitel ").append(chapterCount).append(": Ab dem ersten gemeinsamen Element '").append(firstCommonElement.getName()).append("'\n");
            text.append("----------------\n");
            text.append(generateProcessDescriptionFromStartEvent(modelInstance, firstCommonElement, allFlowNodes));
        }

        // Aktualisieren der fehlenden Elemente
        Set<FlowNode> missing = new HashSet<>(allFlowNodes);
        missing.removeAll(visitedElements);
        missingElements.addAll(missing);

        return text.toString();
    }

    private void generateConvergenceMap(BpmnModelInstance modelInstance, StartEvent startEvent, Set<FlowNode> allFlowNodes, Map<FlowNode, Set<StartEvent>> convergenceMap) {
        Set<FlowNode> visitedNodes = new HashSet<>();
        Stack<FlowNode> nodesToProcess = new Stack<>();
        nodesToProcess.push(startEvent);

        while (!nodesToProcess.isEmpty()) {
            FlowNode currentNode = nodesToProcess.pop();
            if (visitedNodes.contains(currentNode)) {
                continue;
            }
            visitedNodes.add(currentNode);

            convergenceMap.computeIfAbsent(currentNode, k -> new HashSet<>()).add(startEvent);

            List<FlowNode> nextNodes = currentNode.getOutgoing().stream()
                    .map(SequenceFlow::getTarget)
                    .collect(Collectors.toList());
            Collections.reverse(nextNodes);
            nodesToProcess.addAll(nextNodes);
        }
    }

    private FlowNode generatePartialDescription(BpmnModelInstance modelInstance, StartEvent startEvent, Set<FlowNode> allFlowNodes,
                                                StringBuilder text, boolean stopAtCommonElement) {
        Set<FlowNode> visitedNodes = new HashSet<>();
        Stack<FlowNode> nodesToProcess = new Stack<>();
        nodesToProcess.push(startEvent);

        while (!nodesToProcess.isEmpty()) {
            FlowNode currentNode = nodesToProcess.pop();
            if (visitedNodes.contains(currentNode)) {
                continue;
            }
            visitedNodes.add(currentNode);
            visitedElements.add(currentNode); // Hinzufügen zu den besuchten Elementen
            processedElementCount++;

            if (convergenceMap.get(currentNode).size() > 1 && stopAtCommonElement) {
                text.append("\nDas erste gemeinsame Element ist '").append(currentNode.getName()).append("'.\n");
                return currentNode;
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

        return null;
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
            visitedElements.add(currentNode); // Hinzufügen zu den besuchten Elementen
            processedElementCount++;

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
