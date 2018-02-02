package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.dmn_excel_tester.functional.Tuple;

public class DmnTablePreparer {
  
  public static final String HEADER_REPLACE_REGEX = "[\\? -()<>/=-]";
  
  private static final Logger log = LoggerFactory.getLogger(DmnTablePreparer.class);

  public Tuple<DmnModelInstance, Map<String, String>> prepareTable(DmnModelInstance modelInstance) {
    
    Collection<InputExpression> inputExpressions = modelInstance.getModelElementsByType(InputExpression.class);
    for (Iterator<InputExpression> iterator = inputExpressions.iterator(); iterator.hasNext();) {
      InputExpression inputExpression = iterator.next();
      Input inputElement = (Input) inputExpression.getParentElement();
      if (inputExpression.getText() != null) {
        log.info("input expression content: {}", inputExpression.getText().getTextContent());
      } else {
        log.info("input expression content is empty");
      }
      String inputExpressionContent = inputElement.getLabel().replaceAll(HEADER_REPLACE_REGEX, "_");
      Text textElement = modelInstance.newInstance(Text.class);
      textElement.setTextContent(inputExpressionContent);
      inputExpression.setText(textElement);
    }
    
    Collection<Output> outputs = modelInstance.getModelElementsByType(Output.class);
    for (Iterator<Output> iterator = outputs.iterator(); iterator.hasNext();) {
      Output output = iterator.next();
      output.setName(output.getLabel().replaceAll(HEADER_REPLACE_REGEX, "_"));
    }
    return new Tuple<DmnModelInstance, Map<String, String>>(modelInstance, null);
  }
  
  public static Function<String, String> replaceSpecialChars = (String input) -> { return input.replaceAll(HEADER_REPLACE_REGEX, "_"); };
  
  public static BiFunction<String, String, String> calculateHeader = (String headerLabel, String headerInput) -> {
    return 
        (headerInput != null && headerInput.trim().length() > 0) ? headerInput : replaceSpecialChars.apply(headerLabel);
  };
  
  public static Consumer<? super Input> transformInputHeaders = (Input dmnInput) -> {
    //--- side effects!
    Text textElement = dmnInput.getModelInstance().newInstance(Text.class);
    textElement.setTextContent(calculateHeader.apply(dmnInput.getLabel(), dmnInput.getInputExpression().getTextContent()));
    //--------
    dmnInput.getInputExpression().setText(textElement);
  };
  
  public static Consumer<? super Output> transformOutputHeaders = (Output dmnOutput) -> {
    dmnOutput.setName(calculateHeader.apply(dmnOutput.getLabel(), dmnOutput.getName()));
  };

  public static Function<DmnModelInstance, Tuple<DmnModelInstance, ConcurrentMap<String, String>>> getDmnTableHeaders = (DmnModelInstance modelInstance) -> {
    Collection<Input> dmnInputs = modelInstance.getModelElementsByType(Input.class);
    dmnInputs.forEach(transformInputHeaders);
    Collection<Output> dmnOutputs = modelInstance.getModelElementsByType(Output.class);
    dmnOutputs.forEach(transformOutputHeaders);
    
    ConcurrentMap<String, String> headerMap = dmnInputs.parallelStream().collect(
        () -> new ConcurrentHashMap<String, String>(),
        // accumulator
        (ConcurrentMap<String, String> map, Input input) -> { map.putIfAbsent(input.getLabel(), input.getInputExpression().getTextContent()); },
        // combiner
        (ConcurrentMap<String, String> m1, ConcurrentMap<String, String> m2) -> {m1.putAll(m2);}
        );
    headerMap.putAll(dmnOutputs.parallelStream().collect(
        () -> new ConcurrentHashMap<String, String>(), 
        (ConcurrentMap<String, String> map, Output output) -> { map.putIfAbsent(output.getLabel(), output.getName()); }, 
        (ConcurrentMap<String, String> m1, ConcurrentMap<String, String> m2) -> { m1.putAll(m2); }
        ));
    return new Tuple<DmnModelInstance, ConcurrentMap<String,String>>(modelInstance, headerMap);
  };
  
}
