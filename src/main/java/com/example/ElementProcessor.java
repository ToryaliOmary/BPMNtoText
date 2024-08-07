package com.example;

import org.camunda.bpm.model.bpmn.instance.*;

import java.util.HashSet;
import java.util.Set;

public class ElementProcessor {
    private static final Set<FlowNode> visitedNodes = new HashSet<>();

    public static void processElement1(FlowNode flowNode, Lane lane, StringBuilder text) {
        if (visitedNodes.contains(flowNode)) {
            return;
        }
        visitedNodes.add(flowNode);

        if (flowNode instanceof StartEvent) {
            StartEvent startEvent = (StartEvent) flowNode;
            String nextTaskName = TaskUtils.getNextElementName(startEvent);
            text.append(String.format("Der Prozess beginnt mit Event '%s' in der Lane '%s'. Dieses Event setzt die erste Aufgabe '%s' in Gang.\n", startEvent.getName(), lane.getName(), nextTaskName));
        } else if (flowNode instanceof EndEvent) {
            EndEvent endEvent = (EndEvent) flowNode;
            String previousTaskName = TaskUtils.getPreviousElementName(flowNode);
            text.append(String.format("\n Das Element '%s' in der Lane '%s' stellt das End-Event des Prozesses dar. \n Dieses Ereignis tritt ein, nachdem die letzte Aufgabe '%s' abgeschlossen wurde, und signalisiert das Abschließen des Gesamtprozesses.\n", endEvent.getName(), lane.getName(), previousTaskName));
        } else if (flowNode instanceof IntermediateCatchEvent) {
            IntermediateCatchEvent intermediateCatchEvent = (IntermediateCatchEvent) flowNode;
            text.append(String.format("\n Es folgt ein '%s' mit dem Namen '%s' in der Lane '%s' \n", EventUtils.getEventType(intermediateCatchEvent), intermediateCatchEvent.getName(), lane.getName()));
        } else if (flowNode instanceof Task) {
            Task task = (Task) flowNode;
            String taskDescription = TaskUtils.extractHtmlContentTask(task);
            FlowNode nextElement = TaskUtils.getNextElement(task);
            text.append(String.format("\n Die Aufgabe '%s' wird von der Rolle '%s' durchgeführt. \n  Diese Aufgabe ist folgendermaßen beschrieben: \n  '%s'. \n", task.getName(), lane.getName(), taskDescription));
            if (nextElement instanceof IntermediateCatchEvent) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append("\n An dieser Stelle tritt innerhalb des Prozesses ein Zwischenereignis auf.");
            } else if (nextElement instanceof Gateway) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                switch (nextElement.getElementType().getTypeName()) {
                    case "parallelGateway":
                        break;
                    default:
                    text.append("\n An dieser Stelle tritt innerhalb des Prozesses ein Gateway auf.");                        break;
                }
            } else if (nextElement instanceof Task) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append(String.format("  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an das nächste Element '%s' übergeben.\n ", nextElement.getName()));
            }
        } else if (flowNode instanceof Gateway) {
            processGateway1((Gateway) flowNode, lane, text);
        }
    }

    public static void processElement2(FlowNode flowNode, StringBuilder text) {
        if (visitedNodes.contains(flowNode)) {
            return;
        }
        visitedNodes.add(flowNode);

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
            if (eventDescription != null) {
                text.append(String.format("  Dieses Event ist folgendermaßen beschrieben:\n  '%s'.\n Anschließend wird der Prozess fortgesetzt.\n", eventDescription));
            }
        } else if (flowNode instanceof Task) {
            Task task = (Task) flowNode;
            String taskDescription = TaskUtils.extractHtmlContentTask(task);
            FlowNode nextElement = TaskUtils.getNextElement(task);
            text.append(String.format("\n Die Aufgabe '%s' wird durchgeführt. \n  Diese Aufgabe ist folgendermaßen beschrieben: \n  '%s'. \n", task.getName(), taskDescription));
            if (nextElement instanceof IntermediateCatchEvent) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append("\n An dieser Stelle tritt innerhalb des Prozesses ein Zwischenereignis auf.");
            } else if (nextElement instanceof Gateway) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                switch (nextElement.getElementType().getTypeName()) {
                    case "parallelGateway":
                        break;
                    default:
                    text.append("\n An dieser Stelle tritt innerhalb des Prozesses ein Gateway auf.");                        break;
                }            
            } else if (nextElement instanceof Task) {
                IoSpecificationUtils.appendIoSpecification(task, text);
                text.append(String.format("  Nach Abschluss dieser Aufgabe werden die Ergebnisse/Daten an das nächste Element '%s' übergeben.\n ", nextElement.getName()));
            }
        } else if (flowNode instanceof Gateway) {
            processGateway2((Gateway) flowNode, text);
        }
    }

    private static void processGateway1(Gateway gateway, Lane lane, StringBuilder text) {
        switch (gateway.getElementType().getTypeName()) {
            case "parallelGateway":
                processParallelGateway1(gateway, lane, text);
                break;
            default:
                processDefaultGateway1(gateway, lane, text);
                break;
        }
    }

    private static void processGateway2(Gateway gateway, StringBuilder text) {
        switch (gateway.getElementType().getTypeName()) {
            case "parallelGateway":
                processParallelGateway2(gateway, text);
                break;
            default:
                processDefaultGateway2(gateway, text);
                break;
        }
    }

    private static void processParallelGateway1(Gateway gateway, Lane lane, StringBuilder text) {
        if (gateway.getIncoming().size() <= 1) {
            text.append(String.format("\n Es folgt ein Gateway vom Typ %s namens '%s'. \n", gateway.getElementType().getTypeName(), gateway.getName()));
            text.append(" Nun werden parallel folgende Tasks durchgeführt: \n ");
            
            // Liste der parallelen Tasks sammeln
            StringBuilder parallelTasks = new StringBuilder();
            for (SequenceFlow flow : gateway.getOutgoing()) {
                parallelTasks.append(" Task: ")
                        .append(flow.getTarget().getName())
                        .append("\n ");
            }
            
            // Parallele Tasks hinzufügen
            text.append(parallelTasks);

            // Details der parallelen Tasks hinzufügen
            for (SequenceFlow flow : gateway.getOutgoing()) {
                processElement1(flow.getTarget(), lane, text);
            }
        } else if (gateway.getIncoming().size() >= 2) {
            text.append("\n Hier schließt sich das zuvor geöffnete parallele Gateway wieder.\n");
        }
    }

    private static void processParallelGateway2(Gateway gateway, StringBuilder text) {
        if (gateway.getIncoming().size() <= 1) {
            text.append(String.format("\n Es folgt ein Gateway vom Typ %s namens '%s'. \n", gateway.getElementType().getTypeName(), gateway.getName()));
            text.append(" Nun werden parallel folgende Tasks durchgeführt: \n ");

            // Liste der parallelen Tasks sammeln
            StringBuilder parallelTasks = new StringBuilder();
            for (SequenceFlow flow : gateway.getOutgoing()) {
                parallelTasks.append(" Task: ")
                        .append(flow.getTarget().getName())
                        .append("\n ");
            }
            
            // Parallele Tasks hinzufügen
            text.append(parallelTasks);

            // Details der parallelen Tasks hinzufügen
            for (SequenceFlow flow : gateway.getOutgoing()) {
                processElement2(flow.getTarget(), text);
            }
        } else if (gateway.getIncoming().size() >= 2) {
            text.append("\n Hier schließt sich das zuvor geöffnete parallele Gateway wieder.\n");
        }
    }

    private static void processDefaultGateway1(Gateway gateway, Lane lane, StringBuilder text) {
        if (gateway.getIncoming().size() >= 2) {
            text.append(String.format("\n Es folgt ein Gateway namens '%s'. \n", gateway.getName()));
            text.append(" Dieses Gateway dient dem Zusammenführen verschiedener Pfäde, schließt zuvor geöffnete Gateways und symbolisiert einen Loop. \n");
        } else {
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
        }
    }

    private static void processDefaultGateway2(Gateway gateway, StringBuilder text) {
        if (gateway.getIncoming().size() >= 2) {
            text.append(String.format("\n Es folgt ein Gateway namens '%s'. \n", gateway.getName()));
            text.append(" Dieses Gateway dient dem Zusammenführen verschiedener Pfäde, schließt zuvor geöffnete Gateways und symbolisiert einen Loop. \n");
        } else {
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
        }
    }
}
