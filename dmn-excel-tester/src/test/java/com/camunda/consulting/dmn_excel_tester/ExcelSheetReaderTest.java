package com.camunda.consulting.dmn_excel_tester;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.data.Coordinates;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

import static org.assertj.core.api.Assertions.*;

public class ExcelSheetReaderTest {
  
  @Test
  public void testReadExcelSheet() throws Docx4JException, Xlsx4jException {
    File excelFile = new File("src/test/resources/dish-3Expected.xlsx");   
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    assertThat(dataFromExcel).hasSize(3 + 1); // index 0 not used
    assertThat(dataFromExcel.get(1)).containsOnly(
        entry("A", "Season"), 
        entry("B", "How many guests"), 
        entry("C", "Expected: Dish")); // header column
    assertThat(dataFromExcel.get(2)).containsOnly(
        entry("Season", "Fall"), 
        entry("How_many_guests", "7"), 
        entry("Expected:_Dish", "Spareribs"));
  }
  
  @Test
  public void testReadExcelSheet03Formula() throws Docx4JException, Xlsx4jException {
    File ecxelFile = new File("src/test/resources/03-Formulas-and-functionsExpected.xlsx");   
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(ecxelFile);    
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    assertThat(dataFromExcel.get(2)).containsOnly(
        entry("Season", "Spring"), 
        entry("Number_of_Guests", "3"), 
        entry("Expected:_Dish", "Dry aged gourmet steak"), 
        entry("Expected:_Amount", "3"), 
        entry("Expected:_Unit", "Small Pieces"));
  }
  
  @Test
  public void testCoordinateGetterA1() {
    String cellCoordinates = "A1";
    Coordinates coordinates = new Coordinates(cellCoordinates);
    assertThat(coordinates.getColumnName()).isEqualTo("A");
    assertThat(coordinates.getLineNumber()).isEqualTo(1);
    assertThat(coordinates.getColumnIndex()).isEqualTo(0);
  }

  @Test
  public void testCoordinateGetterAB102() {
    String cellCoordinates = "AB102";
    Coordinates coordinates = new Coordinates(cellCoordinates);
    assertThat(coordinates.getColumnName()).isEqualTo("AB");
    assertThat(coordinates.getLineNumber()).isEqualTo(102);
    assertThat(coordinates.getColumnIndex()).isEqualTo(27);
  }
  
  @Test
  public void testCoordianteGetterHS30() {
    String cellCoordiantes = "HC30";
    Coordinates coordinates = new Coordinates(cellCoordiantes);
    assertThat(coordinates.getColumnName()).isEqualTo("HC");
    assertThat(coordinates.getLineNumber()).isEqualTo(30);
    // H is 8th character, C is 3rd character.index starts at 0
    assertThat(coordinates.getColumnIndex()).isEqualTo(8*26+3-1); 
  }
  
  @Test
  public void testCoordinateGetterBF2() {
    String cellCoordinates = "BF2";
    Coordinates coordinates = new Coordinates(cellCoordinates);
    assertThat(coordinates.getColumnName()).isEqualTo("BF");
    assertThat(coordinates.getLineNumber()).isEqualTo(2);
    assertThat(coordinates.getColumnIndex()).isEqualTo(57);
  }
  
}