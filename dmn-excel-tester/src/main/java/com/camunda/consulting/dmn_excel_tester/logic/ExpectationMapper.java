package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.model.dmn.BuiltinAggregator;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.functional.Tuple;

public class ExpectationMapper {
  
  private static final Logger log = LoggerFactory.getLogger(ExpectationMapper.class);

  public static Function<Map<String, Object>, Map<String, Object>> getExpectationData = (Map<String, Object> map) -> {
    ConcurrentMap<String, Object> result = new ConcurrentHashMap<String, Object>();

    map.forEach((key, value) -> { 
      if (key.startsWith("Expected:_")) { 
        result.put(key.replaceFirst("Expected:_", ""), map.get(key)); 
      }
    });
    
    log.info("Expected result: {}", result.toString());
    return result;
  };

  /**
   * <pre>
   * forEach(expectedResult.key -> { 
   *   if (hitPolicy == COLLECT) { 
   *     compareResultAgainstExpectations (from list of results) 
   *   } else { 
   *     compareResultAgainstExpectations (of one result) 
   *   } 
   * } 
   * </pre>
   * @param result
   * @param expectedResultData
   * @param hitPolicy
   * @param builtinAggregator
   * @return
   */
  public Map<String, Object> getUnexpectedResults(DmnDecisionResult result, 
      Map<String, Object> expectedResultData, 
      HitPolicy hitPolicy, 
      BuiltinAggregator builtinAggregator) {
    log.info("Check for unexpected in expected {}; evaluated {} with hitpolicy {} and builtin-aggregator {}", expectedResultData.toString(), result.toString(), hitPolicy, builtinAggregator);
    
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
      // If more than one rule match
      if (hitPolicy.equals(HitPolicy.COLLECT) && 
          BuiltinAggregator.SUM.equals(builtinAggregator) == false) {
        List<Object> collectEntries = result.collectEntries(key);
        log.info("Reduced result: {} for key {}", collectEntries, key);
        
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
        
        if (result.isEmpty()) {
          log.info("result is empty: No rule applied");
          unexpectedResults.put("Error", "No rule applied\n");
        } else {
          // iterate over all result entries to match all expectations
          for (Iterator<DmnDecisionResultEntries> iterator = result.iterator(); iterator.hasNext();) {
            DmnDecisionResultEntries dmnDecisionResultEntries = (DmnDecisionResultEntries) iterator.next();
            log.info("iteration of result with {} and key {}", dmnDecisionResultEntries, key);

            // Convert the decision result to String as all cellValues from
            // Excel are Strings
            String entryStr = dmnDecisionResultEntries.getEntry(key).toString();
            if (expectedResultData.get(key).equals(entryStr)) {
              log.info("Expected result for key {}: {} is found in result {}", key, expectedResultData.get(key), entryStr);
            } else {
              log.info("Expected result for key {}: {} is not found in result {}", key, expectedResultData.get(key), entryStr);
              unexpectedResults.put(key, new EvaluatedResult(expectedResultData.get(key), entryStr));
            }
          }
        }
      }
    }
    log.info("Unexpected Results: {}", unexpectedResults);
    return unexpectedResults;
  }
  
//  public static Function<Tuple<DmnDecisionResult, Map<String, Object>>, Map<String, Object>> collectUnexpectedResultsFromSingleHitPolicy = 
//      (Tuple<DmnDecisionResult, Map<String, Object>> input) -> {
//    ConcurrentMap<String, Object> unexpectedResults = new ConcurrentHashMap<String, Object>();
//    input._1.forEach();
//    return unexpectedResults;
//  };

}
