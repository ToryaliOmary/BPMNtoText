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

        int totalElements = DescriptionGenerator.countTotalElements(modelInstance);
        DescriptionGenerator descriptionGenerator = new DescriptionGenerator();
        String processDescription = descriptionGenerator.generateProcessDescription(modelInstance, startEvent);

        String outputFilePath = "prozessbeschreibung.txt";
        FileWriterUtil.writeToFile(outputFilePath, processDescription);

        int processedElements = descriptionGenerator.getProcessedElementCount();

        System.out.println("Der Text wurde in " + outputFilePath + " gespeichert.");
        System.out.println("Im Prozess sind " + totalElements + " Elemente.");
        System.out.println("In der Prozessbeschreibung sind " + processedElements + " Elemente.");

        if (!descriptionGenerator.getMissingElements().isEmpty()) {
            System.out.println("Fehlende Elemente:");
            descriptionGenerator.getMissingElements().forEach(element ->
                    System.out.println(element.getElementType().getTypeName() + " - " + element.getName()));
        } else {
            System.out.println("Alle Elemente wurden aufgenommen.");
        }
    }
}
