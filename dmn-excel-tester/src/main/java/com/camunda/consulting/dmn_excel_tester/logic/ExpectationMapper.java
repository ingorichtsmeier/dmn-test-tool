package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.model.dmn.HitPolicy;
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

  public HashMap<String, Object> getUnexpectedResults(Map<String, Object> expectedResultData, DmnDecisionResult result, HitPolicy hitPolicy) {
    log.info("Check for unexpected in expected {}; evaluated {} with hitpolicy {}", expectedResultData.toString(), result.toString(), hitPolicy);
    
    // result is a list of map with results per rule  
    // most generic result: (hit policy collect with three rules and two outputs per rule)
    // [
    //  {Drinks=Value 'Aecht Schlenkerla Rauchbier' of type 'PrimitiveValueType[string]', number=Value '1' of type 'PrimitiveValueType[integer]'}, 
    //  {Drinks=Value 'Apple Juice' of type 'PrimitiveValueType[string]', number=Value '5' of type 'PrimitiveValueType[integer]'}, 
    //  {Drinks=Value 'Water' of type 'PrimitiveValueType[string]', number=Value '6' of type 'PrimitiveValueType[integer]'}
    // ]
    
    HashMap<String, Object> unexpectedResults = new HashMap<String, Object>();
    // Iterate over all expectations (columns)
    for (Iterator<String> keys = expectedResultData.keySet().iterator(); keys.hasNext();) {
      String key = (String) keys.next();
      log.info("Handle list compare for {}", expectedResultData.get(key));
      
      // If more than one rule match
      if (hitPolicy.equals(HitPolicy.COLLECT)) {
        List<Object> collectEntries = result.collectEntries(key);
        log.info("Reduced result: {}", collectEntries);
        
        // Maybe the expectedResultData contains only a string
        Collection<Object> expectedList = null;
        if (expectedResultData.get(key) instanceof List<?>) {
          // Expect a list
          expectedList = new ArrayList<>((Collection<Object>) expectedResultData.get(key));
        } else {
          // Expect a single entry
          expectedList = new ArrayList<>();
          expectedList.add(expectedResultData.get(key));
        }
        
        // Compare expectation and result
        if (expectedList.equals(collectEntries)) {
          log.info("Expected result for key {}: {} is found in result {}", key, expectedResultData.get(key), collectEntries);
        } else {
          // compare the lists
          ArrayList<Object> copyOfResults = new ArrayList<>(collectEntries);
          collectEntries.removeAll(expectedList);
          expectedList.removeAll(copyOfResults);
          log.info("Expected result for key {}: {} is not found in result {}", key, expectedList, collectEntries);
          
          unexpectedResults.put(key, new EvaluatedResult(expectedList, collectEntries));
        }
      } else {
        // hitpolicy single result
        
        // iterate over all result entries to match all expectations
        for (Iterator<DmnDecisionResultEntries> iterator = result.iterator(); iterator.hasNext();) {
          DmnDecisionResultEntries dmnDecisionResultEntries = (DmnDecisionResultEntries) iterator.next();
          log.info("iteration of result with {} and key {}", dmnDecisionResultEntries, key);
          Object entry = dmnDecisionResultEntries.getEntry(key);
          if (expectedResultData.get(key).equals(entry)) {
            log.info("Expected result for key {}: {} is found in result {}", key, expectedResultData.get(key), entry);
          } else {
            log.info("Expected result for key {}: {} is not found in result {}", key, expectedResultData.get(key), entry);
            unexpectedResults.put(key, new EvaluatedResult(expectedResultData.get(key), entry));
          }
        }
      }
    }
    log.info("Unexpected Results: {}", unexpectedResults);
    return unexpectedResults;
  }

}
