package com.camunda.consulting.dmn_excel_tester.logic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelDmnValidator {
  
  private static final Logger log = LoggerFactory.getLogger(ExcelDmnValidator.class);
  
  private List<Map<String, Object>> dataFromExcel;
  private DmnModelInstance dmnModelInstance;

  public ExcelDmnValidator(List<Map<String, Object>> dataFromExcel, DmnModelInstance dmnModelInstance) {
    this.dataFromExcel = dataFromExcel;
    this.dmnModelInstance = dmnModelInstance;
  }
  
  public List<String> validateMatchingExcelAndDmnModel() {
    List<String> errorResultList = new ArrayList<String>();
    List<String> dmnInputHeaderLabels = new ArrayList<String>();
    Collection<Input> dmnInputHeaders = dmnModelInstance.getModelElementsByType(Input.class);
    for (Iterator<Input> iterator = dmnInputHeaders.iterator(); iterator.hasNext();) {
      Input dmnInputHeader = (Input) iterator.next();
      log.info("DmnInputHeader: {}", dmnInputHeader.getLabel());
      dmnInputHeaderLabels.add(dmnInputHeader.getLabel());
    }
    
    List<String> dmnOutputHeaderLables = new ArrayList<String>();
    Collection<Output> dmnOutputHeaders = dmnModelInstance.getModelElementsByType(Output.class);
    for (Iterator<Output> iterator = dmnOutputHeaders.iterator(); iterator.hasNext();) {
      Output dmnOutputHeader = (Output) iterator.next();
      log.info("DmnOutputHaeder: {}", dmnOutputHeader.getLabel());
      dmnOutputHeaderLables.add(dmnOutputHeader.getLabel());
    }
    
    Map<String, Object> excelHeaders = dataFromExcel.get(1);
    Collection<Object> headers = excelHeaders.values();
    for (Iterator<Object> iterator = headers.iterator(); iterator.hasNext();) {
      String header = (String) iterator.next();
      log.info("validate header {}", header);
      if (header.startsWith("Expected: ")) {
        // output column
        String outputHeader = header.replaceAll("Expected: ", "");
        if (dmnOutputHeaderLables.contains(outputHeader)) {
          // everything ok
        } else {
          String message = MessageFormat.format("Column ''{0}'' not found in Dmn Output", outputHeader);
          errorResultList.add(message);
        }
      } else {
        // input column
        if (dmnInputHeaderLabels.contains(header)) {
          // everything ok
        } else {
          String message = MessageFormat.format("Column ''{0}'' not found in Dmn Inputs", header);
          errorResultList.add(message);
        }
      }
    }
    return errorResultList;
  }

}
