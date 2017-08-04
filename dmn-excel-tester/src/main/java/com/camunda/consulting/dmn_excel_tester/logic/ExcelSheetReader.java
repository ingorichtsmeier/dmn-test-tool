package com.camunda.consulting.dmn_excel_tester.logic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBElement;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.SpreadsheetML.SharedStrings;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorksheetPart;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.relationships.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlsx4j.exceptions.Xlsx4jException;
import org.xlsx4j.sml.Cell;
import org.xlsx4j.sml.Row;
import org.xlsx4j.sml.STCellType;
import org.xlsx4j.sml.SheetData;
import org.xlsx4j.sml.Worksheet;

import com.camunda.consulting.dmn_excel_tester.data.Coordinates;

public class ExcelSheetReader {
  
  private static final Logger log = LoggerFactory.getLogger(ExcelSheetReader.class);
  
  private File excelFile;
  
  private List<WorksheetPart> worksheets = new ArrayList<WorksheetPart>();
  
  private SharedStrings sharedStrings = null;
  
  private HashMap<Part, Part> handled = new HashMap<Part, Part>();
  
  private final HashMap<String, Object> emptyRow = new HashMap<String, Object>();

  public ExcelSheetReader(File excelFile) {
    this.excelFile = excelFile; 
  }
  
  public List<Map<String, Object>> getDataFromExcel() throws Docx4JException, Xlsx4jException {    
    OpcPackage spreadSheetPackage = (OpcPackage) SpreadsheetMLPackage.load(excelFile);
    
    // List the parts by walking the rels tree
    RelationshipsPart rp = spreadSheetPackage.getRelationshipsPart();
    StringBuilder sb = new StringBuilder();
    printInfo(rp, sb, "");
    traverseRelationships(spreadSheetPackage, rp, sb, "    ");
    
    log.info("StringBuilder: {}", sb.toString());
    
    ArrayList<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
    resultList.add(emptyRow); // first element at index 0
        
    for(WorksheetPart sheet: worksheets) {
      log.info("part name: {}", sheet.getPartName().getName() );
      Worksheet ws = sheet.getJaxbElement();
      SheetData data = ws.getSheetData();
      HashMap<String, Object> headerContent = new HashMap<String, Object>();
      
      for (Row row : data.getRow()) {
        Long rowIndex = row.getR();
        log.info("row: {}", rowIndex);
        HashMap<String, Object> rowContent = new HashMap<String, Object>();
        
        List<Cell> cells = row.getC();
        for (Cell cell : cells) {
          Coordinates coordinates = new Coordinates(cell.getR());
          if (cell.getT().equals(STCellType.S)) {
            String cellValue = sharedStrings.getJaxbElement().getSi().get(Integer.parseInt(cell.getV())).getT().getValue();
            log.info("  {} (S) contains {}", cell.getR(), cellValue);
            // cell.getR() contains coordinates A1, B2 or C3 (Column, Row)
            if (rowIndex.equals(1L)) {
              // handle header 
              headerContent.put(coordinates.getColumnName(), cellValue); //.replaceAll("[ -]+", "_")
              log.info("Set header column {} to {}", coordinates.getColumnName(), cellValue);
            } else {
              // handle content
              String columnNameEscaped = ((String) headerContent.get(coordinates.getColumnName()))
                  .replaceAll(DmnTablePreparer.HEADER_REPLACE_REGEX, "_");
              if (cellValue.contains(";")) {
                // it's a list for hitpolicy collect
                StringTokenizer stringTokenizer = new StringTokenizer(cellValue, ";", false);
                List<String> cellList = new ArrayList<String>();
                while (stringTokenizer.hasMoreElements()) {
                  String cellElement = (String) stringTokenizer.nextElement();
                  cellList.add(translateValue(cellElement.trim()));
                }
                rowContent.put(columnNameEscaped, cellList);
                log.info("Fill key {} with list {}", columnNameEscaped, cellList);
              } else {
                // it's just an ordinary string
                rowContent.put(columnNameEscaped, translateValue(cellValue));
                log.info("Fill key {} with content {}", columnNameEscaped, cellValue);
              }
            }
          } else {
            // TODO: handle other cell types
            String cellValue = cell.getV();
            log.info("  {} contains {}", cell.getR(), cellValue );
            String columnNameEscaped = ((String) headerContent.get(coordinates.getColumnName())).replaceAll("[ -]+", "_");
            rowContent.put(columnNameEscaped, cellValue);
            log.info("Fill other cell type key {} with content {}", columnNameEscaped, cellValue);
          }
        }
        if (resultList.size() <= rowIndex) {
          resultList.add(rowContent);
          log.info("Added content {}", rowContent.toString());
        } else {
          resultList.set(rowIndex.intValue(), rowContent);
          log.info("Set index {} to content {}", rowIndex.intValue(), rowContent.toString());
        }
      }
      resultList.set(1, headerContent);
    }

    return resultList;
  }
  
  public String translateValue(String value) {
    if (value != null) {
      if ("yes".equals(value.toLowerCase())) {
        return "true";
      } else if ("no".equals(value.toLowerCase())) {
        return "false";
      } else if ("ja".equals(value.toLowerCase())) {
        return "true";
      } else if ("nein".equals(value.toLowerCase())){
        return "false";
      } else {
        return value;
      }
    } else {
      return value;
    }
  } 
  
  private void  printInfo(Part p, StringBuilder sb, String indent) {
    sb.append("\n" + indent + "Part " + p.getPartName() + " [" + p.getClass().getName() + "] " );   
    if (p instanceof JaxbXmlPart) {
      Object o = ((JaxbXmlPart)p).getJaxbElement();
      if (o instanceof JAXBElement) {
        sb.append(" containing JaxbElement:" + XmlUtils.JAXBElementDebug((JAXBElement)o) );
      } else {
        sb.append(" containing JaxbElement:"  + o.getClass().getName() );
      }
    }
    if (p instanceof WorksheetPart) {
      worksheets.add((WorksheetPart)p);
    } else if (p instanceof SharedStrings) {
      sharedStrings = (SharedStrings)p;
    }
  }
  
  private void traverseRelationships(org.docx4j.openpackaging.packages.OpcPackage wordMLPackage, 
      RelationshipsPart rp, 
      StringBuilder sb, String indent) {
    
    // TODO: order by rel id
    
    for ( Relationship r : rp.getRelationships().getRelationship() ) {
      log.info("For Relationship Id=" + r.getId() 
          + " Source is " + rp.getSourceP().getPartName() 
          + ", Target is " + r.getTarget() );
    
      if (r.getTargetMode() != null
          && r.getTargetMode().equals("External") ) {
        
        sb.append("\n" + indent + "external resource " + r.getTarget() 
               + " of type " + r.getType() );
        continue;       
      }
      
      Part part = rp.getPart(r);
          
      printInfo(part, sb, indent);
      if (handled.get(part)!=null) {
        sb.append(" [additional reference] ");
        continue;
      }
      handled.put(part, part);
      if (part.getRelationshipsPart()==null) {
        // sb.append(".. no rels" );            
      } else {
        traverseRelationships(wordMLPackage, part.getRelationshipsPart(), sb, indent + "    ");
      }
          
    }   
  }

}
