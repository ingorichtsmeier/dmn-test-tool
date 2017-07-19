package com.camunda.consulting.dmn_excel_tester.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluatedResult {
  
  private static final Logger log = LoggerFactory.getLogger(EvaluatedResult.class);
  
  private Object expected;
  private Object result;

  public EvaluatedResult(Object expected, Object result) {
    this.expected = expected;
    this.result = result;
  }

  public Object getExpected() {
    return expected;
  }

  public void setExpected(Object expected) {
    this.expected = expected;
  }

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EvaluatedResult) {
      if (((EvaluatedResult) obj).getExpected().equals(this.getExpected()) &&
          ((EvaluatedResult) obj).getResult().equals(this.getResult())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "EvaluatedResult [expected=" + expected + "(" + expected.hashCode() + 
        "), result=" + result + "(" + result.hashCode() + ")]";
  }

}
