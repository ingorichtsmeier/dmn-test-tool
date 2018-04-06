package com.camunda.consulting.dmn_excel_tester.logic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.dmn.engine.DmnDecision;
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
  
  public static Function<List<DmnDecision>, Map<String, DmnDecision>> mapDmnDecisionByNames = 
      (List<DmnDecision> decisions) -> decisions.stream().collect(Collectors.toMap(d -> d.getName(), d -> d));

  public static Function<DmnModelInstance, Map<String, Decision>> mapDecisionsByNames = 
      (DmnModelInstance decisionModel) -> decisionModel.getModelElementsByType(Decision.class).stream().collect(Collectors.toMap(d -> d.getName(), d -> d));

  public static BiFunction<List<String>, List<String>, String> checkMatchingSheetAndTableNames = 
      (List<String> sheetNames, List<String> decisionNames) -> {
        if (sheetNames.size() == 1 && decisionNames.size() == 1) {
          return "";
        }
        StringBuilder result = new StringBuilder();
        List<String> unmatchedSheets = sheetNames.stream().filter(sheet -> (decisionNames.contains(sheet) == false)).collect(Collectors.toList());
        List<String> unmatchedDecisions = decisionNames.stream().filter(decision -> (sheetNames.contains(decision) == false)).collect(Collectors.toList());

        if (unmatchedSheets.size() > 0) {
          result.append("Excel sheet");
          result.append((unmatchedSheets.size() > 1) ? "s " : " ");
          result.append(unmatchedSheets.stream().map(sheet -> "'" + sheet + "'").collect(Collectors.joining(" and ")));
          result.append(" didn't match decision");
          result.append((unmatchedDecisions.size() > 1) ? "s " : " ");
          result.append(unmatchedDecisions.stream().map(decision -> "'" + decision + "'").collect(Collectors.joining(" or ")));
        }
        return result.toString();
      };

  public static BiFunction<Map<String, List<Map<String, Object>>>, DmnModelInstance, String> validateMatchingSheetsAndTables = 
      (Map<String, List<Map<String, Object>>> dataFromExcel_, DmnModelInstance dmnModelInstance_) -> {
        Collection<Decision> decisions = dmnModelInstance_.getModelElementsByType(Decision.class);
        List<String> decisionNames = decisions.stream().map(decision -> decision.getName()).collect(Collectors.toList());
        List<String> sheetNames = dataFromExcel_.keySet().stream().collect(Collectors.toList());
        return checkMatchingSheetAndTableNames.apply(sheetNames, decisionNames);
      };

  public static BiFunction<List<Map<String, Object>>, Decision, List<String>> checkHeaders = (List<Map<String, Object>> sheetContent, Decision decision) -> {
    List<String> errorList = new ArrayList<String>();
    String decisionName = decision.getName();
    List<String> inputHeaders = decision
        .getModelInstance()
        .getModelElementsByType(Input.class)
        .stream()
        .map(input -> input.getLabel())
        .collect(Collectors.toList());
    Stream<String> sheetInputHeaders = sheetContent.get(1).values()
        .stream()
        .map(header -> header.toString())
        .filter(header -> (header.startsWith("Expected: ") == false));
    errorList.addAll(sheetInputHeaders
        .filter(header -> (inputHeaders.contains(header) == false))
        .map(error -> "Column '" + error + "' not found in Dmn Input of table '" + decisionName + "'")
        .collect(Collectors.toList()));
    
    List<String> outputHeaders = decision
        .getModelInstance()
        .getModelElementsByType(Output.class)
        .stream()
        .map(output -> output.getLabel())
        .collect(Collectors.toList());
    Stream<String> sheetOutputHeaders = sheetContent.get(1).values()
        .stream()
        .map(header -> header.toString())
        .filter(header -> header.startsWith("Expected: "))
        .map(header -> header.replaceAll("Expected: ", ""));
    errorList.addAll(sheetOutputHeaders
        .filter(header -> (outputHeaders.contains(header) == false))
        .map(error -> "Column '" + error + "' not found in Dmn Output of table '" + decisionName + "'")
        .collect(Collectors.toList()));
    return errorList;
  };
  
  public static BiFunction<DmnModelInstance, String, Decision> findDecisionForSheet = 
      (DmnModelInstance dmnModelInstance, String sheetName) -> {
        Map<String, Decision> decisionMap = mapDecisionsByNames.apply(dmnModelInstance);
        if (decisionMap.entrySet().size() == 1) {
          return decisionMap.entrySet().iterator().next().getValue();
        } else {
          return decisionMap.get(sheetName);
        }
      };

  public static BiFunction<Map<String, List<Map<String, Object>>>, DmnModelInstance, List<String>> validateExcelAndDmnModel = 
      (Map<String, List<Map<String, Object>>> dataFromExcel2, DmnModelInstance dmnModelInstance) -> {
        List<String> errorResultList = new ArrayList<String>();
        String sheetsAndTablesValidationError = validateMatchingSheetsAndTables.apply(dataFromExcel2, dmnModelInstance);
        if (sheetsAndTablesValidationError.isEmpty() == false) {
          errorResultList.add(sheetsAndTablesValidationError);
          return errorResultList;
        }
        
        errorResultList.addAll(dataFromExcel2
            .entrySet()
            .stream()
            .flatMap((element) -> checkHeaders.apply(
                element.getValue(), 
                findDecisionForSheet.apply(dmnModelInstance, element.getKey())).stream())
            .collect(Collectors.toList()));
        
        return errorResultList;
      };
  
  public List<String> validateMatchingExcelAndDmnModel_delete() {
    List<String> errorResultList = new ArrayList<String>();
    
    String sheetsAndTablesValidationError = validateMatchingSheetsAndTables.apply(dataFromExcel, dmnModelInstance);
    if (sheetsAndTablesValidationError.isEmpty() == false) {
      errorResultList.add(sheetsAndTablesValidationError);
      return errorResultList;
    }
      
    Collection<Decision> decisionList = dmnModelInstance.getModelElementsByType(Decision.class);
    for (Decision decision : decisionList) {
      log.info("decision name: {}", decision.getName());
      decisionMap.put(decision.getId(), decision);
      for (Entry<String, List<Map<String, Object>>> sheetData : dataFromExcel.entrySet()) {
        List<String> dmnInputHeaderLabels = new ArrayList<String>();
        
        if (decision.getName().equals(sheetData.getKey()) || decisionList.size() == 1) {
          DecisionTable decisionTable = decision.getChildElementsByType(DecisionTable.class).iterator().next();
          dmnInputHeaderLabels = collectInputHeaderLabels(decisionTable/*, dmnInputHeaderLabels*/);

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
                String message = MessageFormat.format("Column ''{0}'' not found in Dmn Input of table ''{1}''", header, decision.getName());
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
  
  private List<String> collectInputHeaderLabels(DecisionTable decisionTable /*, List<String> dmnInputHeaderLabels*/) {
    List<String> dmnInputHeaderLabels = new ArrayList<String>();
    Collection<Input> dmnInputHeaders = decisionTable.getChildElementsByType(Input.class);
    dmnInputHeaders.forEach(dmnInputHeader -> {
      log.info("DmnInputHeader: {}", dmnInputHeader.getLabel());
      dmnInputHeaderLabels.add(dmnInputHeader.getLabel());
    });
    return dmnInputHeaderLabels;
  }
  
  private Collection<String> getInputHeadersFromRequirement(String decisionName) {
    log.info("collect input headers for required decision {}", decisionName);
    Decision decision = decisionMap.get(decisionName);
    DecisionTable decisionTable = decision.getChildElementsByType(DecisionTable.class).iterator().next();
    return collectInputHeaderLabels(decisionTable/*, new ArrayList<String>()*/);
  }
}
