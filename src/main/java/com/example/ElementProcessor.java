package com.example;

import org.camunda.bpm.model.bpmn.instance.*;

public class ElementProcessor {
    public static void processElement(FlowNode flowNode, Lane lane, StringBuilder text) {
        if (flowNode instanceof StartEvent) {
            StartEvent startEvent = (StartEvent) flowNode;
            String nextTaskName = TaskUtils.getNextElementName(startEvent);
            text.append(String.format("Der Prozess beginnt mit Event ‚%s‘. Dieses Event setzt die erste Aufgabe ‚%s‘ in Gang.\n", startEvent.getName(), nextTaskName));
        } else if (flowNode instanceof EndEvent) {
            EndEvent endEvent = (EndEvent) flowNode;
            String previousTaskName = TaskUtils.getPreviousElementName(flowNode);
            text.append(String.format("Das Element ‚%s‘ stellt das End-Event des Prozesses dar. Dieses Ereignis tritt ein, nachdem die letzte Aufgabe ‚%s‘ abgeschlossen wurde, und signalisiert das Abschließen des Gesamtprozesses.\n\n", endEvent.getName(), previousTaskName));
        } else if (flowNode instanceof IntermediateCatchEvent) {
            IntermediateCatchEvent intermediateCatchEvent = (IntermediateCatchEvent) flowNode;
            String nextTaskName = TaskUtils.getNextElementName(intermediateCatchEvent);
            text.append("  Es folgt ein \"")
                    .append(EventUtils.getEventType(intermediateCatchEvent))
                    .append("\" mit dem Namen \"")
                    .append(intermediateCatchEvent.getName())
                    .append(String.format(".\n  Anschließend wird der Prozess mit dem nachfolgenden Element ‚%s‘ weitergeführt.\n", nextTaskName));
        } else if (flowNode instanceof Task) {
            Task task = (Task) flowNode;
            String taskDescription = TaskUtils.extractHtmlContent(task);
            FlowNode nextElement = TaskUtils.getNextElement(task);

            text.append("Die Aufgabe '")
                    .append(task.getName())
                    .append("' wird von der Rolle '")
                    .append(lane.getName())
                    .append("' durchgeführt.\n  Diese Aufgabe ist folgendermaßen beschrieben: \n   '")
                    .append(taskDescription)
                    .append("'");

            if (nextElement instanceof IntermediateCatchEvent) {
                text.append("\nAn dieser Stelle tritt innerhalb des Prozesses ein Zwischenereignis auf.\n");
            } else if (nextElement != null) {
                text.append("\n  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an das nächste Element '")
                        .append(nextElement.getName())
                        .append("' übergeben.\n");
            } else if (nextElement instanceof Gateway) {
                text.append("");
            } else {
                text.append("\n  Es gibt keinen definierten nächsten Schritt nach dieser Aufgabe.\n");
            }
            IoSpecificationUtils.appendIoSpecification(task, text);
        } else if (flowNode instanceof Gateway) {
            Gateway gateway = (Gateway) flowNode;
            text.append("An dieser Stelle tritt innerhalb des Prozesses ein Gateway bzw. eine Entscheidung vom Typ ")
                    .append(gateway.getElementType().getTypeName())
                    .append(" namens \"")
                    .append(gateway.getName())
                    .append("\" auf.\n Abhängig von der Entscheidungslogik kann der Prozess nun unterschiedliche Wege gehen.\n");
            for (SequenceFlow flow : gateway.getOutgoing()) {
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
