package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.functional.Tuple;

public class ExpectationMapper {
  
  private static final Logger log = LoggerFactory.getLogger(ExpectationMapper.class);

  public static Function<Map<String, Object>, Map<String, Object>> getExpectationData = (Map<String, Object> map) -> {
    Map<String, Object> result = map
        .entrySet()
        .stream()
        .filter(value -> value.getKey().startsWith("Expected:_"))
        .collect(Collectors.toMap(
            value -> value.getKey().replaceFirst("Expected:_", ""), 
            value -> (value.getValue() instanceof List) ? value.getValue() : Arrays.asList(value.getValue())));
    
    log.info("Expected result: {}", result.toString());
    return result;
  };
  
  public static BiFunction<DmnDecisionResult, Tuple<String, List<Object>>, Map<String, Object>> getUnexpectedResultsForOutputColumn = 
      (DmnDecisionResult decisionResult, Tuple<String, List<Object>> expectations) -> {
        Map<String,Object> unexpected = new ConcurrentHashMap<String, Object>();
        String outputName = expectations._1;
        List<Object> expectedValues = expectations._2;

        if (decisionResult.isEmpty()) {
          unexpected.put("Error", "No rule applied\n");
        } else {
          // convert to decisionResults to string, as they are converted to strings when read from Excel
          List<String> resultStrings = decisionResult.collectEntries(outputName)
              .stream()
              .map(result -> result.toString())
              .collect(Collectors.toList());

          List<Object> mismatchedExpectations = expectedValues
              .stream()
              .filter(expected -> (resultStrings.contains(expected) == false))
              .collect(Collectors.toList());
          List<Object> unexpectedResults = resultStrings
              .stream()
              .filter(resultString -> (expectedValues.contains(resultString) == false))
              .collect(Collectors.toList());
          if (mismatchedExpectations.isEmpty() == false || unexpectedResults.isEmpty() == false) {
            unexpected.put(outputName, new EvaluatedResult(mismatchedExpectations, unexpectedResults));
          }
        }
        return unexpected;
      };
}
