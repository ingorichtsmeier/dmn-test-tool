package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.logic.ExcelDmnValidator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

public class ExcelDmnValidatorTest {
  
  @Test
  public void testExcelDmnValidatorFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/dish-3Expected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).isEmpty();
  }

  @Test
  public void testUnmatchedHeaders() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/dish-simpleExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/dish-simple.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).hasSize(3);
    assertThat(errorList).contains("Column 'Jahreszeit' not found in Dmn Inputs of table 'Dish'"); 
    assertThat(errorList).contains("Column 'Gästeanzahl' not found in Dmn Inputs of table 'Dish'");
    assertThat(errorList).contains("Column 'Mahlzeit' not found in Dmn Output of table 'Dish'");
  }
  
  @Test
  public void testDRDHeadersFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).isEmpty();
  }

  public Map<String, List<Map<String, Object>>> readExcelFile(String fileName) throws Docx4JException, Xlsx4jException {
    File excelFile = new File(fileName);
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);    
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    return dataFromExcel;
  }

  public DmnModelInstance readDmnModelInstance(String fileName) {
    File dmnTableFile = new File(fileName);
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    return dmnModelInstance;
  }

}
