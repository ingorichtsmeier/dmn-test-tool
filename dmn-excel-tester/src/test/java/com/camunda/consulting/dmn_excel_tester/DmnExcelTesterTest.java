package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DmnExcelTesterTest {
  
  @Test
  public void testFormatListOneElement() {
    List<String> input = Arrays.asList("Simple Input");
    String formatted = DmnExcelTester.formatList.apply(input);
    assertThat(formatted).isEqualTo("{'Simple Input'}");
  }
  
  @Test
  public void testFormatListTwoElemnts() {
    List<String> input = Arrays.asList("One", "Two");
    String formatted = DmnExcelTester.formatList.apply(input);
    assertThat(formatted).isEqualTo("{'One', 'Two'}");
  }
  
  @Test
  public void testFormatEmptyList() {
    List<String> input = new ArrayList<>();
    String formatted = DmnExcelTester.formatList.apply(input);
    assertThat(formatted).isEqualTo("{}");
  }
}
