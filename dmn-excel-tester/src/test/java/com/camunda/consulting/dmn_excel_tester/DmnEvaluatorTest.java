package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.logic.DmnEvaluator;
import com.camunda.consulting.dmn_excel_tester.logic.DmnTablePreparer;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

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
    assertThat(expectationResult.get("Dish and Drink").get(2)).containsEntry(
        "Drink", 
        new EvaluatedResult(Arrays.asList("Beer"), Arrays.asList("Grauburgunder")));
    assertThat(expectationResult.get("Dish and Drink").get(3)).containsEntry(
        "Drink", new EvaluatedResult(Arrays.asList("Grauburgunder"), Arrays.asList("Beer")));
    assertThat(expectationResult.get("Dish and Drink").get(4)).contains(
        entry("Dish", new EvaluatedResult(Arrays.asList("Light salad and a nice steak"), Arrays.asList("Roastbeef"))),
        entry("Drink", new EvaluatedResult(Arrays.asList("Prosecco"), Arrays.asList("Red Wine"))));
  }
  
//  TODO: @Test testEvaluateFormulars()
//  public void testEvaluateFormulars() throws Docx4JException, Xlsx4jException {
//    File dmnTableFile = new File("src/test/resources/03-Formulas-and-functions.dmn");
//    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
//    
//    File excelFile = new File("src/test/resources/03-Formulas-and-functionsExpected.xlsx");
//    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
//    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
//    
//    DmnEvaluator dmnEvaluator = new DmnEvaluator();
//    Map<String, List<Map<String, Object>>> expectationResults = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
//    assertThat(expectationResults.get("Dish")).hasSize(1);
//  }

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
  public void testCollect() throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File("src/test/resources/collect/beverages.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    File excelFile = new File("src/test/resources/collect/beveragesExpected.xlsx");
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    DmnEvaluator dmnEvaluator = new DmnEvaluator();
    Map<String, List<Map<String, Object>>> expectations = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
    
    assertThat(expectations.get("Beverages")).hasSize(5);
    assertThat(expectations.get("Beverages").get(2)).isEmpty();
    assertThat(expectations.get("Beverages").get(3)).isEmpty();
    assertThat(expectations.get("Beverages").get(4)).containsOnly(
        entry("Drinks", new EvaluatedResult(Arrays.asList("Gin", "Whiskey"), new ArrayList<>())));
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
    assertThat(expectationResult.get("Determine Employee").get(2)).containsOnly(
        entry("Score", new EvaluatedResult(Arrays.asList("-6"), Arrays.asList("-5"))));
    assertThat(expectationResult.get("Determine Employee").get(3)).isEmpty();
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
    assertThat(expectationResult.get("No matching rule").get(2)).containsOnly(entry("Error", "No rule applied\n"));
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
//  public void testDRDDishOnlyResult() throws Docx4JException, Xlsx4jException {
//    File drdFile = new File("src/test/resources/drd/dinnerDecisions.dmn");
//    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(drdFile);
//    
//    File excelFile = new File("src/test/resources/drd/dinnerDecisionsOnlyDishExpected.xlsx");
//    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
//    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
//    
//    DmnEvaluator dmnEvaluator = new DmnEvaluator();
//    Map<String, List<Map<String,Object>>> expectations = dmnEvaluator.evaluateAllExpectations(dmnModelInstance, dataFromExcel);
//    assertThat(expectations).containsOnlyKeys("Dish");
//    assertThat(expectations.get("Dish")).hasSize(4);
//  }
  
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
