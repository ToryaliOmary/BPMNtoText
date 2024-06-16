package com.example;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.io.File;

public class BpmnLoader {
    public static BpmnModelInstance loadBpmnModel(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Datei nicht gefunden: " + file.getAbsolutePath());
            return null;
        }
        return Bpmn.readModelFromFile(file);
    }
}
