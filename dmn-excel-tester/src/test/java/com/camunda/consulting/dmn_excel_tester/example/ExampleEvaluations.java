package com.camunda.consulting.dmn_excel_tester.example;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.assertj.core.data.MapEntry;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionResultException;
import org.camunda.bpm.dmn.engine.impl.DmnEvaluationException;
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
    
    assertThat(checkForSingleEntry.apply(decisionResult)).isTrue();
    
    assertThat(decisionResult.getSingleResult()).containsEntry("Dish", "Light Salad and a nice Steak");
    assertThat(checkForSingleResult.apply(decisionResult)).isTrue();
    
    List<Object> dishEntries = decisionResult.collectEntries("Dish");
    assertThat(dishEntries).containsExactly("Light Salad and a nice Steak");
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
    assertThat(resultEntries).contains(
        MapEntry.entry("Dish", "Light salad and a nice steak"), 
        MapEntry.entry("Drink", "Prosecco"));
    
    assertThat(checkForSingleResult.apply(decisionResult)).isTrue();
    
    assertThat(checkForSingleEntry.apply(decisionResult)).isFalse();
    
    List<Object> dishEntries = decisionResult.collectEntries("Dish");
    assertThat(dishEntries).containsExactly("Light salad and a nice steak");
    List<Object> drinkEntries = decisionResult.collectEntries("Drink");
    assertThat(drinkEntries).containsExactly("Prosecco");
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
    assertThat(collectEntries).containsOnly("Bordeaux", "Water", "Apple Juice");
    
    assertThat(checkForSingleEntry.apply(decisionResult)).isFalse();
    assertThat(checkForSingleResult.apply(decisionResult)).isFalse();
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
    assertThat(experienceCategory).containsExactly("Experienced", "Senior");
    List<Object> knowHowList = decisionResult.collectEntries("Product_Know_How");
    assertThat(knowHowList).containsExactly("Third Party Liability", "Third Party Liability");
    List<Object> skillList = decisionResult.collectEntries("Special_Skills");
    assertThat(skillList).containsExactly("Mobile Phone");
    
    Map<String,Object> firstExpectedListEntry = new HashMap<String, Object>();
    firstExpectedListEntry.put("Experience_Category", "Experienced");
    firstExpectedListEntry.put("Product_Know_How", "Third Party Liability");
    firstExpectedListEntry.put("Special_Skills", "Mobile Phone");
    HashMap<String, Object> secondExpectedEntry = new HashMap<String, Object>();
    secondExpectedEntry.put("Experience_Category", "Senior");
    secondExpectedEntry.put("Product_Know_How", "Third Party Liability");
    List<Map<String,Object>> resultList = decisionResult.getResultList();
    assertThat(resultList).contains(firstExpectedListEntry, secondExpectedEntry);
    
    assertThat(checkForSingleEntry.apply(decisionResult)).isFalse();
    assertThat(checkForSingleResult.apply(decisionResult)).isFalse();
  }
  
  @Test
  public void evaluateDinnerWithSpecialGuests() {
    File dmnTableFile = new File("src/test/resources/drd/dinner-with-special-guests.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("beverages_decision", dmnModelInstance);
    Map<String,Object> variables = new HashMap<String, Object>();
    variables.put("season", "Winter");
    variables.put("guest_name", "Frank");
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(dmnDecision, variables);
    System.out.println("CollectEntries (C): " + decisionResult);
    
    List<Object> beverages = decisionResult.collectEntries("beverages");
    assertThat(beverages).containsOnly("Cola", "Champagner");
  }
  
  public static Function<DmnDecisionResult, Boolean> checkForSingleEntry = (DmnDecisionResult decisionResult) -> {
    boolean hasSingleEntry;
    try {
      decisionResult.getSingleEntry();
      hasSingleEntry = true;
    } catch (DmnDecisionResultException e) {
      hasSingleEntry = false;
    }
    return hasSingleEntry;
  };
  
  public static Function<DmnDecisionResult, Boolean> checkForSingleResult = (DmnDecisionResult decisionResult) -> {
    boolean hasSingleResult;
    try {
      decisionResult.getSingleResult();
      hasSingleResult = true;
    } catch (DmnDecisionResultException e) {
      hasSingleResult = false;
    }
    return hasSingleResult;
  };
  
  @Test
  public void testEvaluateModeler12() {
    File dmnTableFile = new File("src/test/resources/exampleEvaluation/modeler12_1.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecision dmnDecision = dmnEngine.parseDecision("Decision_1", dmnModelInstance);
    
    Map<String,Object> variables = new HashMap<String, Object>();
    variables.put("guest with children", "hallo");
    
    DmnDecisionResult decisionResult;
    try {
      decisionResult = dmnEngine.evaluateDecision(dmnDecision, variables);
    } catch (DmnEvaluationException e) {
      assertThat(e.getLocalizedMessage()).startsWith("DMN-01002 Unable to evaluate expression for language 'juel': '${guests with children?}'");
    }
    //assertThat(decisionResult.collectEntries("label")).containsOnly("f4");
  }

}
