package com.example;

import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class App {
    public static void main(String[] args) {
        // Parameter für die Datei und die Prozess-ID
        String filePath = "src/main/export-neu.bpmn";
        String processId = "_fcda809a-3b73-4ce1-b18a-43a8fc8d8ee9"; // Ändere dies auf die richtige Prozess-ID

        // Lade das BPMN-Diagramm
        BpmnModelInstance modelInstance = loadBpmnModel(filePath);
        if (modelInstance != null){
            debugIntermediateCatchEvents(modelInstance);
            System.out.println("laden erfolgreich");
        } else {
            System.out.println("Fehler beim laden");
        }
        

        // Extrahiere den Prozess
        Process process = extractProcess(modelInstance, processId);
        if (process == null) return;

        // Finde das Start-Ereignis
        StartEvent startEvent = findStartEvent(process);
        if (startEvent == null) return;

        // Drucke die Details der SequenceFlow-Elemente
        //printSequenceFlowDetails(process);

        // Generiere den Fließtext
        String processDescription = generateProcessDescription(modelInstance, process);

        // Schreibe den Fließtext in eine Datei
        String outputFilePath = "prozessbeschreibung.txt";
        writeToFile(outputFilePath, processDescription);

        // Ausgabe in der Konsole
        System.out.println("Der Text wurde in " + outputFilePath + " gespeichert.");
    }

    private static BpmnModelInstance loadBpmnModel(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Datei nicht gefunden: " + file.getAbsolutePath());
            return null;
        }

        return Bpmn.readModelFromFile(file);
    }

    private static Process extractProcess(BpmnModelInstance modelInstance, String processId) {
        Process process = (Process) modelInstance.getModelElementById(processId);
        if (process == null) {
            System.err.println("Process-ID nicht gefunden");
        }
        return process;
    }

    private static StartEvent findStartEvent(Process process) {
        Collection<FlowElement> flowElements = process.getFlowElements();
        for (FlowElement element : flowElements) {
            if (element instanceof StartEvent) {
                return (StartEvent) element;
            }
        }
        System.err.println("Start-Ereignis nicht gefunden.");
        return null;
    }

    //private static void printSequenceFlowDetails(Process process) {
        //for (FlowElement element : process.getFlowElements()) {
            //if (element instanceof SequenceFlow) {
                ///SequenceFlow flow = (SequenceFlow) element;
                //System.out.println("SequenceFlow ID: " + flow.getId());
                //System.out.println("Source: " + flow.getSource().getName());
                //System.out.println("Target: " + flow.getTarget().getName());
                //if (flow.getName() != null && !flow.getName().isEmpty()) {
                    //System.out.println("Label: " + flow.getName());
                //} else {
                    //System.out.println("Label: none");
                //}
                //System.out.println("Condition: " + (flow.getConditionExpression() != null ? flow.getConditionExpression().getTextContent() : "none"));
                //System.out.println("-------------");
            //}
        //}
    //}

    private static String generateProcessDescription(BpmnModelInstance modelInstance, Process process) {
        StringBuilder text = new StringBuilder();

        // Verarbeite die Lanes
        Collection<Lane> lanes = new ArrayList<>();
        for (ModelElementInstance instance : modelInstance.getModelElementsByType(modelInstance.getModel().getType(Lane.class))) {
            if (instance instanceof Lane) {
                lanes.add((Lane) instance);
            }
        }

        for (Lane lane : lanes) {
            List<FlowNode> laneFlowNodes = new ArrayList<>(lane.getFlowNodeRefs());

            // Verarbeite jedes Flow-Element in der Lane
            for (FlowNode flowNode : laneFlowNodes) {
                if (flowNode instanceof StartEvent) {
                    text.append("Der Prozess beginnt mit dem Start-Ereignis: \"")
                        .append(flowNode.getName())
                        .append("\".\n");
                } else if (flowNode instanceof EndEvent) {
                    text.append("Der Prozess endet mit dem End-Ereignis \"")
                        .append(flowNode.getName())
                        .append("\".\n");
                } else if (flowNode instanceof IntermediateCatchEvent) {
                    IntermediateCatchEvent intermediateCatchEvent = (IntermediateCatchEvent) flowNode;
                    text.append("   Einem Zwischenereignis vom Typ \"")
                        .append(getEventType(intermediateCatchEvent))
                        .append("\" mit dem Namen \"")
                        .append(intermediateCatchEvent.getName())
                        .append("\" tritt auf.\n");
                } else if (flowNode instanceof Task) {
                    Task task = (Task) flowNode;

                    // Extrahiere nur den HTML-Textinhalt
                    String taskDescription = extractHtmlContent(task);

                    String nextElement = getNextElementName(task);
                    text.append("Die Aufgabe '")
                        .append(task.getName())
                        .append("' wird von der Rolle '")
                        .append(lane.getName())
                        .append("' durchgeführt.\n  Diese Aufgabe ist folgendermaßen beschrieben: \n   '" + taskDescription + "'")
                        .append("\n  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an '" + nextElement + "' übergeben.\n");
                        appendIoSpecification(task, text);
                } else if (flowNode instanceof Gateway) {
                    Gateway gateway = (Gateway) flowNode;
                    text.append("Am Gateway (")
                        .append(gateway.getElementType().getTypeName())
                        .append(") namens \"")
                        .append(gateway.getName())
                        .append("\" kann der Prozess unterschiedliche Wege gehen, abhängig von der Entscheidungslogik.\n");
                    Collection<SequenceFlow> outgoing = gateway.getOutgoing();
                    for (SequenceFlow flow : outgoing) {
                        String condition = "Unbekannt";
                        if (flow.getName() != null && !flow.getName().isEmpty()) {
                            condition = flow.getName();
                        }
                        text.append("  Die Entscheidung '")
                            .append(condition).append("' führt zu: ")
                            .append(flow.getTarget().getName())
                            .append(".\n");
                    }
                }
            }
        }
        return text.toString();
    }

    private static String getEventType(IntermediateCatchEvent event) {
        if (event.getEventDefinitions().isEmpty()) {
            return "Keine EventDefinitions";
        }
    
        StringBuilder eventTypes = new StringBuilder();
        for (EventDefinition eventDefinition : event.getEventDefinitions()) {
            String simpleName = eventDefinition.getClass().getSimpleName();
            simpleName = simpleName.replace("Impl", ""); // Entfernt Impl, falls die Namen der Klassen Impl enthalten
            appendEventType(eventTypes, simpleName);
        }
        return eventTypes.toString();
    }
    
    private static void appendEventType(StringBuilder builder, String eventType) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(eventType);
    }
    


    private static String extractHtmlContent(Task task) {
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

    private static String getNextElementName(FlowNode node) {
        for (SequenceFlow flow : node.getOutgoing()) {
            if (flow.getTarget() != null) {
                return flow.getTarget().getName();
            }
        }
        return "dem nächsten Schritt";
    }

    private static void debugIntermediateCatchEvents(BpmnModelInstance modelInstance) {
        ModelElementType eventType = modelInstance.getModel().getType(IntermediateCatchEvent.class);
        Collection<ModelElementInstance> elements = modelInstance.getModelElementsByType(eventType);
    
        if (elements.isEmpty()) {
            System.out.println("Keine IntermediateCatchEvents gefunden.");
        }
    
        for (ModelElementInstance elementInstance : elements) {
            IntermediateCatchEvent event = (IntermediateCatchEvent) elementInstance;
            System.out.println("IntermediateCatchEvent ID: " + event.getId());
            boolean foundTimer = false;
    
            for (EventDefinition eventDefinition : event.getEventDefinitions()) {
                System.out.println("  Gefundene EventDefinition: " + eventDefinition.getClass().getSimpleName());
                if (eventDefinition instanceof TimerEventDefinition) {
                    System.out.println("    TimerEvent ist im Prozess definiert.");
                    foundTimer = true;
                }
            }

            if (!foundTimer) {
                System.out.println("    Kein TimerEvent für dieses IntermediateCatchEvent definiert.");
            }
        }
    }
    
    
    private static void appendIoSpecification(Task task, StringBuilder sb) {
        IoSpecification ioSpec = task.getIoSpecification();
        if (ioSpec != null) {
            ioSpec.getDataInputs().forEach(input -> sb.append("  Eingabedaten: ").append(input.getName()).append("\n"));
            ioSpec.getDataOutputs().forEach(output -> sb.append("  Ausgabedaten: ").append(output.getName()).append("\n"));
        }
    }
    


    private static void writeToFile(String filePath, String content) {
        File outputFile = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {  // false überschreibt die Datei
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }
}
