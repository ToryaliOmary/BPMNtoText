package com.example;

import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class DebugUtil {
    public static void printSequenceFlowDetails(Process process) {
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof SequenceFlow) {
                SequenceFlow flow = (SequenceFlow) element;
                System.out.println("SequenceFlow ID: " + flow.getId());
                System.out.println("Source: " + flow.getSource().getName());
                System.out.println("Target: " + flow.getTarget().getName());
                if (flow.getName() != null && !flow.getName().isEmpty()) {
                    System.out.println("Label: " + flow.getName());
                } else {
                    System.out.println("Label: none");
                }
                System.out.println("Condition: " + (flow.getConditionExpression() != null ? flow.getConditionExpression().getTextContent() : "none"));
                System.out.println("-------------");
            }
        }
    }
}
