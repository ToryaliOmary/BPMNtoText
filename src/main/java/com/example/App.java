package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

public class App {
    public static void main(String[] args) {
        String filePath = "src/main/ressources/neu.bpmn"; 
        String processId = "_70e46d4c-fe72-426c-97d7-1cccda37cd11";

        BpmnModelInstance modelInstance = BpmnLoader.loadBpmnModel(filePath);
        if (modelInstance != null) {
            System.out.println("Laden erfolgreich");
        } else {
            System.out.println("Fehler beim Laden");
            return;
        }

        Process process = ProcessExtractor.extractProcess(modelInstance, processId);
        if (process == null) return;

        StartEvent startEvent = ProcessExtractor.findStartEvent(process);
        if (startEvent == null) return;

        // Aufruf der Methode printSequenceFlowDetails
        // printSequenceFlowDetails(process);

        String processDescription = DescriptionGenerator.generateProcessDescription(modelInstance, process);

        String outputFilePath = "prozessbeschreibung.txt";
        FileWriterUtil.writeToFile(outputFilePath, processDescription);

        System.out.println("Der Text wurde in " + outputFilePath + " gespeichert.");
    }

    // Verschiebung der Methode aus der main-Methode heraus und als normale Klasse Methode implementiert
    private static void printSequenceFlowDetails(Process process) {
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof SequenceFlow) {
                SequenceFlow flow = (SequenceFlow) element;
                System.out.println("SequenceFlow ID: " + flow.getId());
                System.out.println("Task: " + flow.getName());
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
