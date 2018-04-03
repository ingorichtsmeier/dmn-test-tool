package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.BuiltinAggregator;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.logic.DmnEvaluator;
import com.camunda.consulting.dmn_excel_tester.logic.DmnTablePreparer;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;
import com.camunda.consulting.dmn_excel_tester.logic.ExpectationMapper;

public class DmnEvaluatorTest {
  
  @Test
  public void testEvaluateDishes() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/dish-3.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/dish-3Expected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectationResult = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectationResult.get("Dish")).hasSize(4);
    assertThat(expectationResult.get("Dish").get(2)).isEmpty();
    assertThat(expectationResult.get("Dish").get(3)).isEmpty();
  }
  
  @Test
  public void testEvaluateDishAndDrink() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/dish-and-drink.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/dish-and-drinkExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectationResult = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectationResult.get("Dish and Drink")).hasSize(4);
    assertThat(expectationResult.get("Dish and Drink").get(2)).isEmpty();
    assertThat(expectationResult.get("Dish and Drink").get(3)).isEmpty();    
  }
  
  @Test
  public void testEvaluateDishAndDrinkFails() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/unexpected/dish-and-drink-fails.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/unexpected/dish-and-drink-failsExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectationResult = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectationResult.get("Dish and Drink")).hasSize(5);
    assertThat(expectationResult.get("Dish and Drink").get(2)).containsEntry("Drink", new EvaluatedResult("Beer", "Grauburgunder"));
    assertThat(expectationResult.get("Dish and Drink").get(3)).containsEntry("Drink", new EvaluatedResult("Grauburgunder", "Beer"));
    assertThat(expectationResult.get("Dish and Drink").get(4)).contains(
        entry("Dish", new EvaluatedResult("Light salad and a nice steak", "Roastbeef")),
        entry("Drink", new EvaluatedResult("Prosecco", "Red Wine")));
  }
  
//  TODO: @Test testEvaluateFormulars()
  public void testEvaluateFormulars() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/03-Formulas-and-functions.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/03-Formulas-and-functionsExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectationResults = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectationResults.get("Dish")).hasSize(1);
  }

  @Test
  public void testExpectationsFromDishInput() {
    Map<String, Object> testData = new HashMap<String, Object>();
    testData.put("Season", "Winter");
    testData.put("Number_of_guests", 3);
    testData.put("Expected:_Dish", "Stew");
    Map<String, Object> expectationData = ExpectationMapper.getExpectationData.apply(testData );
    assertThat(expectationData).containsEntry("Dish", "Stew");
  }

  @Test
  public void testExpectationsFromDishAndDrinkInput() {
    Map<String, Object> testData = new HashMap<String, Object>();
    testData.put("Season", "Winter");
    testData.put("Number_of_guests", 3);
    testData.put("Expected:_Dish", "Stew");
    testData.put("Expected:_Drink", "Beer");
    Map<String, Object> expectationData = ExpectationMapper.getExpectationData.apply(testData );
    assertThat(expectationData).containsOnly(
        entry("Dish", "Stew"), 
        entry("Drink", "Beer"));
  }

  @Test
  public void testUnexpectedResultFromDish() {
    HashMap<String, Object> expectedResults = new HashMap<String, Object>();
    expectedResults.put("Dish", "Steak");
    
    DmnDecisionResult decisionResult = evaluateDish3Dmn();
    Map<String, Object> unexpectedResults = new ExpectationMapper().getUnexpectedResults(decisionResult, expectedResults, HitPolicy.UNIQUE, null);
    assertThat(unexpectedResults).containsEntry("Dish", new EvaluatedResult("Steak", "Stew"));
  }

  @Test
  public void testExpectedResultFromDish() {
    HashMap<String, Object> expectedResults = new HashMap<String, Object>();
    expectedResults.put("Dish", "Stew");
    
    DmnDecisionResult decisionResult = evaluateDish3Dmn();
    Map<String, Object> unexpectedResults = new ExpectationMapper().getUnexpectedResults(decisionResult, expectedResults, HitPolicy.UNIQUE, null);
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
    
    File excelFile = new File("src/test/resources/hitPolicy/determine-employee-1Expected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> decisionResults = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(decisionResults.get("Determine Employee")).hasSize(3);
    assertThat(decisionResults.get("Determine Employee").get(2)).containsKey("error:");
    String errorMessage = (String) decisionResults.get("Determine Employee").get(2).get("error:");
    assertThat(errorMessage).startsWith("DMN-03001 Hit policy 'UNIQUE' only allows a single rule to match.");
  }
  
  @Test
  public void testUnexpectedResultFromBeverages() {
    HashMap<String, Object> expectedResults = new HashMap<String, Object>();
    expectedResults.put("Drinks", Arrays.asList("Budweiser", "Water", "Apple Juice"));
    
    DmnDecisionResult decisionResult = evaluateBeveragesWithChildrenDmn();
    Map<String, Object> unexpectedResults = new ExpectationMapper().getUnexpectedResults(decisionResult, expectedResults, HitPolicy.COLLECT, null);
    assertThat(unexpectedResults).containsEntry("Drinks", new EvaluatedResult(Arrays.asList("Budweiser"), Arrays.asList("Aecht Schlenkerla Rauchbier")));
  }

  private DmnDecisionResult evaluateBeveragesWithChildrenDmn() {
    HashMap<String, Object> decisionInput = new HashMap<String, Object>();
    decisionInput.put("Dish", "Spareribs");
    decisionInput.put("Guests_with_Children", true);
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(Dmn.readModelFromFile(new File("src/test/resources/collect/beverages.dmn")));
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decisions.get(0), decisionInput);
    return decisionResult;
  }
  
  @Test
  public void testCollectWithSingleResult() {
    HashMap<String, Object> expectedResult = new HashMap<String, Object>();
    expectedResult.put("Drinks", "Water");
    
    DmnDecisionResult decisionResult = evaluateBeveragesWithUnknownDishAndNoChildrenDmn();
    Map<String, Object> unexpectedResults = new ExpectationMapper().getUnexpectedResults(decisionResult, expectedResult, HitPolicy.COLLECT, null);
    assertThat(unexpectedResults).isEmpty();
  }

  private DmnDecisionResult evaluateBeveragesWithUnknownDishAndNoChildrenDmn() {
    HashMap<String, Object> decisionInput = new HashMap<String, Object>();
    decisionInput.put("Dish", "Should be unknown");
    decisionInput.put("Guests_with_Children", false);
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(Dmn.readModelFromFile(new File("src/test/resources/collect/beverages.dmn")));
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decisions.get(0), decisionInput);
    return decisionResult;
  }

  @Test
  public void testCollect() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/collect/beverages.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/collect/beveragesExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectations = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    
    assertThat(expectations.get("Beverages")).hasSize(4);
  }
  
  @Test
  public void testUnexpectedResultFromScoring() {
    HashMap<String, Object> expectedResult = new HashMap<String, Object>();
    expectedResult.put("Score", "-6");
    
    DmnDecisionResult decisionResult = evaluateScoringDmn();
    Map<String, Object> unexpectedResults = new ExpectationMapper().getUnexpectedResults(decisionResult, expectedResult, HitPolicy.COLLECT, BuiltinAggregator.SUM);
    assertThat(unexpectedResults).containsEntry("Score", new EvaluatedResult("-6", "-5"));
  }

  private DmnDecisionResult evaluateScoringDmn() {
    HashMap<String, Object> decisionInput = new HashMap<String, Object>();
    decisionInput.put("Number_of_claims", 12);
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(Dmn.readModelFromFile(new File("src/test/resources/collect/scoring-fails.dmn")));
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decisions.get(0), decisionInput);
    return decisionResult;
  }
  
  @Test
  public void testEvaluateScoring() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/collect/scoring-fails.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/collect/scoring-failsExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectationResult = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectationResult.get("Determine Employee")).hasSize(4);
    assertThat(expectationResult.get("Determine Employee").get(2)).containsEntry("Score", new EvaluatedResult("-6", "-5"));
    assertThat(expectationResult.get("Determine Employee").get(3)).isEmpty();
  }
  
  @Test
  public void testNoMatchingRuleResult() {
    HashMap<String, Object> expectedResult = new HashMap<String, Object>();
    expectedResult.put("Output", "would not match");
    
    DmnDecisionResult emptyDecisionResult = evaluateNoMatchingRuleDmn();
    Map<String,Object> unexpectedResults = new ExpectationMapper().getUnexpectedResults(emptyDecisionResult, expectedResult, HitPolicy.UNIQUE, null);
    assertThat(unexpectedResults).containsEntry("Error", "No rule applied\n");    
  }

  private DmnDecisionResult evaluateNoMatchingRuleDmn() {
    HashMap<String, Object> decisionInput = new HashMap<String, Object>();
    decisionInput.put("Input", "a");
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(Dmn.readModelFromFile(new File("src/test/resources/noMatchingRule/noMatchingRule.dmn")));
    DmnDecisionResult emptyDecisionResult = dmnEngine.evaluateDecision(decisions.get(0), decisionInput);
    return emptyDecisionResult;
  }
  
  @Test
  public void testNoMatchingRule() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/noMatchingRule/noMatchingRule.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/noMatchingRule/noMatchingRuleExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectationResult = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectationResult.get("No matching rule")).hasSize(3);
    assertThat(expectationResult.get("No matching rule").get(2)).containsEntry("Error", "No rule applied\n");
  }
  
  @Test
  public void testDRDBeverages() throws Docx4JException, Xlsx4jException {
    File drdFile = new File("src/test/resources/drd/dinnerDecisions.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(drdFile);
    
    File excelFile = new File("src/test/resources/drd/dinnerDecisionsExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String,Object>>> expectations = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectations).containsKeys("Beverages", "Dish");
    assertThat(expectations.get("Dish")).hasSize(4);
    assertThat(expectations.get("Beverages")).hasSize(3);
  }
  
//  TODO: @Test testDRDDishOnlyResult()
  public void testDRDDishOnlyResult() throws Docx4JException, Xlsx4jException {
    File drdFile = new File("src/test/resources/drd/dinnerDecisions.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(drdFile);
    
    File excelFile = new File("src/test/resources/drd/dinnerDecisionsOnlyDishExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String,Object>>> expectations = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    assertThat(expectations).containsOnlyKeys("Dish");
    assertThat(expectations.get("Dish")).hasSize(4);
  }
  
  @Test
  public void testCustomerExample1() throws Docx4JException, Xlsx4jException {
    File dmnFile = new File("src/test/resources/customerExamples/dmn_versicherung_schritt2.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnFile);
    
    File excelFile = new File("src/test/resources/customerExamples/testdaten_versicherung.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectations = dmnEvaluator.evaluateAllExpectations(DmnTablePreparer.prepareTableAndCollectHeaders.apply(dmnModelInstance)._1, dataFromExcel);
    assertThat(expectations).containsKey("Versicherung");
    Map<String, Object> versicherungResult = expectations.get("Versicherung").get(2);
    assertThat(versicherungResult).isEmpty();
  }
}
