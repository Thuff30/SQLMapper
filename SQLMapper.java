package sqlmapper;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.Collections;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;

public class SQLMapper {
    
    
    public static void main(String[] args) {
       
        String host = "localhost";
        String port = "3306";
        String database = "shipping";
        String MYSQL_URL = "jdbc:mysql://" +host+ ":" +port+ "/" +database;
        String user = "root";
        String pass = "Sjrhdu3485";
        String targetDB = "shipping";
        String tablefile = "Tables";
        String usersfile = "Users and Roles";
        ArrayList<String> tables = new ArrayList<String>();

        HashMap<String, String> connectInfo = new HashMap();
        HashMap<String, String> dboptions = new HashMap();
        TreeMap<String, String> dbcolumns = new TreeMap();
        TreeMap<String, String> rolePriveleges = new TreeMap();
        TreeMap<String, String> tableConstraints = new TreeMap();
        TreeMap<String, String> userList = new TreeMap();
        TreeMap<String, String> roles = new TreeMap();
        TreeMap<String, String> tablePriveleges = new TreeMap();
        
        connectInfo = getDBInfo();
        Connection conn = dbconnect(MYSQL_URL, user, pass);
        dboptions = findDatabases(targetDB, conn);
        targetDB=getTarget(dboptions);
        tables = findTables(conn, targetDB);
        dbcolumns = findColumns(conn, targetDB, tables);
        tableConstraints = findConst(conn, targetDB, tables);
        //userList = findUsers(conn, targetDB);
        tablePriveleges = findTabPriv(conn, targetDB, userList);
        //rolePriveleges = findRolePriv(conn, targetDB);
        CreateTableCSV(tables, dbcolumns, tableConstraints);
        
    }
    
    //Get database information to establish connection
    public static HashMap<String, String> getDBInfo(){

        String dbType, host, port, database, user, pass;
        HashMap<String, String> connectInfo = new HashMap();
        Scanner userIn = new Scanner(System.in);

        System.out.println("Is the databse MySQL, MSSQL, or Oracle SQL?");
        dbType = userIn.nextLine();
        System.out.println("Enter the hostname for the database:");
        host=userIn.nextLine();
        System.out.println("Enter the correct connection port:");
        port=userIn.nextLine();
        System.out.println("Enter the name of a known database:");
        database=userIn.nextLine();
        String URL = "jdbc:mysql://" +host+ ":" +port+ "/" +database;
        connectInfo.put("URL", URL);
        System.out.println("Enter an database administrative username: ");
        user=userIn.nextLine();
        connectInfo.put("User", user);
        System.out.println("Enter the password for " +user+ ":");
        pass=userIn.nextLine();
        connectInfo.put("Pass", pass);

        return connectInfo;
    }
        
    //Create connection to database
    public static Connection dbconnect(String MYSQL_URL, String user, String pass){
        String JDBCDRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";
        Connection conn = null;
        try{    
            //Create connection
            Class.forName(JDBCDRIVER_MYSQL);
            System.out.println("Connecting to the server and database provided");
            conn = DriverManager.getConnection(MYSQL_URL, user, pass);
            System.out.println("Database connection successful");
            
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        return conn;
    }
    
    //Determine databases on server
    public static HashMap<String, String> findDatabases(String targetDB, Connection conn){
        //If not database is explicitly given, search for all options
        int numdb=1;
        String shDB = "SHOW DATABASES;";
        Statement stmt=null;
        HashMap<String, String> dbList = new HashMap();

        try{
            if(targetDB==""){
                stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(shDB);

                while(rs.next()){
                    //Store all databases in an array
                    dbList.put("DB" + numdb, rs.getString("Database"));
                    numdb++;
                }
                //close all statements/connections to preserve resources
                rs.close();
                stmt.close();

                for(int i=1;i<=numdb;i++){
                    String line = "DB" +i;
                    String thisLine=dbList.get(line);
                    if(thisLine.equalsIgnoreCase("information_schema") || thisLine.equalsIgnoreCase("performance_schema") || 
                            thisLine.equalsIgnoreCase("mysql") || thisLine.equalsIgnoreCase("sys")){
                        dbList.remove(line);
                    }
                }
            }else{
                dbList.put("DB1",targetDB);
            }
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        //Remove all system databases from the list

        return dbList;
    }
        
    //Determine the database to be mapped
    public static String getTarget(HashMap dboptions){

        String targetDB;
        Scanner userIn = new Scanner(System.in);
        //Prompt user to choose a database to map
        System.out.println("Please select a database from the below options:");
        Iterator it = dboptions.entrySet().iterator();    
        while(it.hasNext()){
            HashMap.Entry element=(HashMap.Entry)it.next();
            System.out.println((String)element.getValue());
        }
        targetDB= userIn.nextLine();
        System.out.println("Mapping " +targetDB);

        return targetDB;
    }

    //Find all tables on database
    public static ArrayList<String> findTables(Connection conn, String database){
        ArrayList<String> tables = new ArrayList<String>();
        String useDB = "USE " + database+ ";";
        String shTables = "SHOW TABLES;";

            try{
                Statement stmt = conn.createStatement();
                stmt.executeQuery(useDB);
                ResultSet rs1 = stmt.executeQuery(shTables);

                while(rs1.next()){
                    tables.add(rs1.getString("Tables_in_" +database));
                }
                //Close all connections/statements to preserve resources
                rs1.close();
                stmt.close();
                }catch(SQLException se){
                se.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            }
        System.out.println("Tables: " +tables);
        return tables;
    }

    //Search for all tables in selected database    
    public static TreeMap<String, String> findColumns(Connection conn, String database, ArrayList tables){
        TreeMap<String, String> columns = new TreeMap();
        String useDB = "USE " +database+ ";";

            try{
                Statement stmt = conn.createStatement();
                stmt.executeQuery(useDB);

                //Loop through all tables in the database
                for(int ts =0; ts<=tables.size()-1; ts++){  
                    ResultSet rs1 = stmt.executeQuery("DESCRIBE " +tables.get(ts));

                    //declare/reset integer to hold number of columns
                    int tick = 1;
                    
                    //Loop through all columns in the table and store
                    while(rs1.next()){
                        columns.put(tables.get(ts) + "-" + tick + "-Field", rs1.getString("Field"));
                        columns.put(tables.get(ts) + "-" + tick + "-Type",rs1.getString("Type"));
                        columns.put(tables.get(ts) + "-" + tick + "-Null", rs1.getString("Null"));
                        columns.put(tables.get(ts) + "-" + tick + "-Key", rs1.getString("Key"));
                        columns.put(tables.get(ts) + "-" + tick + "-Default", rs1.getString("Default"));
                        columns.put(tables.get(ts) + "-" + tick + "-Extra", rs1.getString("Extra"));
                        tick++;
                    }
                    //Enter a count of columns for table
                    columns.put(tables.get(ts) + "-Total", Integer.toString(tick - 1));
                    
                    //Close results, statement and connection
                     rs1.close();

                }
                stmt.close();
                System.out.println("Columns: " +columns);
                System.out.println();
            }catch(SQLException se){
                se.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            }
        return columns;
    }

    public static TreeMap<String, String> findConst(Connection conn, String targetDB, ArrayList tables){
        TreeMap<String, String> constraints = new TreeMap<String, String>();
        String keyQuery = "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME='";
        String constQuery1 = "SELECT CONSTRAINT_SCHEMA, CONSTRAINT_NAME, TABLE_SCHEMA, CONSTRAINT_TYPE, ENFORCED FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='";
        String constQuery2= "' AND CONSTRAINT_TYPE != 'PRIMARY KEY' OR 'FOREIGN KEY';";
        String useDB = "USE " +targetDB+ ";";
        int tick = 1;
        int tick2 = 1;
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);

            for(int ts =0; ts<=tables.size()-1; ts++){
                ResultSet rs1 = stmt.executeQuery(keyQuery+ tables.get(ts)+ "';" );

                while(rs1.next()){
                    constraints.put(tables.get(ts)+ "-" +tick+ "-Constraint Name", rs1.getString("CONSTRAINT_NAME"));
                    constraints.put(tables.get(ts)+ "-" +tick+ "-Column Name", rs1.getString("COLUMN_NAME"));
                    constraints.put(tables.get(ts)+ "-" +tick+ "-Referenced Table", rs1.getString("REFERENCED_TABLE_NAME"));
                    constraints.put(tables.get(ts)+ "-" +tick+ "-Referenced Column", rs1.getString("REFERENCED_COLUMN_NAME"));
                tick++;
                }
                rs1.close();
                constraints.put(tables.get(ts)+ "-Total", Integer.toString(tick - 1));

                ResultSet rs2= stmt.executeQuery(constQuery1+ tables.get(ts)+ constQuery2);

                while(rs2.next()){
                    constraints.put(tables.get(ts)+ "-" +tick2+ "-Constraint Schema", rs2.getString("CONSTRAINT_SCHEMA"));
                    constraints.put(tables.get(ts)+ "-" +tick2+ "-Constraint Name2", rs2.getString("CONSTRAINT_NAME"));
                    constraints.put(tables.get(ts)+ "-" +tick2+ "-Table Schema", rs2.getString("TABLE_SCHEMA"));
                    constraints.put(tables.get(ts)+ "-" +tick2+ "-Constraint Type", rs2.getString("CONSTRAINT_TYPE"));
                    constraints.put(tables.get(ts)+ "-" +tick2+ "-Enforced", rs2.getString("ENFORCED"));
                tick2++;
                }
                rs2.close();
                constraints.put(tables.get(ts)+ "-Total2", Integer.toString(tick2 - 1));

            }

            stmt.close();
            System.out.println("Constraints: " +constraints);
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        return constraints;
    }

    public static TreeMap<String,String> findUsers (Connection conn, String targetDB){
        TreeMap<String, String> userList = new TreeMap();
        String userQuery = "SELECT * FROM MYSQL.USER WHERE User != 'mysql.infoschema' AND User != 'mysql.session' AND User != 'mysql.sys';";
        int tick=1;
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs1=stmt.executeQuery(userQuery);

            while(rs1.next()){
                userList.put("Host-" +tick, rs1.getString("Host"));
                userList.put("User-" +tick, rs1.getString("User"));
              userList.put("Select-" +tick, rs1.getString("Select_priv"));
                userList.put("Insert-" +tick, rs1.getString("Insert_priv"));
                userList.put("Update-" +tick, rs1.getString("Update_priv"));
                userList.put("Delete-" +tick, rs1.getString("Delete_priv"));
                userList.put("Create-" +tick, rs1.getString("Create_priv"));
                userList.put("Drop-" +tick, rs1.getString("Drop_priv"));
                userList.put("Reload-" +tick, rs1.getString("Reload_priv"));
                userList.put("Shutdown-" +tick, rs1.getString("Shutdown_priv"));
                userList.put("Process-" +tick, rs1.getString("Process_priv"));
                userList.put("File-" +tick, rs1.getString("File_priv"));
                userList.put("Grant-" +tick, rs1.getString("Grant_priv"));
                userList.put("Index-" +tick, rs1.getString("Index_priv"));
                userList.put("Alter-" +tick, rs1.getString("Alter_priv"));
                userList.put("Show DB-" +tick, rs1.getString("Show_db_priv"));
                userList.put("Super-" +tick, rs1.getString("Super_priv"));
                userList.put("Lock Tables-" +tick, rs1.getString("Lock_tables_priv"));
                userList.put("Execute-" +tick, rs1.getString("Execute_priv"));
                userList.put("Replicate Slave-" +tick, rs1.getString("Repl_slave_priv"));
                userList.put("PLicate Client-" +tick, rs1.getString("Repl_client_priv"));
                userList.put("Create View-" +tick, rs1.getString("Create_view_priv"));
                userList.put("Show View-" +tick, rs1.getString("Show_view_priv"));
                userList.put("Create Routine-" +tick, rs1.getString("Create_routine_priv"));
                userList.put("Alter Routine-" +tick, rs1.getString("Alter_routine_priv"));
                userList.put("Create User-" +tick, rs1.getString("Create_user_priv"));
                userList.put("Max Connections-" +tick, rs1.getString("max_connections"));
                
                tick++;
            }
            
            
            rs1.close();
            stmt.close();

        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("User List: " +userList);
        return userList;
    }

    public static TreeMap<String, String> findTabPriv (Connection conn, String targetDB, TreeMap<String, String> users){
        TreeMap<String, String> tablePriv = new TreeMap();
        String tabPrivQuery = "SHOW GRANTS FOR ";
        ResultSet rs1;
        int tick=1;
        int secTick=1;
        try{
            Statement stmt = conn.createStatement();
            for(int ts =0; ts<=users.size()-1; ts++){
                while(users.get("User-" +tick)!= "null"){
                    rs1=stmt.executeQuery("SHOW GRANTS FOR '" +users.get("User-" +tick)+ "'@'" +users.get("Host-" +tick)+ "';");

                    while(rs1.next()){
                        tablePriv.put(users.get("User-" +tick)+ "-" +secTick, rs1.getString("Grants for " +users.get("User-" +tick)+ "@" +users.get("Host-" +tick)));
                        secTick++;
                    }
                    rs1.close();
                }
                tick++;
            }
            stmt.close();
            conn.close();
            
            System.out.println("Table Privileges: " +tablePriv);
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        return tablePriv;
    }
    
    public static void CreateTableCSV(ArrayList tables, TreeMap<String, String> columns, TreeMap<String, String> constraints){
        FileWriter fileOut = null;
        String filename="Tables.csv";
        try{
            fileOut = new FileWriter(filename);
            
            for(int ts=0; ts<tables.size()-1; ts++){
                int colnum=Integer.parseInt(columns.get(tables.get(ts)+"-Total"));
                fileOut.append("\n" +tables.get(ts)+ " Columns\n");
                fileOut.append("Column Name, Data Type, Default Value, Null Allowed, Key Type, Extra\n");
                
                for(int cs=1; cs<=colnum; cs++){
                    fileOut.append(columns.get(tables.get(ts)+ "-" +cs+ "-Field") +","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Type") +","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Default") +","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Null") +","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Key") +","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Extra") +"\n");
                }
                int constnum1=Integer.parseInt(constraints.get(tables.get(ts)+ "-Total"));
                fileOut.append("\n" +tables.get(ts)+ " Keys\nConstraint Name, Column Name, Referenced Column, Referenced Table\n");
                for(int cs=1; cs<= constnum1; cs++){
                    fileOut.append(constraints.get(tables.get(ts)+ "-" +cs+ "-Constraint Name")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Column Name")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Referenced Column")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Referenced Table")+ "\n");
                }
                int constnum2 = Integer.parseInt(constraints.get(tables.get(ts)+ "-Total2"));
                fileOut.append("\n"+ tables.get(ts)+ " Constraints\nConstraint Name, Constraint Type, Enforced, Constraint Schema, Constraint Table\n");
                for(int cs=1; cs<=constnum2; cs++){
                    fileOut.append(constraints.get(tables.get(ts)+ "-" +cs+ "-Constraint Name2")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Constraint Type")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Enforced")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Constraint Scema")+ "," +
                            constraints.get(tables.get(ts)+ "-" +cs+ "-Constraint Table")+ "\n");
                }
            }
        }catch(Exception e){
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        }finally {
             
            try{
                fileOut.flush();
                fileOut.close();
            }catch(IOException e){
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
}
