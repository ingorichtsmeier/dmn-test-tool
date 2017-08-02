package com.camunda.consulting.dmn_excel_tester.logic;

import java.util.Collection;
import java.util.Iterator;

import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnTablePreparer {
  
  public static final String HEADER_REPLACE_REGEX = "[\\? -()<>/=]";
  
  private static final Logger log = LoggerFactory.getLogger(DmnTablePreparer.class);
  private DmnModelInstance modelInstance;
  // private List<Map<String, Object>> headers;

  public DmnTablePreparer(DmnModelInstance modelInstance /*, List<Map<String, Object>> headers*/) {
    this.modelInstance = modelInstance;
    // this.headers = headers;
  }

  /**
   * Fill the input expressions with the labels of the input elements.
   * Replace any special characters in the expressions with _.
   * 
   * @return decision model with input expressions filled
   */
  public DmnModelInstance prepareTable() {
    
    Collection<InputExpression> inputExpressions = modelInstance.getModelElementsByType(InputExpression.class);
    for (Iterator<InputExpression> iterator = inputExpressions.iterator(); iterator.hasNext();) {
      InputExpression inputExpression = (InputExpression) iterator.next();
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
      Output output = (Output) iterator.next();
      output.setName(output.getLabel().replaceAll("[ -]", "_"));
    }
    return modelInstance;
  }
}
