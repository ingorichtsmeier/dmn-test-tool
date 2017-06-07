package com.camunda.consulting.dmn_excel_tester.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Coordinates {
  
  private String columnName;
  private int columnIndex;
  private int lineNumber;
  
  private Pattern columnPattern = Pattern.compile("[A-Z]+");
  private Pattern linePattern = Pattern.compile("[0-9]+");

  public Coordinates(String cellCoordinates) {
    Matcher columnMatcher = columnPattern.matcher(cellCoordinates);
    if (columnMatcher.find()) {
      columnName = columnMatcher.group();
    }
    Matcher lineMatcher = linePattern.matcher(cellCoordinates);
    if (lineMatcher.find()) {
      lineNumber = Integer.valueOf(lineMatcher.group());
    }
    columnIndex = calculateColumnIndex(columnName);
  }

  
  /**
   * @return the column name from the Excel sheet, e.g. C or AD
   */
  public String getColumnName() {
    return columnName;
  }

  public int getColumnIndex() {
    return columnIndex;
  }

  public int getLineNumber() {
    return lineNumber;
  }
  
  private int calculateColumnIndex(String columnName) {
    if (columnName.length() == 1) {
      return columnName.charAt(0) - 'A';
    } else {
      return 26 * (columnName.charAt(0) - 'A' + 1) + calculateColumnIndex(columnName.substring(1));
    }
  }

  
}
