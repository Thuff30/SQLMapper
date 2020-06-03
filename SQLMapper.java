package sqlmapper;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.Collections;
import java.util.Scanner;
import java.io.IOException;

public class SQLMapper {
    
    
    public static void main(String[] args) {
       
        String host = "localhost";
        String port = "3306";
        String database = "lessonschedule";
        String MYSQL_URL = "jdbc:mysql://" +host+ ":" +port+ "/" +database;
        String user = "root";
        String pass = "Sjrhdu3485";
        String targetDB = "lessonschedule";
        String tablefile = "Tables";
        String usersfile = "Users and Roles";
        ArrayList<String> tables = new ArrayList<String>();

        HashMap<String, String> connectInfo = new HashMap();
        HashMap<String, String> dboptions = new HashMap();
        TreeMap<String, String> tableKeys = new TreeMap();
        TreeMap<String, String> dbcolumns = new TreeMap();
        TreeMap<String, String> rolePriveleges = new TreeMap();
        TreeMap<String, String> tableConstraints = new TreeMap();
        TreeMap<String, String> userList = new TreeMap();
        TreeMap<String, String> roles = new TreeMap();
        TreeMap<String, String> tablePriveleges = new TreeMap();
        
        connectInfo = getDBInfo();
        
        switch(connectInfo.get("DBType")){
            case "mysql":
                MySQLMap map = new MySQLMap();
                map.MapDB(connectInfo);
                break;
            case "mssql":
                break;
            case "oraclpe sql":
                break;
            
        }
 
    }
    
    //Get database information to establish connection
    public static HashMap<String, String> getDBInfo(){

        String dbType, URL, host, port, database, user, pass;
        HashMap<String, String> connectInfo = new HashMap();
        Scanner userIn = new Scanner(System.in);
        
        System.out.println("Is the databse MySQL, MSSQL, or Oracle SQL?");
        dbType = userIn.nextLine().toLowerCase();
        connectInfo.put("DBType", dbType);
        System.out.println("Enter the hostname for the database:");
        host=userIn.nextLine();
        System.out.println("Enter the correct connection port:");
        port=userIn.nextLine();
        System.out.println("Enter the name of a known database:");
        database=userIn.nextLine();
        connectInfo.put("Database", database);
        
        switch(dbType){
            case "mysql":
                URL = "jdbc:mysql://" +host+ ":" +port+ "/" +database;
                break;
            case "mssql":
                URL = "jdbc:sqlserver://" +host+ ":" +port+ ";databaseName=" +database;
                break;
            case "oracle sql":
                URL = "jdbc:oracle:thin:@" +host+ ":" +port+ ":" +database;
                break;
            default:
                URL = null;
                System.out.println("You have entered an invalid database type of " +dbType);
        }
        
        connectInfo.put("URL", URL);
        System.out.println("Enter an database administrative username: ");
        user=userIn.nextLine();
        connectInfo.put("User", user);
        System.out.println("Enter the password for " +user+ ":");
        pass=userIn.nextLine();
        connectInfo.put("Pass", pass);

        return connectInfo;
    }
        
}
