package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.assertj.core.data.MapEntry;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Input;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.dmn_excel_tester.functional.Tuple;
import com.camunda.consulting.dmn_excel_tester.logic.DmnTablePreparer;

public class DmnTablePrepareTest {
  
  private final Logger log = LoggerFactory.getLogger(DmnTablePrepareTest.class);
  
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
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer();
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable(modelInstance)._1;
    
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
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer();
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable(modelInstance)._1;
    
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
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer();
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable(modelInstance)._1;
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(preparedModelInstance);
    Map<String, Object> inputVariables = new HashMap<String, Object>();
    inputVariables.put("Claim_region_identical_", true);
    DmnDecisionResult result = dmnEngine.evaluateDecision(decisions.get(0), inputVariables);
    
    assertThat(result.getFirstResult()).containsEntry("Score", 0.9);
  }
  
  @Test
  public void testInputWithSpecialChars() {
    File decisionTableFile = new File("src/test/resources/dmnPreparation/headers-with-special-chars.dmn");
    DmnModelInstance modelInstance = Dmn.readModelFromFile(decisionTableFile);
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer();
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable(modelInstance)._1;
    
    log.info("prepared table: {}", Dmn.convertToString(preparedModelInstance));
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(preparedModelInstance);
    Map<String, Object> inputVariables = new HashMap<String, Object>();
    inputVariables.put("High_Load___1M_Workflow_Instances___Day_", true);
    inputVariables.put("Only__Basic_Workflow_Execution__required_", true);
    DmnDecisionResult result = dmnEngine.evaluateDecision(decisions.get(0), inputVariables);
    
    assertThat(result.getFirstResult()).containsEntry("Proposed_Camunda_product", "ZeeBe");
  }
  
  @Test
  public void testDRDBusinessView() {
    File drdFile = new File("src/test/resources/dmnPreparation/dinnerDecisionsBusinessView.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(drdFile);
    
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer();
    DmnModelInstance preparedModelInstance = dmnTablePreparer.prepareTable(dmnModelInstance)._1;
    
    log.info("prepared table: {}", Dmn.convertToString(preparedModelInstance));
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    DmnDecisionRequirementsGraph decisionRequirementsGraph = dmnEngine.parseDecisionRequirementsGraph(preparedModelInstance);
    Collection<DmnDecision> decisions = decisionRequirementsGraph.getDecisions();
    DmnDecision testDecision = null;
    for (DmnDecision dmnDecision : decisions) {
      if (dmnDecision.getName().equals("Beverages")) {
        testDecision = dmnDecision;
      }
    }
    
    HashMap<String, Object> variables = new HashMap<String, Object>();
    variables.put("Season", "Spring");
    variables.put("Number_of_Guests", 5);
    variables.put("Guests_with_children", true);
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(testDecision, variables);
    
    assertThat(decisionResult.getFirstResult()).containsKey("Beverages");
  }
  
  @Test
  public void testKeepInputExpression() {
    assertThat(DmnTablePreparer.calculateHeader.apply("My Header", "inputExpression"))
      .isEqualTo("inputExpression");
  }
  
  @Test
  public void testMapInputName() {
    assertThat(DmnTablePreparer.calculateHeader.apply("My Header?", ""))
      .isEqualTo("My_Header_");
  }
  
  @Test
  public void testMapOutputName() {
    assertThat(DmnTablePreparer.calculateHeader.apply("Expected: Product Know-How", ""))
      .isEqualTo("Expected:_Product_Know_How");
  }
  
  @Test
  public void testTransformInputHeaders() {
    File decisionTableFile = new File("src/test/resources/dmnPreparation/headers-with-special-chars.dmn");
    DmnModelInstance modelInstance = Dmn.readModelFromFile(decisionTableFile);
    
    Collection<Input> inputs = modelInstance.getModelElementsByType(Input.class);
    inputs.forEach(DmnTablePreparer.transformInputHeaders);
    Input[] inputArray = (Input[]) inputs.toArray(new Input[2]);
    
    assertThat(inputArray[0].getInputExpression().getTextContent()).isEqualTo("High_Load___1M_Workflow_Instances___Day_");
    assertThat(inputArray[1].getInputExpression().getTextContent()).isEqualTo("basicWorkflow");
  }
  
  @Test
  public void testgetDmnTableHeaders() {
    File decisionTableFile = new File("src/test/resources/dmnPreparation/headers-with-special-chars.dmn");
    DmnModelInstance modelInstance = Dmn.readModelFromFile(decisionTableFile);
    Tuple<DmnModelInstance, ConcurrentMap<String, String>> preparation = DmnTablePreparer.getDmnTableHeaders.apply(modelInstance);
    DmnModelInstance preparedModelInstance = preparation._1;
    
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(preparedModelInstance);
    Map<String, Object> inputVariables = new HashMap<String, Object>();
    inputVariables.put("High_Load___1M_Workflow_Instances___Day_", true);
    inputVariables.put("basicWorkflow", true);
    DmnDecisionResult result = dmnEngine.evaluateDecision(decisions.get(0), inputVariables);
    
    assertThat(result.getFirstResult()).containsEntry("Proposed_Camunda_product", "ZeeBe");
    
    assertThat(preparation._2).contains(MapEntry.entry("High Load (>1M Workflow Instances / Day)", "High_Load___1M_Workflow_Instances___Day_"), 
        MapEntry.entry("Only \"Basic Workflow Execution\" required?", "basicWorkflow"),
        MapEntry.entry("Proposed Camunda product", "Proposed_Camunda_product"));
  }
  
}
