package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.ResourceRef;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.util.*;

public class DescriptionGenerator {
    public static String generateProcessDescription(BpmnModelInstance modelInstance, Process process) {
        StringBuilder text = new StringBuilder();
        Map<String, String> resourceMap = buildResourceMap(modelInstance);  // Mapping von ResourceRef zu Namen

        List<FlowNode> orderedFlowNodes = new ArrayList<>();
        // Startpunkt finden
        FlowElement startElement = process.getFlowElements().iterator().next();
        if (startElement instanceof FlowNode) {
            FlowNode startNode = (FlowNode) startElement;
            collectNodesRecursively(startNode, orderedFlowNodes, new HashSet<>());
        } else {
            return "Startelement ist kein FlowNode.";
        }

        for (FlowNode node : orderedFlowNodes) {
            String performerName = getPerformerName(node, resourceMap, modelInstance);
            ElementProcessor.processElement(node, performerName, text);
        }

        return text.toString();
    }

    private static Map<String, String> buildResourceMap(BpmnModelInstance modelInstance) {
    Map<String, String> map = new HashMap<>();
    
    // Erhalte den ModelElementType für Resource.
    ModelElementType resourceType = modelInstance.getModel().getType(Resource.class);
    
    // Nutze den erhaltenen ModelElementType, um die Ressourcen abzurufen.
    Collection<ModelElementInstance> resources = modelInstance.getModelElementsByType(resourceType);
    
    for (ModelElementInstance instance : resources) {
        Resource resource = (Resource) instance;
        System.out.println("Resource ID: " + resource.getId() + ", Name: " + resource.getName()); // Zum Debuggen
        
        map.put(resource.getId(), resource.getName());
    }
    
    return map;
}


private static String getPerformerName(FlowNode node, Map<String, String> resourceMap, BpmnModelInstance modelInstance) {
    ModelElementType resourceRoleType = modelInstance.getModel().getType(ResourceRole.class);

    // Suche nach dem ersten ResourceRole-Element des Nodes
    Optional<ResourceRole> resourceRole = node.getChildElementsByType(resourceRoleType).stream()
                                               .filter(ResourceRole.class::isInstance)
                                               .map(ResourceRole.class::cast)
                                               .findFirst();

    if (resourceRole.isPresent()) {
        Resource resource = resourceRole.get().getResource();
        if (resource != null) {
            String resourceId = resource.getId();
            String resourceName = resourceMap.get(resourceId);
            System.out.println("Resource ID: " + resourceId + " Resource Name: " + resourceName); // Debugging-Ausgabe
            return resourceMap.getOrDefault(resourceId, "Unbekannte Ressource");
        } else {
            System.out.println("ResourceRole vorhanden, aber keine zugeordnete Resource gefunden."); // Debugging-Ausgabe
        }
    } else {
        System.out.println("Keine ResourceRole für diesen Node gefunden."); // Debugging-Ausgabe
    }

    return "Kein Performer definiert";
}





    // Annahme: ElementProcessor.processElement wurde angepasst, um mit einem String für den Performer-Namen umzugehen
    private static void collectNodesRecursively(FlowNode currentNode, List<FlowNode> orderedFlowNodes, Set<FlowNode> visited) {
        if (visited.contains(currentNode)) {
            return;
        }
        visited.add(currentNode);
        orderedFlowNodes.add(currentNode);
        for (SequenceFlow outgoing : currentNode.getOutgoing()) {
            FlowNode nextNode = outgoing.getTarget();
            collectNodesRecursively(nextNode, orderedFlowNodes, visited);
        }
    }
}
