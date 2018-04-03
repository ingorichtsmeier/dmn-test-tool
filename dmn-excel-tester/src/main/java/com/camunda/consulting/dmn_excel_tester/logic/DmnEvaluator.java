package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionLogic;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.dmn.engine.impl.hitpolicy.DmnHitPolicyException;
import org.camunda.bpm.model.dmn.BuiltinAggregator;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnEvaluator {
  
  private static final Logger log = LoggerFactory.getLogger(DmnEvaluator.class);
  
  private DmnEngine dmnEngine;

  private ExpectationMapper expectationMapper;

  public DmnEvaluator() {
    this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    this.expectationMapper = new ExpectationMapper();
  }

  /**
   * @return list of unexpected results 
   */
  public Map<String, List<Map<String, Object>>> evaluateAllExpectations(DmnModelInstance decisionModel, Map<String, List<Map<String, Object>>> testData) {
    List<DmnDecision> decisions = dmnEngine.parseDecisions(decisionModel);
    HashMap<String, List<Map<String, Object>>> unexpectedResultMap = new HashMap<String, List<Map<String, Object>>>();
    
    // iterate over all tables to test
    // or 
    // (no sheets used in excel file) test what?
    
    // TODO: iterate over keys of testData: Sheets in the Excel file, Sheet name maps to dmnDecision
        
    // iterate over all decisions
    for (DmnDecision dmnDecision : decisions) {
      List<Map<String, Object>> unexpectedResultList = new ArrayList<Map<String,Object>>();
      // index 0: not used in Excel
      unexpectedResultList.add(new HashMap<String, Object>());
      // index 1: Header in Excel
      unexpectedResultList.add(new HashMap<String, Object>());
      
      log.info("Evaluating decision {}", dmnDecision.getName());
      HitPolicy hitPolicy = null;
      BuiltinAggregator builtinAggregator = null;
      DmnDecisionLogic decisionLogic = dmnDecision.getDecisionLogic();
      if (decisionLogic instanceof DmnDecisionTableImpl) {
        DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionLogic;
        hitPolicy = decisionTable.getHitPolicyHandler().getHitPolicyEntry().getHitPolicy();
        builtinAggregator = decisionTable.getHitPolicyHandler().getHitPolicyEntry().getAggregator();
      };
      log.info("HitPolicy: {}", hitPolicy);
      List<Map<String, Object>> sheetData = testData.get(dmnDecision.getName());
      // TODO: Only one sheet with default name, mostly 'Tabelle1'
      if (sheetData == null) {
        sheetData = testData.entrySet().iterator().next().getValue();
      }
      // iterate over lines in the sheet
      for (int i = 2; i < sheetData.size(); i++) {
        Map<String, Object> decisionData = sheetData.get(i);
        Map<String, Object> expectedResultData = expectationMapper.getExpectationData(sheetData.get(i));
        try {
          DmnDecisionResult result = dmnEngine.evaluateDecision(dmnDecision, decisionData);
          log.info("Result: {}", result);
          Map<String, Object> unexpectedResult = expectationMapper.getUnexpectedResults(expectedResultData, result, hitPolicy, builtinAggregator);
          for (DmnDecisionResultEntries resultEntries : result) {
            log.info("ResultEntries {}", resultEntries.toString());
          }
          unexpectedResultList.add(unexpectedResult);
        } catch (DmnHitPolicyException e) {
          HashMap<String, Object> errorMap = new HashMap<String, Object>();
          String errorMessage = e.getLocalizedMessage();
          errorMessage = errorMessage.replaceAll("\\.", ".\n");
          errorMessage = errorMessage.replaceAll("DmnEvaluatedDecisionRule", "\nDmEvaluatedDecisionRule");
          errorMap.put("error:", errorMessage);
          unexpectedResultList.add(errorMap);
        }
      }
      unexpectedResultMap.put(dmnDecision.getName(), unexpectedResultList);
    }
    log.info("");
    return unexpectedResultMap;
  }

}
