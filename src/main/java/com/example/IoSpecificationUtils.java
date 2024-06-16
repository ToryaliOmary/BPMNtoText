package com.example;

import org.camunda.bpm.model.bpmn.instance.IoSpecification;
import org.camunda.bpm.model.bpmn.instance.Task;

public class IoSpecificationUtils {
    public static void appendIoSpecification(Task task, StringBuilder sb) {
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
}
