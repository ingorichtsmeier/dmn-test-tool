package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;

public class ExpectationMapper {
  
  private static final Logger log = LoggerFactory.getLogger(ExpectationMapper.class);

  public Map<String, Object> getExpectationData(Map<String, Object> map) {
    HashMap<String, Object> result = new HashMap<String, Object>();
    for (Iterator<String> iterator = map.keySet().iterator(); iterator.hasNext();) {
      String key = (String) iterator.next();
      if (key.startsWith("Expected:_")) {
        result.put(key.replaceFirst("Expected:_", ""), map.get(key));
      }
    }
    log.info("Expected result: {}", result.toString());
    return result;
  }

  public HashMap<String, Object> getUnexpectedResults(Map<String, Object> expectedResultData, DmnDecisionResult result) {
    log.info("Check for unexpected in expected {}; evaluated {}", expectedResultData.toString(), result.toString());
    HashMap<String, Object> unexpectedResults = new HashMap<String, Object>();
    for (Iterator<String> keys = expectedResultData.keySet().iterator(); keys.hasNext();) {
      String key = (String) keys.next();
      for (Iterator<DmnDecisionResultEntries> iterator = result.iterator(); iterator.hasNext();) {
        DmnDecisionResultEntries dmnDecisionResultEntries = (DmnDecisionResultEntries) iterator.next();
        Object entry = dmnDecisionResultEntries.getEntry(key);
        if (expectedResultData.get(key).equals(entry)) {
          log.info("Key: {} is found in result {}", key, entry);
        } else {
          unexpectedResults.put(key, new EvaluatedResult(expectedResultData.get(key), entry));
        }
      }
      
    }
    return unexpectedResults;
  }

}
