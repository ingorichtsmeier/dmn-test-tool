package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

import com.camunda.consulting.dmn_excel_tester.functional.Tuple;

public class ExcelDmnValidator {
  
  private static final Logger log = LoggerFactory.getLogger(ExcelDmnValidator.class);
  
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
      
  public static BiFunction<DmnModelInstance, String, Stream<String>> getInputHeadersForDecisionId = 
      (DmnModelInstance dmnModelInstance, String decisionId) -> {
        Decision foundDecision = dmnModelInstance.getModelElementsByType(Decision.class)
            .stream()
            .filter(decision -> (decision.getId().equals(decisionId)))
            .findFirst().get();
        Collection<Input> inputHeaders = foundDecision
            .getChildElementsByType(DecisionTable.class).iterator().next()
            .getChildElementsByType(Input.class);
        return inputHeaders.stream().map(input -> input.getLabel());
      };
      
  public static BiFunction<DmnModelInstance, String, Stream<String>> getOutputLabelsForDecisionId = 
      (DmnModelInstance dmnModelInstance, String decisionId) -> {
        Decision foundDecision = dmnModelInstance.getModelElementsByType(Decision.class)
            .stream()
            .filter(decision -> (decision.getId().equals(decisionId)))
            .findFirst().get();
        Collection<Output> outputHeaders = foundDecision.getChildElementsByType(DecisionTable.class).iterator().next().getChildElementsByType(Output.class);
        return outputHeaders.stream().map(output -> output.getLabel());
      };

  public static BiFunction<DmnModelInstance, Decision, List<Tuple<List<String>, List<String>>>> createInputToReplaceList = 
      (DmnModelInstance dmnModelInstance, Decision decision) -> {
        Stream<String> requiredDecisionIds = decision
            .getChildElementsByType(InformationRequirement.class)
            .stream()
            .map(element -> element.getChildElementsByType(RequiredDecisionReference.class).iterator().next())
            .map(required -> required.getHref().substring(1));
        
        return requiredDecisionIds
            .map(requiredId -> new Tuple<>(
                getOutputLabelsForDecisionId.apply(dmnModelInstance, requiredId).collect(Collectors.toList()), 
                getInputHeadersForDecisionId.apply(dmnModelInstance, requiredId).collect(Collectors.toList()))
                )
            .collect(Collectors.toList());
      };
      
  /**
   * Two output columns match a single input column
   * (A, B),(C, D) -> A, (C, D); B, ()
   */
  public static Function<Tuple<List<String>, List<String>>, List<Tuple<String, List<String>>>> mapTupleListToListOfTuple =
      (Tuple<List<String>, List<String>> tuple) -> {
        log.info("Tuple: {}", tuple);
        List<Tuple<String, List<String>>> tupleList = tuple._1.stream().limit(1).map(k -> new Tuple<>(k, tuple._2)).collect(Collectors.toList());
        tupleList.addAll(tuple._1.stream().skip(1).map(k -> new Tuple<>(k, (List<String>) new ArrayList<String>())).collect(Collectors.toList()));
        log.info("tuple list: {}", tupleList);
        return tupleList;
      };

  public static Function<List<Tuple<List<String>, List<String>>>, Map<String, List<String>>> createReplaceMapping = 
      (List<Tuple<List<String>, List<String>>> mappingList) -> {
        return mappingList
            .stream()
            .flatMap(t -> mapTupleListToListOfTuple.apply(t).stream())
            .collect(Collectors.toMap(t -> t._1, t -> t._2));
      };

  public static BiFunction<DmnModelInstance, Decision, Stream<String>> replaceRequiredInputHeaders = 
      (DmnModelInstance dmnModelInstance, Decision decision) -> {
        Stream<String> inputHeaderStream = getInputHeadersForDecisionId.apply(dmnModelInstance, decision.getId());
        
        List<Tuple<List<String>, List<String>>> mappingList = createInputToReplaceList.apply(dmnModelInstance, decision);
        
        Map<String, List<String>> replaceMapping = createReplaceMapping.apply(mappingList);
        
        return inputHeaderStream
            .flatMap(header -> replaceMapping.containsKey(header) ? replaceMapping.get(header).stream() : Stream.of(header));
      };

  public static BiFunction<Tuple<String, List<Map<String, Object>>>, Tuple<Decision, DmnModelInstance>, Stream<String>> checkHeaders = 
      (Tuple<String, List<Map<String, Object>>> sheetData, Tuple<Decision, DmnModelInstance> decisionContext) -> {
        Decision decision = decisionContext._1;
        String sheetName = sheetData._1;

        log.info("Check headers for sheet {} and decision {}", sheetName, decision.getName());
        List<Map<String, Object>> sheetContent = sheetData._2;
        DmnModelInstance dmnModelInstance = decisionContext._2;

        // replace input column of output of required table with input columns of required decision tables
        List<String> inputHeaders = replaceRequiredInputHeaders.apply(dmnModelInstance, decision)
            .collect(Collectors.toList());

        log.info("decision input headers: {}", inputHeaders);
        Stream<String> sheetInputHeaders = sheetContent.get(1).values()
            .stream()
            .map(header -> header.toString())
            .filter(header -> (header.startsWith("Expected: ") == false));
        Stream<String> inputErrorStream = sheetInputHeaders
            .filter(header -> (inputHeaders.contains(header) == false))
            .map(error -> "Column '" + error + "' not found in Dmn Input of table '" + decision.getName() + "'");

        List<String> outputHeaders = getOutputLabelsForDecisionId.apply(dmnModelInstance, decision.getId())
            .collect(Collectors.toList());
        log.info("decision output headers: {}", outputHeaders);
        Stream<String> sheetOutputHeaders = sheetContent.get(1).values()
            .stream()
            .map(header -> header.toString())
            .filter(header -> header.startsWith("Expected: "))
            .map(header -> header.replaceAll("Expected: ", ""));
        Stream<String> outputErrorStream = sheetOutputHeaders
            .filter(header -> (outputHeaders.contains(header) == false))
            .map(error -> "Column '" + error + "' not found in Dmn Output of table '" + decision.getName() + "'");

        List<String> sheetHeaderList = sheetContent.get(1).values()
            .stream()
            .map(header -> header.toString())
            .collect(Collectors.toList());
        log.info("sheet haeders: {}", sheetHeaderList);
        Stream<String> inputExcelErrorStream = inputHeaders
            .stream()
            .filter(header -> (sheetHeaderList.contains(header) == false))
            .map(error -> "Input column '" + error + "' not found in Excel sheet '" + sheetName + "'");

        Stream<String> outputExcelErrorStream = outputHeaders
            .stream()
            .filter(header -> (sheetHeaderList.contains("Expected: " + header) == false))
            .map(error -> "Output column 'Expected: " + error + "' not found in Excel sheet '" + sheetName + "'");

        return Stream.concat(inputErrorStream, Stream.concat(outputErrorStream, 
            Stream.concat(inputExcelErrorStream, outputExcelErrorStream)));
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
                new Tuple<String, List<Map<String,Object>>>(element.getKey(), element.getValue()), 
                new Tuple<Decision, DmnModelInstance>(findDecisionForSheet.apply(dmnModelInstance, element.getKey()), dmnModelInstance)))
            .collect(Collectors.toList()));
        
        return errorResultList;
      };
}
