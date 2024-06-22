package com.example;

import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.HashSet;
import java.util.Set;

public class TaskUtils {
    public static FlowNode getNextElement(Task task) {
        if (!task.getOutgoing().isEmpty()) {
            SequenceFlow flow = task.getOutgoing().iterator().next();
            return flow.getTarget();
        }
        return null;
    }

    public static String getNextElementName(FlowNode node) {
        for (SequenceFlow flow : node.getOutgoing()) {
            if (flow.getTarget() != null) {
                return flow.getTarget().getName();
            }
        }
        return null;
    }

    public static String getPreviousElementName(FlowNode node) {
        for (SequenceFlow flow : node.getIncoming()) {
            if (flow.getSource() != null) {
                return flow.getSource().getName();
            }
        }
        return null;
    }

    public static String extractHtmlContentTask(Task task) {
        StringBuilder htmlContent = new StringBuilder();
        Set<String> uniqueContents = new HashSet<>();
        for (ModelElementInstance childElement : task.getChildElementsByType(task.getModelInstance().getModel().getType(Documentation.class))) {
            if (childElement instanceof Documentation) {
                Documentation documentation = (Documentation) childElement;
                String content = documentation.getTextContent().trim();
                if (uniqueContents.add(content)) {
                    htmlContent.append(content);
                }
            }
        }
        return htmlContent.toString();
    }

    public static String extractHtmlContentEvent(IntermediateCatchEvent intermediateCatchEvent) {
        StringBuilder htmlContent = new StringBuilder();
        Set<String> uniqueContents = new HashSet<>();
    
        for (Documentation documentation : intermediateCatchEvent.getDocumentations()) {
            // Überprüfen, ob das Dokumentationselement das Attribut textFormat="text/html" hat
            if ("text/html".equals(documentation.getAttributeValue("textFormat"))) {
                String content = documentation.getTextContent().trim();
                // Überprüfen Sie, ob der Inhalt nicht leer ist und ob er bereits hinzugefügt wurde
                if (!content.isEmpty() && uniqueContents.add(content)) {
                    htmlContent.append(content).append(" ");
                }
            }
        }
    
        return htmlContent.toString().trim(); // Entferne das letzte Leerzeichen
    }
    

    public static String extractHtmlContentGateway(Gateway gateway) {
        StringBuilder htmlContent = new StringBuilder();
        Set<String> uniqueContents = new HashSet<>();
        for (Documentation documentation : gateway.getDocumentations()) {
            String content = documentation.getTextContent().trim();
            if (uniqueContents.add(content)) {
                htmlContent.append(content);
            }
        }
        return htmlContent.toString().trim();
    }
}
