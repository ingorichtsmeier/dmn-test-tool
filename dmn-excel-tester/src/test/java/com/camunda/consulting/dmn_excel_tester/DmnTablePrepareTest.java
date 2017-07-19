package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Text;
import org.junit.Test;

import com.camunda.consulting.dmn_excel_tester.logic.DmnTablePreparer;

public class DmnTablePrepareTest {
  
  @Test
  public void testDishTablePreparationEmpty() {
    File decisionTableFile = new File("src/test/resources/dmnPreparation/dish-empty.dmn");   
    DmnModelInstance modelInstance = Dmn.readModelFromFile(decisionTableFile);
    
    //    List<Map<String, Object>> headers = new ArrayList<Map<String,Object>>();
    //    Map<String, Object> columnA = new HashMap<String, Object>();
    //    columnA.put("A", "Season");
    //    headers.add(columnA);
    //    Map<String, Object> columnB = new HashMap<String, Object>();
    //    columnB.put("B", "Number of guests");
    //    headers.add(columnB);
    //    Map<String, Object> columnC = new HashMap<String, Object>();
    //    columnC.put("C", "Expected: Dish");
    //    headers.add(columnC);
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer(modelInstance/*, headers*/);
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable();
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(preparedModelInstance);
    Map<String, Object> inputVariables = new HashMap<String, Object>();
    inputVariables.put("Season", "Winter");
    inputVariables.put("Number_of_guests", 5);
    DmnDecisionResult result = dmnEngine.evaluateDecision(decisions.get(0), inputVariables);
    
    assertThat(result.getFirstResult()).containsEntry("Dish", "Roastbeef");
  }
  
  @Test
  public void testDishTablePreparationTechnical() {
    File decisionTableFile = new File("src/test/resources/dmnPreparation/dish-technical.dmn");   
    DmnModelInstance modelInstance = Dmn.readModelFromFile(decisionTableFile);
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer(modelInstance);
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable();
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(preparedModelInstance);
    Map<String, Object> inputVariables = new HashMap<String, Object>();
    inputVariables.put("Season", "Summer");
    inputVariables.put("Number_of_guests", 6);
    DmnDecisionResult result = dmnEngine.evaluateDecision(decisions.get(0), inputVariables);
    
    assertThat(result.getFirstResult()).containsEntry("Dish", "Light salad and a nice steak");
  }
  
  @Test
  public void testBooleanInputWithQuestionMark() {
    File decisionTableFile = new File("src/test/resources/dmnPreparation/boolean-input.dmn");
    DmnModelInstance modelInstance = Dmn.readModelFromFile(decisionTableFile);
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer(modelInstance);
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable();
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(preparedModelInstance);
    Map<String, Object> inputVariables = new HashMap<String, Object>();
    inputVariables.put("Claim_region_identical_", true);
    DmnDecisionResult result = dmnEngine.evaluateDecision(decisions.get(0), inputVariables);
    
    assertThat(result.getFirstResult()).containsEntry("Score", 0.9);
    
  }
  
}
