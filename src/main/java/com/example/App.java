package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
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

        String processDescription = DescriptionGenerator.generateProcessDescription(modelInstance, process);

        String outputFilePath = "prozessbeschreibung.txt";
        FileWriterUtil.writeToFile(outputFilePath, processDescription);

        System.out.println("Der Text wurde in " + outputFilePath + " gespeichert.");
    }
}
