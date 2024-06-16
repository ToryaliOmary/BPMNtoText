package com.example;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DescriptionGenerator {
    public static String generateProcessDescription(BpmnModelInstance modelInstance, Process process) {
        StringBuilder text = new StringBuilder();
        Collection<Lane> lanes = new ArrayList<>();
        for (ModelElementInstance instance : modelInstance.getModelElementsByType(modelInstance.getModel().getType(Lane.class))) {
            if (instance instanceof Lane) {
                lanes.add((Lane) instance);
            }
        }

        for (Lane lane : lanes) {
            List<FlowNode> laneFlowNodes = new ArrayList<>(lane.getFlowNodeRefs());

            for (FlowNode flowNode : laneFlowNodes) {
                ElementProcessor.processElement(flowNode, lane, text);
            }
        }
        return text.toString();
    }
}
