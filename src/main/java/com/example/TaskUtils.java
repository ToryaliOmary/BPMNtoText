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
        return "dem nächsten Schritt";
    }

    public static String getPreviousElementName(FlowNode node) {
        for (SequenceFlow flow : node.getIncoming()) {
            if (flow.getTarget() != null) {
                return flow.getTarget().getName();
            }
        }
        return "dem nächsten Schritt";
    }

    public static String extractHtmlContent(Task task) {
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
}
