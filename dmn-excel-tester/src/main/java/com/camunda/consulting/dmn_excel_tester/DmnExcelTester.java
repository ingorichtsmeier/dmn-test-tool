package com.camunda.consulting.dmn_excel_tester;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.camunda.consulting.dmn_excel_tester.logic.DmnTablePreparer;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelDmnValidator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class DmnExcelTester extends Application {
  
  private static final Font helvetica21 = Font.font("Helvetica Neue,Helvetica,Arial,sans-serif", FontWeight.NORMAL, 21);
  private static final Font helvetica14 = Font.font("Helvetica Neue,Helvetica,Arial,sans-serif", FontWeight.NORMAL, 14);

  private static final Logger log = LoggerFactory.getLogger(DmnExcelTester.class);
  
  String excelSheetFilename;
  String dmnFileName;

  public static void main(String[] args) throws Docx4JException, Xlsx4jException {
    DmnExcelTester dmnExcelTester = new DmnExcelTester();
    dmnExcelTester.evaluateDecisionsFromExcelInput(args);
  }

  public void evaluateDecisionsFromExcelInput(String[] args) throws Docx4JException, Xlsx4jException {
    // handle the input args
    if (args.length == 0) {
      launch(args);
      System.exit(0);
    }
    
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
    
    List<Map<String, Object>> expectationsMismatches = readAndEvaluateDecsions(excelSheetFilename, dmnFileName);
    
    System.out.println("Results:");
    System.out.println(formatResultsForGui(expectationsMismatches));
    // save the results in a new tab and mark the results 'green' or 'red'
  }

  public List<Map<String, Object>> readAndEvaluateDecsions(String excelSheetFilename, String dmnFileName) throws Docx4JException, Xlsx4jException {
    File dmnTableFile = new File(dmnFileName);
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    DmnTablePreparer dmnTablePreparer = new DmnTablePreparer(dmnModelInstance);
    DmnModelInstance preparedTable = dmnTablePreparer.prepareTable();
    
    // open the excel file
    File excelFile = new File(excelSheetFilename);
    
    // read the sheet
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);
    List<Map<String,Object>> dataFromExcel = excelSheetReader.getDataFromExcel();
    
    // validate the excel to decision
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, preparedTable);
    List<Map<String,Object>> expectationsMismatches = new ArrayList<Map<String, Object>>();
    List<String> validationResult = excelDmnValidator.validateMatchingExcelAndDmnModel();
    if (validationResult.size() > 0) {
      expectationsMismatches.add(new HashMap<>());
      HashMap<String, Object> validatonErrorMessage = new HashMap<>();
      validatonErrorMessage.put("error:", "Excelsheet doesn't fit to the dmn table: ");
      validatonErrorMessage.put("details:", validationResult.toString());
      expectationsMismatches.add(validatonErrorMessage);
    } else {    
      // evaluate the decisions with values from the excel sheet
      DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
      List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
      DmnEvaluator dmnEvaluator = new DmnEvaluator(decisions, dataFromExcel);
      expectationsMismatches = dmnEvaluator.evaluateAllExpectations();
    }
    return expectationsMismatches;
  }

  public List<Text> formatResultsForGui(List<Map<String, Object>> expectationsMismatches) {
    List<Text> result = new ArrayList<Text>();
    int i = 0;
    for (Map<String, Object> mismatchLine : expectationsMismatches) {
      if (i != 0) {
        if (mismatchLine.isEmpty()) {
          Text correctLine = new Text(MessageFormat.format("Line {0}: correct \n\n", i));
          correctLine.setFill(Color.GREEN);
          correctLine.setFont(helvetica14);
          result.add(correctLine);
        } else {
          StringBuilder errorLineBuilder = new StringBuilder();
          errorLineBuilder.append(MessageFormat.format("Line {0} with errors:\n", i));
          for (String key : mismatchLine.keySet()) {
            if (mismatchLine.get(key) instanceof EvaluatedResult) {
              EvaluatedResult evaluatedResult = (EvaluatedResult)mismatchLine.get(key);
              errorLineBuilder.append(MessageFormat.format("{0}: expected: ''{1}'', result: ''{2}''\n", 
                  key, 
                  evaluatedResult.getExpected(), 
                  evaluatedResult.getResult()));
            } else if (mismatchLine.get(key) instanceof String) {
              errorLineBuilder.append(mismatchLine.get(key));
            }
          }
          errorLineBuilder.append('\n');
          Text errorLine = new Text(errorLineBuilder.toString());
          errorLine.setFill(Color.RED);
          errorLine.setFont(helvetica14);
          result.add(errorLine);
        }
      }
      i++;
    }
    return result;
  }
  
  private void printUsage() {
    System.out.println("usage: java -jar whatever dmnFile.dmn excelSheet.xlxs" );
    System.exit(1);
  }
  
  File recentDirectory = new File(System.getProperty("user.home"));
  FileChooser dmnFileChooser = new FileChooser();
  FileChooser excelFileChooser = new FileChooser();
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    primaryStage.setTitle("Dmn Evaluator");
    
    dmnFileChooser.setTitle("Choose a Dmn File");
    dmnFileChooser.getExtensionFilters().addAll(
        new ExtensionFilter("Dmn Files", "*.dmn"),
        new ExtensionFilter("All files", "*.*"));
    dmnFileChooser.setInitialDirectory(recentDirectory);
    excelFileChooser.setTitle("Choose an Excel File");
    excelFileChooser.getExtensionFilters().addAll(
        new ExtensionFilter("Excel Files", "*.xlsx"),
        new ExtensionFilter("All Files", "*.*"));
    excelFileChooser.setInitialDirectory(recentDirectory);
    
    ProgressIndicator progressIndicator = new ProgressIndicator();
    progressIndicator.setMaxSize(25, 25);
    progressIndicator.setVisible(false);

    GridPane grid = new GridPane();
    grid.setAlignment(Pos.TOP_LEFT);
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10, 10, 10, 10));
    grid.setBackground(
        new Background(
            new BackgroundFill(
                Color.WHITE, 
                new CornerRadii(5.0), 
                new Insets(0.0, 5.0, 0.0, 5.0))));

    HBox titleBox = new HBox();
    Text logo = new Text('\ue831' + "");
    logo.setFont(Font.loadFont(getClass().getResourceAsStream("/assets/fonts/bpmn.woff"), 21));
    logo.setFill(Color.web("#b5152b"));
    Text scenetitle = new Text(" DMN Evaluator");
    scenetitle.setFont(helvetica21);
    titleBox.getChildren().addAll(logo, scenetitle);
    titleBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(titleBox, 0, 0, 2, 1);

    Label dmnName = new Label("DMN File:");
    dmnName.setFont(helvetica14);
    grid.add(dmnName, 0, 1);

    TextField dmnFileField = new TextField();
    dmnFileField.setPrefColumnCount(50);
    grid.add(dmnFileField, 1, 1);

    Button selectDmnFile = new Button("Select Dmn File");
    selectDmnFile.setFont(helvetica14);
    grid.add(selectDmnFile, 2, 1);
    
    selectDmnFile.setOnAction((final ActionEvent e) -> {
      File file = dmnFileChooser.showOpenDialog(primaryStage);
      if (file != null) {
          dmnFileName = file.getAbsoluteFile().getPath();
          setRecentDirectoryToFileChooser(file);
          dmnFileField.setText(dmnFileName);
      }
    });

    Label excelLabel = new Label("Excel File:");
    excelLabel.setFont(helvetica14);
    grid.add(excelLabel, 0, 2);

    TextField excelFileField = new TextField();
    grid.add(excelFileField, 1, 2);

    Button selectExcelFile = new Button("Select Excel File");
    selectExcelFile.setFont(helvetica14);
    grid.add(selectExcelFile, 2, 2);
    
    selectExcelFile.setOnAction((final ActionEvent e) -> {
      File file = excelFileChooser.showOpenDialog(primaryStage);
      if (file != null) {
        excelSheetFilename = file.getAbsoluteFile().getPath();
        setRecentDirectoryToFileChooser(file);
        excelFileField.setText(excelSheetFilename);
      }
    });

    Button evaluateBtn = new Button("Evaluate");
    evaluateBtn.setFont(helvetica14);
    HBox evaluateAndProgressBox = new HBox(10, evaluateBtn, progressIndicator);
    grid.add(evaluateAndProgressBox, 1, 4);

    VBox resultLabelBox = new VBox();
    Label resultLabel = new Label("Result:");
    resultLabel.setFont(helvetica14);
    resultLabelBox.getChildren().add(resultLabel);
    grid.add(resultLabelBox, 0, 6);

    final TextFlow resultArea = new TextFlow();
    ScrollPane scrollPane = new ScrollPane(resultArea);
    scrollPane.setPrefHeight(200);
    grid.add(scrollPane, 1, 6);

    evaluateBtn.setOnAction((final ActionEvent e) -> handleEvaluateButton(resultArea, progressIndicator));
    
    ColumnConstraints column1Constraints = new ColumnConstraints();
    ColumnConstraints column2Constraints = new ColumnConstraints();
    column2Constraints.setHgrow(Priority.ALWAYS);
    ColumnConstraints column3Constraints = new ColumnConstraints();
    grid.getColumnConstraints().addAll(column1Constraints, column2Constraints, column3Constraints);
    
    RowConstraints row1Constraints = new RowConstraints();
    RowConstraints row2Constraints = new RowConstraints();
    RowConstraints row3Constraints = new RowConstraints();
    RowConstraints row4Constraints = new RowConstraints();
    RowConstraints row5Constraints = new RowConstraints();
    RowConstraints row6Constraints = new RowConstraints();
    RowConstraints row7Constraints = new RowConstraints();
    row7Constraints.setVgrow(Priority.ALWAYS);
    grid.getRowConstraints().addAll(row1Constraints, row2Constraints, row3Constraints, row4Constraints, row5Constraints, row6Constraints, row7Constraints);
        
    Scene scene = new Scene(grid/*, 300, 275*/);
    primaryStage.setScene(scene);

    primaryStage.show();
    
  }
  
  private void handleEvaluateButton(TextFlow resultArea, ProgressIndicator progressIndicator) {
    if (progressIndicator.isVisible()) {
      return;
    }
    
    progressIndicator.setVisible(true);
    resultArea.getChildren().clear();
    
    Task<List<Text>> resultLoader = new Task<List<Text>>() {
      {
        setOnSucceeded(workerStateEvent -> {
          resultArea.getChildren().addAll(getValue());
          progressIndicator.setVisible(false); // stop displaying the loading indicator
        });

        setOnFailed(workerStateEvent -> getException().printStackTrace());
      }

      @Override
      protected List<Text> call() throws Exception {
        return formatResultsForGui(readAndEvaluateDecsions(excelSheetFilename, dmnFileName));
      }
    };

    Thread loadingThread = new Thread(resultLoader, "evaluator");
    loadingThread.setDaemon(true);
    loadingThread.start();      
  }
  
  private void setRecentDirectoryToFileChooser(File file) {
    recentDirectory = file.getAbsoluteFile().getParentFile();
    dmnFileChooser.setInitialDirectory(recentDirectory);
    excelFileChooser.setInitialDirectory(recentDirectory);
  }

}
