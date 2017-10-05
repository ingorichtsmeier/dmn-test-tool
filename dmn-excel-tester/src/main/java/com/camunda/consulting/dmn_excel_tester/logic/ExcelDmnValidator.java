package com.camunda.consulting.dmn_excel_tester.logic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.InformationRequirement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.RequiredDecisionReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelDmnValidator {
  
  private static final Logger log = LoggerFactory.getLogger(ExcelDmnValidator.class);
  
  private Map<String, List<Map<String, Object>>> dataFromExcel;
  private DmnModelInstance dmnModelInstance;
  private Map<String, Decision> decisionMap;

  public ExcelDmnValidator(Map<String, List<Map<String, Object>>> dataFromExcel2, DmnModelInstance dmnModelInstance) {
    this.dataFromExcel = dataFromExcel2;
    this.dmnModelInstance = dmnModelInstance;
    this.decisionMap = new HashMap<String, Decision>();
  }
  
  public List<String> validateMatchingExcelAndDmnModel() {
    List<String> errorResultList = new ArrayList<String>();
      
    for (Entry<String, List<Map<String, Object>>> sheetData : dataFromExcel.entrySet()) {
      List<String> dmnInputHeaderLabels = new ArrayList<String>();
      Collection<Decision> decisionList = dmnModelInstance.getModelElementsByType(Decision.class);
      for (Decision decision : decisionList) {
        log.info("decision name: {}", decision.getName());
        decisionMap.put(decision.getId(), decision);
        if (decision.getName().equals(sheetData.getKey()) || decisionList.size() == 1) {
          DecisionTable decisionTable = decision.getChildElementsByType(DecisionTable.class).iterator().next();
          dmnInputHeaderLabels = collectInputHeaderLabels(decisionTable, dmnInputHeaderLabels);

          Collection<InformationRequirement> informationRequirements = decision
              .getChildElementsByType(InformationRequirement.class);
          if (informationRequirements.isEmpty() == false) {
            Collection<RequiredDecisionReference> requiredDecisions = informationRequirements.iterator().next()
                .getChildElementsByType(RequiredDecisionReference.class);
            for (RequiredDecisionReference requiredDecisionReference : requiredDecisions) {
              log.info("Required decision {}", requiredDecisionReference.getHref());
              dmnInputHeaderLabels.addAll(getInputHeadersFromRequirement(requiredDecisionReference.getHref().substring(1)));
            }
          } else {
            log.info("No information requirements");
          }

          List<String> dmnOutputHeaderLables = new ArrayList<String>();
          Collection<Output> dmnOutputHeaders = decisionTable.getChildElementsByType(Output.class);
          for (Iterator<Output> iterator = dmnOutputHeaders.iterator(); iterator.hasNext();) {
            Output dmnOutputHeader = (Output) iterator.next();
            log.info("DmnOutputHaeder: {}", dmnOutputHeader.getLabel());
            dmnOutputHeaderLables.add(dmnOutputHeader.getLabel());
          }

          Map<String, Object> excelHeaders = sheetData.getValue().get(1);
          Collection<Object> headers = excelHeaders.values();
          for (Iterator<Object> iterator = headers.iterator(); iterator.hasNext();) {
            String header = (String) iterator.next();
            log.info("validate header {}", header);
            if (header.startsWith("Expected: ")) {
              // output column
              String outputHeader = header.replaceAll("Expected: ", "");
              if (dmnOutputHeaderLables.contains(outputHeader)) {
                // everything ok
              } else {
                String message = MessageFormat.format("Column ''{0}'' not found in Dmn Output of table ''{1}''", outputHeader, decision.getName());
                errorResultList.add(message);
              }
            } else {
              // input column
              if (dmnInputHeaderLabels.contains(header)) {
                // everything ok
              } else {
                String message = MessageFormat.format("Column ''{0}'' not found in Dmn Inputs of table ''{1}''", header, decision.getName());
                errorResultList.add(message);
              }
            }
          }
        } else {
          log.info("don't compare decision {} with expectations from {}", decision.getName(), sheetData.getKey());
        }
      }
    }
    return errorResultList;
  }

  private List<String> collectInputHeaderLabels(DecisionTable decisionTable, List<String> dmnInputHeaderLabels) {
    Collection<Input> dmnInputHeaders = decisionTable.getChildElementsByType(Input.class);
    for (Iterator<Input> iterator = dmnInputHeaders.iterator(); iterator.hasNext();) {
      Input dmnInputHeader = (Input) iterator.next();
      log.info("DmnInputHeader: {}", dmnInputHeader.getLabel());
      dmnInputHeaderLabels.add(dmnInputHeader.getLabel());
    }
    return dmnInputHeaderLabels;
  }

  private Collection<String> getInputHeadersFromRequirement(String decisionName) {
    log.info("collect input headers for required decision {}", decisionName);
    List<String> requiredHeaders = new ArrayList<String>();
    Decision decision = decisionMap.get(decisionName);
    DecisionTable decisionTable = decision.getChildElementsByType(DecisionTable.class).iterator().next();
    return collectInputHeaderLabels(decisionTable, requiredHeaders);
  }

}
