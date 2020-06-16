//BE ADVISED this portion of the application is not complete and has not been tested.

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

public class MSSQLMap {
    
    public static void MapDB(HashMap<String, String> connectInfo){
        ArrayList<String> tables = new ArrayList<String>();
        HashMap<String, String> dboptions = new HashMap();
        TreeMap<String, String> tableKeys = new TreeMap();
        TreeMap<String, String> dbcolumns = new TreeMap();
        TreeMap<String, String> rolePriveleges = new TreeMap();
        TreeMap<String, String> tableConstraints = new TreeMap();
        TreeMap<String, String> userList = new TreeMap();
        TreeMap<String, String> roles = new TreeMap();
        TreeMap<String, String> tablePriveleges = new TreeMap();
        
        Connection conn = dbconnect(connectInfo);
        tables = findTables(conn, connectInfo);
        dbcolumns = findColumns(conn, connectInfo, tables);
        tableKeys = findKeys(conn, connectInfo, tables);
        tableConstraints = findConst(conn, connectInfo, tables);
        //CreateTableCSV(connectInfo, tables, dbcolumns, tableKeys, tableConstraints);
        //DrawioCSV(connectInfo, tables, dbcolumns, tableKeys);
    }
    
    //Create connection to database
    public static Connection dbconnect(HashMap<String, String> connectInfo){
        Connection conn = null;
        
        try{
            System.out.println("Connecting to the server and database provided...");
            conn = DriverManager.getConnection(connectInfo.get("URL"));
            if(conn!=null){
                System.out.println("Database connection successful.");
            }else{
                System.out.println("Connection failed. Please verify the information provided");
            }
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        return conn;
    }
    
    public static ArrayList<String> findTables(Connection conn, HashMap<String, String> connectInfo){
        ArrayList<String> tables = new ArrayList<String>();
        String useDB = "USE " +connectInfo.get("Database") +";";
        String showTables = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;";
        
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);
            ResultSet rs1 = stmt.executeQuery(showTables);
            
            while(rs1.next()){
                tables.add(rs1.getString("TABLE_NAME"));
            }
            rs1.close();
            stmt.close();
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(tables);
        return tables;
    }
    
    public static TreeMap<String, String> findColumns(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        TreeMap<String, String> columns = new TreeMap();
        String useDB = "USE " +connectInfo.get("Database") +";";
        
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);
            
            for(int ts=0; ts<=tables.size()-1; ts++){
                ResultSet rs1 = stmt.executeQuery("SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" +tables.get(ts)+ "';");
                int tick = 1;
                
                while(rs1.next()){
                    columns.put(tables.get(ts)+ "-" +tick+ "-Name",  rs1.getString("COLUMN_NAME"));
                    columns.put(tables.get(ts)+ "-" +tick+ "-Type", rs1.getString("DATA_TYPE"));
                    columns.put(tables.get(ts)+ "-" +tick+ "-Default", rs1.getString("COLUMN_DEFAULT"));
                    columns.put(tables.get(ts)+ "-" +tick+ "-Null", rs1.getString("IS_NULLABLE"));
                    tick++;
                }
                columns.put(tables.get(ts)+ "-Total", Integer.toString(tick-1));
                rs1.close();
            }
            stmt.close();
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(columns);
        return columns;
    }
    
    public static TreeMap<String, String> findKeys(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        TreeMap<String, String> keys = new TreeMap();
        String keyQuery = "SELECT UNIQUE_CONSTRAINT_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS;";
        String useDB = "USE " +connectInfo.get("Database")+ ";";
        int tick =1;
        
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);
                ResultSet rs1 = stmt.executeQuery(keyQuery);
                while(rs1.next()){
                    String test = rs1.getString("UNIQUE_CONSTRAINT_NAME");
                    if(rs1.wasNull()){
                        continue;
                    }else{
                        String[] keyArray = rs1.getString("UNIQUE_CONSTRAINT_NAME").split("_");
                        keys.put(connectInfo.get("Database")+ tick, keyArray[1]);
                        tick++;
                    }
                }
                rs1.close();
                stmt.close();
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("Table Keys:" +keys);
        return keys;
    }
    
    public static TreeMap<String, String> findConst(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        TreeMap<String, String> constraints = new TreeMap();
        String constQuery1 = "SELECT COLUMN_NAME, CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE WHERE TABLE_NAME = '";
        String useDB= "USE " +connectInfo.get("Database")+ ";";
        int tick=1;
        int counter=1;
       
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);
            for (int ts=0; ts<=tables.size()-1;ts++){
                ResultSet rs1= stmt.executeQuery(constQuery1+ tables.get(ts)+ "';");
                
                while(rs1.next()){
                    String test =rs1.getString("CONSTRAINT_NAME");
                    if(rs1.wasNull()){
                    continue;
                    }else{
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Column_Name", rs1.getString("COLUMN_NAME"));
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Constraint_Name", rs1.getString("CONSTRAINT_NAME"));
                        tick++;
                    }
                }
                rs1.close();
                constraints.put(tables.get(ts)+ "-Total", Integer.toString(counter-1));
                counter=1;
            }
            stmt.close();
            System.out.println("Constraints:" +constraints);
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        return constraints;               
    }
    
    public static void CreateTableCSV(HashMap<String, String> connectInfo, ArrayList tables, TreeMap<String, String> columns, TreeMap<String, String> keys, TreeMap<String, String> constraints){
        FileWriter fileOut = null;
        int loopCount =1;
        int loopCount2 =1;
        String filename =connectInfo.get("Database")+ "Tables.csv";
        try{
            fileOut = new FileWriter(filename);
            
            for(int ts=0; ts<tables.size(); ts++){
                int colnum= Integer.parseInt(columns.get(tables.get(ts)+ "-Totl"));
                fileOut.append("\n" +tables.get(ts)+ "Columns\n");
                fileOut.append("Column_Name, Data_Type, Default_Value, Null_Allowed\n");
                
                for(int cs=1; cs<=colnum; cs++){
                    fileOut.append(columns.get(tables.get(ts)+ "-" +cs+ "-Name")+ ","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Type")+ ","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Default")+ ","+
                            columns.get(tables.get(ts)+ "-" +cs+ "-Null")+ "\n");
                }
                int constnum = Integer.parseInt(constraints.get(tables.get(ts)+ "-Total"));
                fileOut.append("\n" +tables.get(ts)+ " Constraints\nColumn_name, Constraint_Name\n");
                for (int cs=1; cs<= constnum; cs++){
                    fileOut.append(constraints.get(tables.get(ts)+ "-" +loopCount+ "-Column_Name")+ ","+
                            constraints.get(tables.get(ts)+ "-" +loopCount+ "-Constraint_Name")+ "\n");
                    loopCount++;
                }
            }
            
            fileOut.append("\n" +connectInfo.get("Database")+ "Referential Keys\nName");
            for(int cs=1; cs<=keys.size();cs++){
                fileOut.append(keys.get(connectInfo.get("Database")+ "-" +loopCount2)+ "\n");
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
