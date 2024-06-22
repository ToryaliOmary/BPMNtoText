package com.example;

import org.camunda.bpm.model.bpmn.instance.IoSpecification;
import org.camunda.bpm.model.bpmn.instance.Task;

public class IoSpecificationUtils {
    public static void appendIoSpecification(Task task, StringBuilder sb) {
        IoSpecification ioSpec = task.getIoSpecification();
        if (ioSpec != null) {
            ioSpec.getDataInputs().forEach(input -> sb
                    .append(String.format("  Diese Aufgabe erhält bzw. benötigt folgenden Input: '%s' \n", input.getName())));
            ioSpec.getDataOutputs().forEach(output -> sb
                    .append(String.format("  Diese Aufgabe erstellt bzw. gibt folgenden Output aus: '%s' \n ", output.getName())));
        }
    }
}
