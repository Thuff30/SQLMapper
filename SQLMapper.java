package sqlmapper;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.Collections;




public class SQLMapper {
    
    
    
    public static void main(String[] args) {
       
        String host = "localhost";
        String database = "shipping";
        String MYSQL_URL = "jdbc:mysql://" +host+ ":3306/" +database;
        String user = "root";
        String pass = "Sjrhdu3485";
        String targetDB = "";
        TreeMap<String, TreeMap> dbcolumns = new TreeMap();
        HashMap totalColumns= new HashMap();
        
        Connection conn = dbconnect(MYSQL_URL, user, pass);
        HashMap<String, String> dboptions = findDatabases(targetDB, conn);
        System.out.println(dboptions);
        dbcolumns = findColumns(conn,dboptions);
        
    }
    
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
                }
                
                for(int i=1;i<=numdb;i++){
                    String line = "DB" +i;
                    String thisLine=dbList.get(line);
                    if(thisLine.equalsIgnoreCase("information_schema") || thisLine.equalsIgnoreCase("performance_schema") || 
                            thisLine.equalsIgnoreCase("mysql") || thisLine.equalsIgnoreCase("sys")){
                        dbList.remove(line);
                    }
                }
            }catch(SQLException se){
                se.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            }
            //Remove all system databases from the list
            
            return dbList;
        }
        
            //Search for all tables in selected database
        public static TreeMap<String, TreeMap> findColumns(Connection conn, HashMap databases){
            TreeMap<String, TreeMap> allcolumns = new TreeMap();
            TreeMap<String, String> columns = new TreeMap();
            String shTables = "SHOW TABLES;";
            Iterator it = databases.entrySet().iterator();
            
            while(it.hasNext()){
                try{
                    HashMap.Entry element=(HashMap.Entry)it.next();
                    String line = (String)element.getValue();
                    String useDB = "USE " +line+ ";";
                    System.out.println(useDB);
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery(useDB);
                    ResultSet rs1 = stmt.executeQuery(shTables);
                    
                    //determine the size of the result set and create a matching array
                    rs1.last();
                    int rows = rs1.getRow();
                    rs1.beforeFirst();
                    int t =0;
                    String[] tables = new String[rows];
                    
                    while(rs1.next()){
                        tables[t] = rs1.getString("Tables_in_" +line);
                        System.out.println("|Table " + t + " | " + tables[t] + "|");
                        t++;
                    }
                    //Close all connections/statements to preserve resources
                    rs1.close();
                    stmt.close();


                    stmt = conn.createStatement();
                    //Loop through all tables in the database
                    //declare/reset integer to hold number of columns
                    int tick = 1;
                    for(int ts =0; ts<=tables.length-1; ts++){  
                        ResultSet rs2 = stmt.executeQuery("DESCRIBE " +tables[ts]);

                        //Loop through all columns in the table and store
                        while(rs2.next()){
                            columns.put(tables[ts] + "-Field-" +tick, rs2.getString("Field"));
                            columns.put(tables[ts] + "-Type-" +tick,rs2.getString("Type"));
                            columns.put(tables[ts] + "-Null-" +tick, rs2.getString("Null"));
                            columns.put(tables[ts] + "-Key-" +tick, rs2.getString("Key"));
                            columns.put(tables[ts] + "-Default-" +tick, rs2.getString("Default"));
                            columns.put(tables[ts] + "-Extra-" +tick, rs2.getString("Extra"));
                            tick++;
                        }

                        //Store the total number of columns for the table for later refference
                        allcolumns.put(line+ "." +tables[ts], columns);

                         rs2.close();
                    }
                   stmt.close();
                        System.out.println(columns);
                        System.out.println();


                    conn.close();
                }catch(SQLException se){
                    se.printStackTrace();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
