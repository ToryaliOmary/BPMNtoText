package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.util.Collection;

public class ProcessExtractor {
    public static Process extractProcess(BpmnModelInstance modelInstance, String processId) {
        Process process = (Process) modelInstance.getModelElementById(processId);
        if (process == null) {
            System.err.println("Process-ID nicht gefunden");
        }
        return process;
    }

    public static StartEvent findStartEvent(Process process) {
        Collection<FlowElement> flowElements = process.getFlowElements();
        for (FlowElement element : flowElements) {
            if (element instanceof StartEvent) {
                return (StartEvent) element;
            }
        }
        System.err.println("Start-Ereignis nicht gefunden.");
        return null;
    }
}
