package com.example;

import org.camunda.bpm.model.bpmn.instance.*;

public class ElementProcessor {
    public static void processElement(FlowNode flowNode, String performerName,  StringBuilder text) {
        //String laneName = (lane != null) ? lane.getName() : "Unbekannte Lane";
        if (flowNode instanceof StartEvent) {
            StartEvent startEvent = (StartEvent) flowNode;
            String nextTaskName = TaskUtils.getNextElementName(startEvent);
            text.append(String.format("Der Prozess beginnt mit Event '%s'. Dieses Event setzt die erste Aufgabe '%s' in Gang.\n", startEvent.getName(), nextTaskName));
        } else if (flowNode instanceof EndEvent) {
            EndEvent endEvent = (EndEvent) flowNode;
            String previousTaskName = TaskUtils.getPreviousElementName(flowNode);
            text.append(String.format("\n Das Element '%s' stellt das End-Event des Prozesses dar. \n Dieses Ereignis tritt ein, nachdem die letzte Aufgabe '%s' abgeschlossen wurde, und signalisiert das Abschließen des Gesamtprozesses.\n", endEvent.getName(), previousTaskName));
        } else if (flowNode instanceof IntermediateCatchEvent) {
            IntermediateCatchEvent intermediateCatchEvent = (IntermediateCatchEvent) flowNode;
            String eventDescription = TaskUtils.extractHtmlContentEvent(intermediateCatchEvent);
            text.append(String.format("\n Es folgt ein '%s' mit dem Namen '%s' \n", EventUtils.getEventType(intermediateCatchEvent), intermediateCatchEvent.getName()));
            if (eventDescription != null){
                text.append(String.format("  Dieses Event ist folgendermaßen beschrieben:\n  '%s'.\n Anschließend wird der Prozess fortgesetzt.\n", eventDescription));
            }
        } else if (flowNode instanceof Task) {
            Task task = (Task) flowNode;
            String taskDescription = TaskUtils.extractHtmlContentTask(task);
            FlowNode nextElement = TaskUtils.getNextElement(task);
            text.append(String.format("\n Die Aufgabe '%s' wird von der Rolle '%s' durchgeführt. \n  Diese Aufgabe ist folgendermaßen beschrieben: \n  '%s'. \n", task.getName(), performerName,  taskDescription));
            if (nextElement instanceof IntermediateCatchEvent) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append("\n An dieser Stelle tritt innerhalb des Prozesses ein Zwischenereignis auf.");
            } else if (nextElement instanceof Gateway) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append(String.format("\n An dieser Stelle tritt innerhalb des Prozesses ein Gateway names '%s' auf.", nextElement.getName()));
            } else if (nextElement instanceof Task){
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append(String.format("  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an das nächste Element '%s' übergeben.\n ", nextElement.getName()));
            }
        } else if (flowNode instanceof Gateway) {
            Gateway gateway = (Gateway) flowNode;
            switch (gateway.getElementType().getTypeName()) {
                case "parallelGateway":
                    if (gateway.getIncoming().size() <= 1) {
                        text.append(String.format("\n Das Gateway vom Typ %s namens '%s'. \n", gateway.getElementType().getTypeName(), gateway.getName()));
                        text.append(" Nun werden parallel folgende Tasks durchgeführt: \n ");
                        for (SequenceFlow flow : gateway.getOutgoing()) {
                            text.append(" Task: ")
                                .append(flow.getTarget().getName())
                                .append("\n ");
                        }
                    } else if (gateway.getIncoming().size() >= 2) {
                        text.append("\n Hier schließt sich das zuvor geöffnete parallele Gateway wieder.\n");
                    }
                    break;
                
                default:
                    if (gateway.getIncoming().size() == 1){
                        text.append(String.format("\n Es folgt ein Gateway vom Typ %s namens '%s'. \n", gateway.getElementType().getTypeName(), gateway.getName()));
                        text.append(" Abhängig von der Entscheidungslogik kann der Prozess nun unterschiedliche Wege gehen.\n");
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
                    }else {
                        text.append(" \n Dieses Gateway schließt ein oder mehr geöffnete Gateways und symbolisiert einen Loop.");

                    }
                    break;
            }
            
        }
    }
}