Test DMN Tables with data from Excel
====================================

With this tool you test your DMN decision table against the data from an Excel sheet.

![DMN table](documentation/dmn-table-example.png)

The Excel sheet has to be structured like the decision table:
A Header line contains the input and output names from the decision tables. The output names must be preceded with `Expected: `

![Excel example](documentation/excel-example.png)

Then you can run the java program `com.camunda.consulting.dmn_excel_tester.DmnExcelTester` with the filenames of the dmn table and the excel sheet and check the outputs for the result.

![Console output](documentation/console-output.png)