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
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnEvaluator {
  
  private static final Logger log = LoggerFactory.getLogger(DmnEvaluator.class);
  
  private DmnModelInstance decisionModel;
  private List<Map<String, Object>> testData;
  private DmnEngine dmnEngine;

  private ExpectationMapper expectationMapper;

  public DmnEvaluator(DmnModelInstance decisionModel, List<Map<String, Object>> dataFromExcel) {
    this.decisionModel = decisionModel;
    this.testData = dataFromExcel;
    this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    this.expectationMapper = new ExpectationMapper();
  }

  /**
   * @return list of unexpected results 
   */
  public List<Map<String, Object>> evaluateAllExpectations() {
    List<DmnDecision> decisions = dmnEngine.parseDecisions(decisionModel);
    List<Map<String, Object>> unexpectedResultList = new ArrayList<Map<String,Object>>();
    unexpectedResultList.add(new HashMap<String, Object>());
    unexpectedResultList.add(new HashMap<String, Object>());
    
    // iterate over all decisions
    for (DmnDecision dmnDecision : decisions) {
      log.info("Evaluating decision {}", dmnDecision.getName());
      HitPolicy hitPolicy = null;
      DmnDecisionLogic decisionLogic = dmnDecision.getDecisionLogic();
      if (decisionLogic instanceof DmnDecisionTableImpl) {
        DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionLogic;
        hitPolicy = decisionTable.getHitPolicyHandler().getHitPolicyEntry().getHitPolicy();
      };
      log.info("HitPolicy: {}", hitPolicy);
      for (int i = 2; i < testData.size(); i++) {
        Map<String, Object> decisionData = testData.get(i);
        Map<String, Object> expectedResultData = expectationMapper.getExpectationData(testData.get(i));
        try {
          DmnDecisionResult result = dmnEngine.evaluateDecision(dmnDecision, decisionData);
          log.info("Result: {}", result);
          HashMap<String, Object> unexpectedResult = expectationMapper.getUnexpectedResults(expectedResultData, result, hitPolicy);
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
    }
    log.info("");
    return unexpectedResultList;
  }

}
