package com.example;

import org.activiti.bpmn.model.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.EventDefinition;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.activiti.bpmn.model.TimerEventDefinition;

import java.util.Optional;

public class EventUtils {
    public static String getEventType(IntermediateCatchEvent event) {
        if (event.getEventDefinitions().isEmpty()) {
            DomElement domElement = event.getDomElement();
            Optional<DomElement> definitionsElement = domElement.getChildElements().stream()
                    .filter(e -> e.getLocalName().contains("EventDefinition"))
                    .findFirst();

            if (definitionsElement.isPresent()) {
                String localName = definitionsElement.get().getLocalName();
                switch (localName) {
                    case "timerEventDefinition":
                        return "Timer-Event";
                    case "messageEventDefinition":
                        return "Message-Event";
                    default:
                        return "Unbekanntes Event (" + localName + ")";
                }
            } else {
                return "CatchEvent (keine spezifische Event-Definition)";
            }
        }

        StringBuilder eventTypes = new StringBuilder();
        for (EventDefinition eventDefinition : event.getEventDefinitions()) {
            String eventTypeName;
            if (eventDefinition instanceof TimerEventDefinition) {
                eventTypeName = "Timer-Event";
            } else if (eventDefinition instanceof MessageEventDefinition) {
                eventTypeName = "Message-Event";
            } else {
                eventTypeName = eventDefinition.getClass().getSimpleName().replace("Impl", "");
            }

            if (eventTypes.length() > 0) {
                eventTypes.append(", ");
            }
            eventTypes.append(eventTypeName);
        }
        return eventTypes.toString();
    }
}
