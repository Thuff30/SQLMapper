This is a simple Java application that will create a CSV listing pertinent MySQL or MS SQL database information as well as a file that can be imported into Draw.io to create a basic ERD. This is accomplished by performing several automated queries to the provided database. The results are stored in assorted HashMaps that are indexed to export the needed information in an organized and easily readable format

The application relies on JDBC drivers mssql-jdbc-8.2.2.jre8.jar and mysql-connector-java-8.0.18.jar(included in the files). These must be configured in the root folder for the source files.

After launching the user will need to enter basic connection information when prompted (host name, connection port, admin credentials, and the name of the database to be mapped). The application will then perform several queries on the database to gather the needed information. Simply open the CSV titled "<database> Tables.csv" to see a description of the database's tables and their columns. It will also list all constraints, primary keys, and foreign keys, as well as the columns they reference.

The file named "<database> DrawIOImport.csv" can be imported into the Draw.io desktop application or the web based version. To do this go to https://www.draw.io/ and start a new diagram. Select Arrange>Insert>Advanced>CSV... and copy and paste everything then select Import.
