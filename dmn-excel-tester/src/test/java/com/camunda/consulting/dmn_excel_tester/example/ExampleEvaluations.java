package com.camunda.consulting.dmn_excel_tester.example;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.data.MapEntry;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.junit.Test;

public class ExampleEvaluations {
  
  @Test
  public void evaluateSingleEntry() {
    File dmnTableFile = new File("src/test/resources/dish-3.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("dish-decision", dmnModelInstance);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Season", "Summer");
    variables.put("How_many_guests", 5);
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(dmnDecision, variables);
    System.out.println("SingleEntry: " + decisionResult);
    Object singleEntry = decisionResult.getSingleEntry();
    assertThat(singleEntry).isEqualTo("Light Salad and a nice Steak");
  }
  
  @Test
  public void evaluateSingleResult() {
    File dmnTableFile = new File("src/test/resources/dish-and-drink.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("decision", dmnModelInstance);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Season", "Summer");
    variables.put("Number_of_guests", 5);
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(dmnDecision, variables);
    System.out.println("SingleResult: " + decisionResult);
    DmnDecisionResultEntries resultEntries = decisionResult.getSingleResult();
    assertThat(resultEntries).contains(MapEntry.entry("Dish", "Light salad and a nice steak"), MapEntry.entry("Drink", "Prosecco"));
  }
  
  @Test
  public void evaluateCollectEntriesHitPolicyC() {
    File dmnTableFile = new File("src/test/resources/collect/beverages.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("Decision_13nychf", dmnModelInstance);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Dish", "Roastbeef");
    variables.put("Guests_with_Children", true);
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(dmnDecision, variables);
    System.out.println("CollectEntries (C): " + decisionResult);
    List<Object> collectEntries = decisionResult.collectEntries("Drinks");
    assertThat(collectEntries).contains("Bordeaux", "Water", "Apple Juice");
  }
  
  @Test
  public void evaluateResultList() {
    File dmnTableFile = new File("src/test/resources/hitPolicy/versicherung-result-list.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("Mitarbeiterauswahl", dmnModelInstance);  
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("Claim_Type", "Third Party Liability");
    variables.put("Object", "Mobile Phone");
    variables.put("Expenditure", 550);
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(dmnDecision, variables);
    System.out.println("ResultList (C): " + decisionResult);
    List<Object> experienceCategory = decisionResult.collectEntries("Experience_Category");
    assertThat(experienceCategory).contains("Experienced", "Senior");
    Map<String,Object> expectedListEntry = new HashMap<String, Object>();
    expectedListEntry.put("Experience_Category", "Experienced");
    expectedListEntry.put("Product_Know_How", "Third Party Liability");
    expectedListEntry.put("Special_Skills", "Mobile Phone");
    List<Map<String,Object>> resultList = decisionResult.getResultList();
    assertThat(resultList).contains(expectedListEntry);
  }

}
