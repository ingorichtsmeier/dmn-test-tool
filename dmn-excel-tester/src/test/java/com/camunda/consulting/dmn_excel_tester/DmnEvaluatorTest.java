package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.logic.DmnEvaluator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;
import com.camunda.consulting.dmn_excel_tester.logic.ExpectationMapper;

public class DmnEvaluatorTest {
  
  @Test
  public void testEvaluateDishes() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/dish-3.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    
    File excelFile = new File("src/test/resources/dish-3Expected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
    List<Map<String, Object>> expectationResult = dmnEvaluator.evaluateAllExpectations();
    assertThat(expectationResult).hasSize(4);
    assertThat(expectationResult.get(2)).isEmpty();
    assertThat(expectationResult.get(3)).isEmpty();
  }
  
  @Test
  public void testEvaluateDishAndDrink() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/dish-and-drink.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    
    File excelFile = new File("src/test/resources/dish-and-drinkExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
    List<Map<String, Object>> expectationResult = dmnEvaluator.evaluateAllExpectations();
    assertThat(expectationResult).hasSize(4);
    assertThat(expectationResult.get(2)).isEmpty();
    assertThat(expectationResult.get(3)).isEmpty();    
  }
  
  @Test
  public void testEvaluateDishAndDrinkFails() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/unexpected/dish-and-drink-fails.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    
    File excelFile = new File("src/test/resources/unexpected/dish-and-drink-failsExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
    List<Map<String, Object>> expectationResult = dmnEvaluator.evaluateAllExpectations();
    assertThat(expectationResult).hasSize(5);
    assertThat(expectationResult.get(2)).containsEntry("Drink", new EvaluatedResult("Beer", "Grauburgunder"));
    assertThat(expectationResult.get(3)).containsEntry("Drink", new EvaluatedResult("Grauburgunder", "Beer"));
    assertThat(expectationResult.get(4)).contains(
        entry("Dish", new EvaluatedResult("Light salad and a nice steak", "Roastbeef")),
        entry("Drink", new EvaluatedResult("Prosecco", "Red Wine")));
  }
  
//  TODO: @Test
  public void testEvaluateFormulars() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/03-Formulas-and-functions.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    
    File excelFile = new File("src/test/resources/03-Formulas-and-functionsExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
    List<Map<String, Object>> expectationResults = dmnEvaluator.evaluateAllExpectations();
    assertThat(expectationResults).hasSize(1);
    
  }

  @Test
  public void testExpectationsFromDishInput() {
    ExpectationMapper expectationMapper = new ExpectationMapper();
    Map<String, Object> testData = new HashMap<String, Object>();
    testData.put("Season", "Winter");
    testData.put("Number_of_guests", 3);
    testData.put("Expected:_Dish", "Stew");
    Map<String, Object> expectationData = expectationMapper.getExpectationData(testData );
    assertThat(expectationData).containsEntry("Dish", "Stew");
  }

  @Test
  public void testExpectationsFromDishAndDrinkInput() {
    ExpectationMapper expectationMapper = new ExpectationMapper();
    Map<String, Object> testData = new HashMap<String, Object>();
    testData.put("Season", "Winter");
    testData.put("Number_of_guests", 3);
    testData.put("Expected:_Dish", "Stew");
    testData.put("Expected:_Drink", "Beer");
    Map<String, Object> expectationData = expectationMapper.getExpectationData(testData );
    assertThat(expectationData).containsOnly(
        entry("Dish", "Stew"), 
        entry("Drink", "Beer"));
  }

  @Test
  public void testUnexpectedResultFromDish() {
    HashMap<String, Object> expectedResults = new HashMap<String, Object>();
    expectedResults.put("Dish", "Steak");
    
    DmnDecisionResult decisionResult = evaluateDish3Dmn();
    ExpectationMapper expectationMapper = new ExpectationMapper();
    HashMap<String,Object> unexpectedResults = expectationMapper.getUnexpectedResults(expectedResults, decisionResult);
    assertThat(unexpectedResults).containsEntry("Dish", new EvaluatedResult("Steak", "Stew"));
  }

  @Test
  public void testExpectedResultFromDish() {
    HashMap<String, Object> expectedResults = new HashMap<String, Object>();
    expectedResults.put("Dish", "Stew");
    
    DmnDecisionResult decisionResult = evaluateDish3Dmn();
    ExpectationMapper expectationMapper = new ExpectationMapper();
    HashMap<String,Object> unexpectedResults = expectationMapper.getUnexpectedResults(expectedResults, decisionResult);
    assertThat(unexpectedResults).isEmpty();
  }

  private DmnDecisionResult evaluateDish3Dmn() {
    HashMap<String, Object> decisionInput = new HashMap<String, Object>();
    decisionInput.put("Season", "Winter");
    decisionInput.put("How_many_guests", 9);
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(Dmn.readModelFromFile(new File("src/test/resources/dish-3.dmn")));
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decisions.get(0), decisionInput);
    return decisionResult;
  }
  
  @Test
  public void testHitPolicyError() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/hitPolicy/determine-employee-1.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    
    File excelFile = new File("src/test/resources/hitPolicy/determine-employee-1Expected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
    List<Map<String, Object>> decisionResults = dmnEvaluator.evaluateAllExpectations();
    assertThat(decisionResults).hasSize(3);
    assertThat(decisionResults.get(2)).containsKey("error:");
    String errorMessage = (String) decisionResults.get(2).get("error:");
    assertThat(errorMessage).startsWith("DMN-03001 Hit policy 'UNIQUE' only allows a single rule to match.");
  }

}
