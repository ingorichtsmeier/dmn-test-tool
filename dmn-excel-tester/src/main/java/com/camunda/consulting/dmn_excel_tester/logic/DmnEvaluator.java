package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnEvaluator {
  
  private static final Logger log = LoggerFactory.getLogger(DmnEvaluator.class);
  
  private List<DmnDecision> decisions;
  private List<Map<String, Object>> testData;
  private DmnEngine dmnEngine;

  private ExpectationMapper expectationMapper;

  public DmnEvaluator(List<DmnDecision> decisions, List<Map<String, Object>> dataFromExcel) {
    this.decisions = decisions;
    this.testData = dataFromExcel;
    this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    this.expectationMapper = new ExpectationMapper();
  }

  /**
   * @return list of unexpected results 
   */
  public List<Map<String, Object>> evaluateAllExpectations() {
    List<Map<String, Object>> unexpectedResultList = new ArrayList<Map<String,Object>>();
    unexpectedResultList.add(new HashMap<String, Object>());
    unexpectedResultList.add(new HashMap<String, Object>());
    for (DmnDecision dmnDecision : decisions) {
      log.info("Evaluating decision {}", dmnDecision.getName());
      for (int i = 2; i < testData.size(); i++) {
        Map<String, Object> decisionData = testData.get(i);
        Map<String, Object> expectedResultData = expectationMapper.getExpectationData(testData.get(i));
        DmnDecisionResult result = dmnEngine.evaluateDecision(dmnDecision, decisionData);
        HashMap<String, Object> unexpectedResult = expectationMapper.getUnexpectedResults(expectedResultData, result);
        log.info("Result: {}", result.getResultList());
        for (DmnDecisionResultEntries resultEntries : result) {
          log.info("ResultEntries {}", resultEntries.toString());
        }
        unexpectedResultList.add(unexpectedResult);
      }
    }
    log.info("\n");
    return unexpectedResultList;
  }

}
