import java.util.HashMap;
import java.util.Scanner;
import java.io.IOException;

public class SQLMapper {
       
    public static void main(String[] args) {

        HashMap<String, String> connectInfo = new HashMap();
        
        connectInfo = getDBInfo();
        
        switch(connectInfo.get("DBType")){
            case "mysql":
                MySQLMap mymap = new MySQLMap();
                mymap.MapDB(connectInfo);
                break;
            case "mssql":
                MSSQLMap msmap = new MSSQLMap();
                msmap.MapDB(connectInfo);
                break;          
        } 
    }
    
    //Get database information to establish connection
    public static HashMap<String, String> getDBInfo(){

        String dbType, URL, host, port, database, user, pass;
        HashMap<String, String> connectInfo = new HashMap();
        Scanner userIn = new Scanner(System.in);
        
        System.out.println("Is the databse MySQL or MSSQL?");
        dbType = userIn.nextLine().toLowerCase();
        connectInfo.put("DBType", dbType);
        System.out.println("Enter the hostname for the database:");
        host=userIn.nextLine();
        System.out.println("Enter the correct connection port:");
        port=userIn.nextLine();
        System.out.println("Enter the name of a known database:");
        database=userIn.nextLine();
        connectInfo.put("Database", database);    
        System.out.println("Enter an database administrative username: ");
        user=userIn.nextLine();
        connectInfo.put("User", user);
        System.out.println("Enter the password for " +user+ ":");
        pass=userIn.nextLine();
        connectInfo.put("Pass", pass);
        
        switch(dbType){
            case "mysql":
                URL = "jdbc:mysql://" +host+ ":" +port+ "/" +database;
                break;
            case "mssql":
                URL = "jdbc:sqlserver://" +host+ ":" +port+ ";databaseName=" +database+ ";user=" +user+ ";password=" +pass;
                break;
            default:
                URL = null;
                System.out.println("You have entered an invalid database type of " +dbType);
        }       
        connectInfo.put("URL", URL);

        return connectInfo;
    }        
}
