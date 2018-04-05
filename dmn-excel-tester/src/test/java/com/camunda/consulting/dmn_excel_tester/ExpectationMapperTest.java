package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.data.MapEntry;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.junit.Test;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.functional.Tuple;
import com.camunda.consulting.dmn_excel_tester.logic.ExpectationMapper;

public class ExpectationMapperTest {
  @Test
  public void testExpectationsFromDishInput() {
    Map<String, Object> testData = new HashMap<String, Object>();
    testData.put("Season", "Winter");
    testData.put("Number_of_guests", 3);
    testData.put("Expected:_Dish", "Stew");
    Map<String, Object> expectationData = ExpectationMapper.getExpectationData.apply(testData );
    assertThat(expectationData).containsEntry("Dish", Arrays.asList("Stew"));
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
        entry("Dish", Arrays.asList("Stew")), 
        entry("Drink", Arrays.asList("Beer")));
  }
  
  @Test
  public void testExpectationsFromList() {
    HashMap<String, Object> testData = new HashMap<String, Object>();
    testData.put("Dish", "Roastbeef");
    testData.put("Expected:_Drinks", Arrays.asList("Guiness", "Water", "Apple Juice"));
    Map<String, Object> expectationData = ExpectationMapper.getExpectationData.apply(testData);
    assertThat(expectationData).containsOnly(entry("Drinks", Arrays.asList("Guiness", "Water", "Apple Juice")));
  }

  @Test
  public void testExpectedResultFromDish() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Dish";
    expectedResults.put(outputName, Arrays.asList("Stew"));
    DmnDecisionResult decisionResult = evaluateDish3Dmn();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper
        .getUnexpectedResultsForOutputColumn.apply(
            decisionResult, 
            new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).isEmpty();
  }
  
  @Test
  public void testUnexpectedResultFromDish() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    expectedResults.put("Dish", Arrays.asList("Steak"));
    DmnDecisionResult decisionResult = evaluateDish3Dmn();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper
        .getUnexpectedResultsForOutputColumn.apply(
            decisionResult, 
            new Tuple<String, List<Object>>("Dish", (List<Object>) expectedResults.get("Dish")));
    assertThat(unexpectedResults).containsEntry("Dish", new EvaluatedResult(Arrays.asList("Steak"), Arrays.asList("Stew")));
  }
  
  DmnDecisionResult evaluateDish3Dmn() {
    HashMap<String, Object> decisionInput = new HashMap<String, Object>();
    decisionInput.put("Season", "Winter");
    decisionInput.put("How_many_guests", 9);
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(Dmn.readModelFromFile(new File("src/test/resources/dish-3.dmn")));
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decisions.get(0), decisionInput);
    return decisionResult;
  }
  
  @Test
  public void testExpectedResultFromList() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Drinks";
    expectedResults.put(outputName, Arrays.asList("Bordeaux", "Water", "Apple Juice"));
    DmnDecisionResult decisionResult = evaluateBeverages();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).isEmpty();
  }
  
  /*
   * Unexpected Results:
   * expected: ("Bordeaux", "Pinot Noir", "Apple Juice")
   * result:   ("Bordeaux", "Water", "Apple Juice")
   * output: (expected: "Pinot Noir", result: "Water")
   */
  @Test
  public void testUnexpectedResultFromList() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Drinks";
    expectedResults.put(outputName, Arrays.asList("Bordeaux", "Pinot Noir", "Apple Juice"));
    DmnDecisionResult decisionResult = evaluateBeverages();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).containsOnly(
        MapEntry.entry(outputName, new EvaluatedResult(Arrays.asList("Pinot Noir"), Arrays.asList("Water"))));
  }
  
  /* 
   * expected: ("Apple Juice", "Pinot Noir", "Bordeaux")
   * result:   ("Bordeaux", "Water", "Apple Juice")
   * output: (expected: "Pinot Noir", result: "Water")
   */
  @Test
  public void testUnexpectedResultFromListSequenceChanged() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Drinks";
    expectedResults.put(outputName, Arrays.asList("Apple Juice", "Pinot Noir", "Bordeaux"));
    DmnDecisionResult decisionResult = evaluateBeverages();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).containsOnly(
        MapEntry.entry(outputName, new EvaluatedResult(Arrays.asList("Pinot Noir"), Arrays.asList("Water"))));
  }
  
  /* 
   * expected: ("Guiness", "Pinot Noir", "Apple Juice")
   * result: ("Bordeaux", "Water", "Apple Juice")
   * output: (expected: "Guiness", "Pinot Noir", result: "Bordeaux", "Water")
   */
  @Test
  public void testUnexpectedResultFromListManyErrors() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Drinks";
    expectedResults.put(outputName, Arrays.asList("Guiness", "Pinot Noir", "Apple Juice"));
    DmnDecisionResult decisionResult = evaluateBeverages();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).containsOnly(
        MapEntry.entry(outputName, 
            new EvaluatedResult(Arrays.asList("Guiness", "Pinot Noir"), Arrays.asList("Bordeaux", "Water"))));
  }
  
  /* 
   * expected: ("Guiness", "Pinot Noir", "Bordeaux", "Water", "Apple Juice")
   * result: ("Bordeaux", "Water", "Apple Juice")
   * output: (expected: "Guiness", "Pinot Noir", result:)
   */ 
  @Test
  public void testUnexpectedResultFromListMoreExpectations() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Drinks";
    expectedResults.put(outputName, Arrays.asList("Guiness", "Pinot Noir", "Bordeaux", "Water", "Apple Juice"));
    DmnDecisionResult decisionResult = evaluateBeverages();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).containsOnly(
        MapEntry.entry(outputName, 
            new EvaluatedResult(Arrays.asList("Guiness", "Pinot Noir"), new ArrayList<>())));
  }

  /* expected: ("Bordeaux", "Water")
   * result: ("Bordeaux", "Water", "Apple Juice")
   * output: (expected:, result: "Apple Juice")
   */
  @Test
  public void testUnexpectedResultFromListMoreResults() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Drinks";
    expectedResults.put(outputName, Arrays.asList("Bordeaux", "Water"));
    DmnDecisionResult decisionResult = evaluateBeverages();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).containsOnly(
        MapEntry.entry(outputName, 
            new EvaluatedResult(new ArrayList<>(), Arrays.asList("Apple Juice"))));
  }
  
  private DmnDecisionResult evaluateBeverages() {
    File dmnTableFile = new File("src/test/resources/collect/beverages.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("Decision_13nychf", dmnModelInstance);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Dish", "Roastbeef");
    variables.put("Guests_with_Children", true);
    return dmnEngine.evaluateDecision(dmnDecision, variables);
  }
  
  @Test
  public void testUnexpectedResultfromTwoOutputColumns() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputDrinks = "Drink";
    expectedResults.put(outputDrinks, Arrays.asList("Bordeaux", "Water"));
    String outputDish = "Dish";
    expectedResults.put(outputDish, Arrays.asList("Roastbeef"));
    DmnDecisionResult decisionResult = evaluateDishAndDrinkFails();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedDrinks = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputDrinks, (List<Object>) expectedResults.get(outputDrinks)));
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedDish = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, 
        new Tuple<String, List<Object>>(outputDish, (List<Object>) expectedResults.get(outputDish)));
    
    Map<String,Object> unexpectedResults = new HashMap<>();
    unexpectedResults.putAll(unexpectedDish);
    unexpectedResults.putAll(unexpectedDrinks);
    
    assertThat(unexpectedResults).containsOnly(
        entry(outputDrinks, new EvaluatedResult(Arrays.asList("Bordeaux", "Water"), Arrays.asList("Grauburgunder"))),
        entry(outputDish, new EvaluatedResult(Arrays.asList("Roastbeef"), Arrays.asList("Stew"))));
  }
  
  private DmnDecisionResult evaluateDishAndDrinkFails() {
    File dmnTableFile = new File("src/test/resources/unexpected/dish-and-drink-fails.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);

    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("decision", dmnModelInstance);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Season", "Winter");
    variables.put("Number_of_guests", 9);
    return dmnEngine.evaluateDecision(dmnDecision, variables);    
  }
  
  @Test
  public void testExpectedWithTypeConversion() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Score";
    expectedResults.put(outputName, Arrays.asList("-5"));
    DmnDecisionResult decisionResult = evaluateScoring();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).isEmpty();
  }
  
  @Test
  public void testUnexpectedWithTypeConversion() {
    Map<String, Object> expectedResults = new HashMap<String, Object>();
    String outputName = "Score";
    expectedResults.put(outputName, Arrays.asList("-6"));
    DmnDecisionResult decisionResult = evaluateScoring();
    
    @SuppressWarnings("unchecked")
    Map<String, Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        decisionResult, new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResults.get(outputName)));
    assertThat(unexpectedResults).containsOnly(
        entry(outputName, new EvaluatedResult(Arrays.asList("-6"), Arrays.asList("-5"))));
    
  }
  
  private DmnDecisionResult evaluateScoring() {
    File dmnTableFile = new File("src/test/resources/collect/scoring-fails.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);

    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("decision", dmnModelInstance);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Number_of_claims", 11);
    return dmnEngine.evaluateDecision(dmnDecision, variables);    
  }
  
  @Test
  public void testNoMatchingRuleResult() {
    HashMap<String, Object> expectedResult = new HashMap<String, Object>();
    String outputName = "Output";
    expectedResult.put(outputName, Arrays.asList("would not match"));
    DmnDecisionResult emptyDecisionResult = evaluateNoMatchingRuleDmn();
    
    @SuppressWarnings("unchecked")
    Map<String,Object> unexpectedResults = ExpectationMapper.getUnexpectedResultsForOutputColumn.apply(
        emptyDecisionResult, new Tuple<String, List<Object>>(outputName, (List<Object>) expectedResult.get(outputName))); 
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

}
