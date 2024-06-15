package com.example;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.activiti.bpmn.model.TimerEventDefinition;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class App {
    public static void main(String[] args) {
        // Parameter für die Datei und die Prozess-ID
        String filePath = "src/main/ressources/ZweiterProzess7.bpmn";
        String processId = "_fcda809a-3b73-4ce1-b18a-43a8fc8d8ee9"; // Ändere dies auf die richtige Prozess-ID

        // Lade das BPMN-Diagramm
        BpmnModelInstance modelInstance = loadBpmnModel(filePath);
        if (modelInstance != null){
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

    ///// Extrahiere den Prozess
    private static Process extractProcess(BpmnModelInstance modelInstance, String processId) {
        Process process = (Process) modelInstance.getModelElementById(processId);
        if (process == null) {
            System.err.println("Process-ID nicht gefunden");
        }
        return process;
    }

    /// findet Start-Event

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



    ///// Erstellen des Fließtextes

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
                    // Textbaustein für StartEvent
                    StartEvent startEvent = (StartEvent) flowNode;
                    String nextTaskName = getNextElementName(startEvent);
                    text.append(String.format("Der Prozess beginnt mit Event ‚%s‘. Dieses Event setzt die erste Aufgabe ‚%s‘ in Gang.\n", startEvent.getName(), nextTaskName));
                }else if (flowNode instanceof EndEvent) {
                    // Textbaustein für EndEvent
                    EndEvent endEvent = (EndEvent) flowNode;
                    String previousTaskName = getPreviousElementName(flowNode);
                    text.append(String.format("Das Element ‚%s‘ stellt das End-Event des Prozesses dar. Dieses Ereignis tritt ein, nachdem die letzte Aufgabe ‚%s‘ abgeschlossen wurde, und signalisiert das Abschließen des Gesamtprozesses.\n\n", endEvent.getName(), previousTaskName));
                } else if (flowNode instanceof IntermediateCatchEvent) {
                    IntermediateCatchEvent intermediateCatchEvent = (IntermediateCatchEvent) flowNode;
                    String nextTaskName = getNextElementName(intermediateCatchEvent);
                    text.append("  Es folgt ein \"")
                        .append(getEventType(intermediateCatchEvent))
                        .append("\" mit dem Namen \"")
                        .append(intermediateCatchEvent.getName())
                        .append(String.format(".\n  Anschließend wird der Prozess mit dem nachfolgenden Element ‚%s‘ weitergeführt.\n", nextTaskName));
                    } else if (flowNode instanceof Task) {
                        Task task = (Task) flowNode;
                        String taskDescription = extractHtmlContent(task);  // Extract only the HTML text content
                        FlowNode nextElement = getNextElement(task);  // Determine the next element
                    
                        // Build the text depending on the type of the next element
                        text.append("Die Aufgabe '")
                            .append(task.getName())
                            .append("' wird von der Rolle '")
                            .append(lane.getName())
                            .append("' durchgeführt.\n  Diese Aufgabe ist folgendermaßen beschrieben: \n   '")
                            .append(taskDescription)
                            .append("'");
                    
                        // Check if the next element is an event
                        if (nextElement instanceof IntermediateCatchEvent) {
                            text.append("\nAn dieser Stelle tritt innerhalb des Prozesses ein Zwischenereignis auf.\n");
                        } else if (nextElement != null) {  // Ensuring nextElement is not null before calling getName()
                            text.append("\n  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an das nächste Element '")
                                .append(nextElement.getName())
                                .append("' übergeben.\n");
                        } else if (nextElement instanceof Gateway){
                            text.append("");
                        } else {
                            text.append("\n  Es gibt keinen definierten nächsten Schritt nach dieser Aufgabe.\n");
                        }
                        appendIoSpecification(task, text);  // Add Input/Output specification if available
                    } else if (flowNode instanceof Gateway) {
                    Gateway gateway = (Gateway) flowNode;
                    text.append("An dieser Stelle tritt innerhalb des Prozesses ein Gateway bzw. eine Entscheidung vom Typ ")
                        .append(gateway.getElementType().getTypeName())
                        .append(" namens \"")
                        .append(gateway.getName())
                        .append("\" auf.\n Abhängig von der Entscheidungslogik kann der Prozess nun unterschiedliche Wege gehen.\n");
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

    private static FlowNode getNextElement(Task task) {
        if (!task.getOutgoing().isEmpty()) {
            SequenceFlow flow = task.getOutgoing().iterator().next();  // Getting the first outgoing flow
            return flow.getTarget();  // Returning the target of this flow
        }
        return null;  // Return null if there are no outgoing flows
    }
    

    private static String getNextElementName(FlowNode node) {
        for (SequenceFlow flow : node.getOutgoing()) {
            if (flow.getTarget() != null) {
                return flow.getTarget().getName();
            }
        }
        return "dem nächsten Schritt";
    }

    private static String getPreviousElementName(FlowNode node) {
        for (SequenceFlow flow : node.getIncoming()) {
            if (flow.getTarget() != null) {
                return flow.getTarget().getName();
            }
        }
        return "dem nächsten Schritt";
    }

    
    ///// Übersetzen Events

    private static String getEventType(IntermediateCatchEvent event) {
    // Prüfen, ob Event-Definitionen vorhanden sind
    if (event.getEventDefinitions().isEmpty()) {
        // Zugriff auf das XML-Element des Events
        DomElement domElement = event.getDomElement();
        Optional<DomElement> definitionsElement = domElement.getChildElements().stream()
            .filter(e -> e.getLocalName().contains("EventDefinition")) // Suche nach Elementen, die "EventDefinition" im Namen haben
            .findFirst();

        if (definitionsElement.isPresent()) {
            String localName = definitionsElement.get().getLocalName();
            switch (localName) {
                case "timerEventDefinition":
                    return "Timer-Event";
                case "messageEventDefinition":
                    return "Message-Event";
                // Add other cases as necessary
                default:
                    return "Unbekanntes Event (" + localName + ")";
            }
        } else {
        return "CatchEvent (keine spezifische Event-Definition)";
        }
    }
    
    // StringBuilder für Event-Typen
    StringBuilder eventTypes = new StringBuilder();
    for (EventDefinition eventDefinition : event.getEventDefinitions()) {
        String eventTypeName;
        if (eventDefinition instanceof TimerEventDefinition) {
            eventTypeName = "Timer-Event";
        } else if (eventDefinition instanceof MessageEventDefinition) {
            eventTypeName = "Message-Event";
        } else {
            // Use the simple class name as a fallback
            eventTypeName = eventDefinition.getClass().getSimpleName().replace("Impl", "");
        }

        if (eventTypes.length() > 0) {
            eventTypes.append(", ");
        }
        eventTypes.append(eventTypeName);
    }
        return eventTypes.toString();
    }

     
    

    ///// Übersetzen Taskbeschreibung

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


   
    ///// Übersetzen Input und Output für Tasks
    
    private static void appendIoSpecification(Task task, StringBuilder sb) {
        IoSpecification ioSpec = task.getIoSpecification();
        if (ioSpec != null) {
            ioSpec.getDataInputs().forEach(input -> sb
                .append("  Diese Aufgabe erhält bzw. benötigt folgenden Input: ")
                .append(input.getName())
                .append("\n"));
            ioSpec.getDataOutputs().forEach(output -> sb
                .append("  Diese Aufgabe erstellt bzw. gibt folgenden Output aus: ")
                .append(output.getName())
                .append("\n"));
        }
    }
    

    ///// Schreiben der Textdatei

    private static void writeToFile(String filePath, String content) {
        File outputFile = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {  // false überschreibt die Datei
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }


    ///// Debug
    private static void printSequenceFlowDetails(Process process) {
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
