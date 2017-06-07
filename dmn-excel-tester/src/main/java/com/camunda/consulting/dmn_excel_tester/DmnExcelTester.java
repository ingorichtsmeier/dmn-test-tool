package com.camunda.consulting.dmn_excel_tester;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.data.EvaluatedResult;
import com.camunda.consulting.dmn_excel_tester.logic.DmnEvaluator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelDmnValidator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

public class DmnExcelTester {
  
  private static final Logger log = LoggerFactory.getLogger(DmnExcelTester.class);
  
  public static void main(String[] args) throws Docx4JException, Xlsx4jException {
    // handle the input args
    if (args.length < 2) {
      printUsage();
    }
    
    String excelSheetFilename = args[1]; 
    String dmnFileName = args[0];
    
    if (excelSheetFilename == null || dmnFileName == null) {
      printUsage();
    }
    
    // create the decision
    System.out.println(MessageFormat.format("Test of DMN table ''{0}''\nwith values from Excel sheet ''{1}''\n", dmnFileName, excelSheetFilename));
    File dmnTableFile = new File(dmnFileName);
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    
    // open the excel file
    File excelFile = new File(excelSheetFilename);
    
    // read the sheet
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    // validate the excel to decision
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> validationResult = excelDmnValidator.validateMatchingExcelAndDmnModel();
    if (validationResult.size() > 0) {
      System.out.println("Excelsheet doesn't fit to the dmn table: ");
      System.out.println(validationResult.toString());
      System.exit(2);
    }
    
    // evaluate the decisions with values from the excel sheet
    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
    List<Map<String,Object>> expectationsMismatches = dmnEvaluator.evaluateAllExpectations();
    
    System.out.println("Results:");
    int i = 0;
    for (Map<String, Object> mismatchLine : expectationsMismatches) {
      if (i != 0) {
        if (mismatchLine.isEmpty()) {
          System.out.println(MessageFormat.format("Line {0} correct \n", i));
        } else {
          System.out.println(MessageFormat.format("Line {0} with errors:", i));
          for (String key : mismatchLine.keySet()) {
            EvaluatedResult evaluatedResult = (EvaluatedResult)mismatchLine.get(key);
            System.out.println(MessageFormat.format("{0}: expected: ''{1}'', result: ''{2}''", 
                key, 
                evaluatedResult.getExpected(), 
                evaluatedResult.getResult()));
          }
          System.out.println();
        }
      }
      i++;
    }
    // save the results in a new tab and mark the results 'green' or 'red'

  }

  private static void printUsage() {
    System.out.println("usage: java -jar whatever dmnFile.dmn excelSheet.xlxs" );
    System.exit(1);
  }

}
