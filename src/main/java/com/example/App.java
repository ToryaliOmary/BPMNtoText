package com.example;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

//Testprozess 1 - normal:   export-5.5a     id: _a0c2e84e-6a29-409b-bc16-6f0b3b08c7ef
//Testrpozess 2 - Gateway:  Prozess         id: Process_0jjby6t

public class App {
    public static void main(String[] args) {
        // Lade das BPMN-Diagramm
        File file = new File("src/main/export-5.5a.bpmn");
        if (!file.exists()) {
            System.err.println("Datei nicht gefunden: " + file.getAbsolutePath());
            return;
        }

        BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
        if (modelInstance == null) {
            System.err.println("ModelInstance konnte nicht geladen werden.");
            return;
        }

        // Extrahiere den Prozess nach der gegebenen ID
        Process process = (Process) modelInstance.getModelElementById("_a0c2e84e-6a29-409b-bc16-6f0b3b08c7ef");
        if (process == null) {
            System.err.println("Process-id nicht gefunden");
            return;
        }

        // Finde das Start-Ereignis
        StartEvent startEvent = null;
        Collection<FlowElement> flowElements = process.getFlowElements();
        for (FlowElement element : flowElements) {
            if (element instanceof StartEvent) {
                startEvent = (StartEvent) element;
                break;
            }
        }
        if (startEvent == null) {
            System.err.println("Start-Ereignis nicht gefunden.");
            return;
        }

        // Generiere den Fließtext
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
                    text.append("Der Prozess beginnt mit dem Start-Ereignis: \"").append(flowNode.getName()).append("\".\n");
                } else if (flowNode instanceof Task) {
                    Task task = (Task) flowNode;
                    String nextTask = getNextTaskName(task);
                    text.append("Die Aufgabe '").append(task.getName()).append("' wird von der Rolle '").append(lane.getName())
                        .append("' durchgeführt.\n  Diese Aufgabe umfasst {kurze Beschreibung der Aufgabe}.\n  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an '")
                        .append(nextTask).append("' übergeben.\n");
                } else if (flowNode instanceof Gateway) {
                    Gateway gateway = (Gateway) flowNode;
                    text.append("  An einem Gateway (").append(gateway.getElementType().getTypeName()).append(") namens \"").append(gateway.getName()).append("\" kann der Prozess unterschiedliche Wege gehen, abhängig von der Entscheidungslogik.\n");
                    Collection<SequenceFlow> outgoing = gateway.getOutgoing();
                    for (SequenceFlow flow : outgoing) {
                        String condition = flow.getConditionExpression() != null ? flow.getConditionExpression().getTextContent() : "Unbekannt";
                        text.append("    Entscheidung '").append(condition).append("' führt zu: ").append(flow.getTarget().getName()).append(".\n");
                    }
                } else if (flowNode instanceof EndEvent) {
                    text.append("  Der Prozess endet mit dem End-Ereignis \"").append(flowNode.getName()).append("\".\n");
                }
            }
        }

        // Schreibe den generierten Text in eine neue Datei
        File outputFile = new File("prozessbeschreibung.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {  // false überschreibt die Datei
            writer.write(text.toString());
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }

        // Ausgabe in der Konsole
        System.out.println("Der Text wurde in " + outputFile.getAbsolutePath() + " gespeichert.");
    }

    private static String getNextTaskName(FlowNode node) {
        for (SequenceFlow flow : node.getOutgoing()) {
            if (flow.getTarget() instanceof Task) {
                return flow.getTarget().getName();
            }
        }
        return "dem nächsten Schritt";
    }
}
