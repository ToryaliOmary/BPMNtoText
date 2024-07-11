package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.type.ModelElementType;
import java.util.*;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) {
        String filePath = "src/main/ressources/neu.bpmn";

        BpmnModelInstance modelInstance = BpmnLoader.loadBpmnModel(filePath);
        if (modelInstance != null) {
            System.out.println("Laden erfolgreich");
        } else {
            System.out.println("Fehler beim Laden");
            return;
        }

        Process process = extractFirstProcess(modelInstance);
        if (process == null) {
            System.out.println("Kein Prozess in der Datei gefunden.");
            return;
        }

        int totalElements = DescriptionGenerator.countTotalElements(modelInstance);
        DescriptionGenerator descriptionGenerator = new DescriptionGenerator();
        String processDescription = descriptionGenerator.generateProcessDescription(modelInstance);

        String outputFilePath = "prozessbeschreibung.txt";
        FileWriterUtil.writeToFile(outputFilePath, processDescription);

        int processedElements = descriptionGenerator.getProcessedElementCount();
        List<FlowNode> missingElements = descriptionGenerator.getMissingElements();

        System.out.println("Der Text wurde in " + outputFilePath + " gespeichert.");
        System.out.println("Im Prozess sind " + totalElements + " Elemente.");
        System.out.println("In der Prozessbeschreibung sind " + processedElements + " Elemente.");

        if (!missingElements.isEmpty()) {
            System.out.println("Fehlende Elemente:");
            missingElements.forEach(element ->
                    System.out.println(element.getElementType().getTypeName() + " - " + element.getName()));
        } else {
            System.out.println("Alle Elemente wurden aufgenommen.");
        }
    }

    private static Process extractFirstProcess(BpmnModelInstance modelInstance) {
        ModelElementType processType = modelInstance.getModel().getType(Process.class);
        Collection<Process> processes = modelInstance.getModelElementsByType(processType).stream()
                .filter(Process.class::isInstance)
                .map(Process.class::cast)
                .collect(Collectors.toList());
        if (processes.isEmpty()) {
            return null;
        }
        return processes.iterator().next();
    }
}
