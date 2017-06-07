package com.camunda.consulting.dmn_excel_tester.data;

public class EvaluatedResult {
  
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
    return "EvaluatedResult [expected=" + expected + ", result=" + result + "]";
  }

}
