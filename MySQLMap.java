import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.Collections;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;

public class MySQLMap {
    
    public static void MapDB(HashMap <String, String> connectInfo, String directory){
        ArrayList<String> tables = new ArrayList<String>();
        HashMap<String, String> dboptions = new HashMap();
        HashMap<String, String> tableKeys = new HashMap();
        HashMap<String, String> dbcolumns = new HashMap();
        HashMap<String, String> tableConstraints = new HashMap();
        String filename=directory+ "\\" +connectInfo.get("Database")+ "_DrawIOImport.csv";
        JFrame status = new JFrame("MS SQL Database");
        JFrame errFrame = new JFrame();
        JPanel top = new JPanel();
        JPanel button = new JPanel();
        JTextArea output = new JTextArea(600,600);
        
        JButton page = new JButton("Draw.io");
        top.add(output);
        button.add(page);
        status.setLayout(new BorderLayout());
        status.getContentPane().add(output, BorderLayout.CENTER);
        status.getContentPane().add(page, BorderLayout.SOUTH);
        status.setSize(1000,950);
        status.setLocation(425,50);
        status.setVisible(true);
        output.setEditable(false);
        
        page.addActionListener(e->{
            Desktop desk = java.awt.Desktop.getDesktop();
            try{
                URI thisURL = new URI("https://app.diagrams.net");
                desk.browse(thisURL);
            }catch(URISyntaxException uriError){
                JOptionPane.showMessageDialog(errFrame, uriError.getMessage(), "Web Browser Error", JOptionPane.ERROR_MESSAGE);
            }catch(IOException ioError){
                JOptionPane.showMessageDialog(errFrame, ioError.getMessage(), "Web Browser Error", JOptionPane.ERROR_MESSAGE);
            } 
        });
        
        output.append("Connecting to the server and database provided...\n");
        Connection conn = dbconnect(connectInfo);
        if(conn!= null){
            output.append("Connection to " +connectInfo.get("Database")+ " established...\n");

            output.append("Gatheting information on all tables...\n");
            tables = findTables(conn, connectInfo);
            
            output.append("Collecting a list of all columns...\n");
            dbcolumns = findColumns(conn, connectInfo, tables);
            
            output.append("Querying for table contraints...\n");
            tableKeys = findKeys(conn, connectInfo, tables);
            
            output.append("Writing CSV containing details of all tables...\n");
            tableConstraints = findConst(conn, connectInfo, tables);
            
            output.append("Creating CSV file for upload to draw.io...\n");
            CreateTableCSV(connectInfo, tables, dbcolumns, tableKeys, tableConstraints, directory);
            drawioCSV(connectInfo, tables, dbcolumns, tableKeys, directory);
            output.append("Database successfully mapped.\n Terminating connection...\n\n\n"+
                "Click on the Draw.io buttong below to https://www.draw.io/ and start a new diagram.\n" +
                "Select Arrange>Insert>Advanced>CSV... and copy and paste everything below this line then select Import\n\n");
            try{
                BufferedReader read = new BufferedReader(new FileReader(filename));
                String line;
                while((line = read.readLine()) != null){
                    output.append(line+ "\n");
                }
                read.close();
            }catch(IOException err){
                JOptionPane.showMessageDialog(errFrame, err.getMessage(), "File Reader Error", JOptionPane.ERROR_MESSAGE);
            }
            
        }else{
            output.append("Connection failed. Please review database information provided");
        }
    }
    
    //Create connection to database
    private static Connection dbconnect(HashMap<String, String> connectInfo){
        String JDBCDRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";
        Connection conn = null;
        JFrame errFrame = new JFrame();
        try{    
            //Create connection
            Class.forName(JDBCDRIVER_MYSQL);
            System.out.println("Connecting to the server and database provided");
            conn = DriverManager.getConnection(connectInfo.get("URL"), connectInfo.get("User"), connectInfo.get("Pass"));
            System.out.println("Database connection successful");
            
        }catch(SQLException se){
             JOptionPane.showMessageDialog(errFrame, "<html><body><p style='width: 200px;'>" +se.getMessage()+ "</p></body></html>", "Databse Connection Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, "<html><body><p style='width: 300px;'>" +e.getMessage()+ "</p></body></html>", "Database Connection Error", JOptionPane.ERROR_MESSAGE);
        }
        return conn;
    }
     
    //Find all tables on database
    private static ArrayList<String> findTables(Connection conn, HashMap<String, String> connectInfo){
        ArrayList<String> tables = new ArrayList<String>();
        String useDB = "USE " + connectInfo.get("Database")+ ";";
        String shTables = "SHOW TABLES;";
        JFrame errFrame = new JFrame();

            try{
                Statement stmt = conn.createStatement();
                stmt.executeQuery(useDB);
                ResultSet rs1 = stmt.executeQuery(shTables);

                while(rs1.next()){
                    tables.add(rs1.getString("Tables_in_" +connectInfo.get("Database")));
                }
                //Close all connections/statements to preserve resources
                rs1.close();
                stmt.close();
            }catch(SQLException se){
                JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
            }catch(Exception e){
                JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
            }
        return tables;
    }

    //Search for all tables in selected database    
    private static HashMap<String, String> findColumns(Connection conn, HashMap<String,String> connectInfo, ArrayList tables){
        HashMap<String, String> columns = new HashMap();
        String useDB = "USE " +connectInfo.get("Database")+ ";";
        JFrame errFrame = new JFrame();

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
            }catch(SQLException se){
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
            }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
            }
        return columns;
    }

    private static HashMap<String, String> findKeys(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        HashMap<String, String> keys = new HashMap();
        String keyQuery = "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME='";
        String useDB = "USE " +connectInfo.get("Database")+ ";";
        int tick =1;
        int counter;
        JFrame errFrame = new JFrame();
        
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);
            for(int ts =0; ts<=tables.size()-1; ts++){
                ResultSet rs1 = stmt.executeQuery(keyQuery+ tables.get(ts)+ "';" );
                counter=1;
                while(rs1.next()){
                    String test = rs1.getString("CONSTRAINT_NAME");
                    if(rs1.wasNull()){
                        continue;
                    }else{
                        keys.put(tables.get(ts)+ "-" +tick+ "-Constraint Name", rs1.getString("CONSTRAINT_NAME"));
                        keys.put(tables.get(ts)+ "-" +tick+ "-Column Name", rs1.getString("COLUMN_NAME"));
                        keys.put(tables.get(ts)+ "-" +tick+ "-Referenced Table", rs1.getString("REFERENCED_TABLE_NAME"));
                        keys.put(tables.get(ts)+ "-" +tick+ "-Referenced Column", rs1.getString("REFERENCED_COLUMN_NAME"));
                        tick++;
                        counter++;
                    }
                }
                rs1.close();
                keys.put(tables.get(ts)+ "-Total", Integer.toString(counter - 1));
                    }
        }catch(SQLException se){
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        return keys;
    }
    
    private static HashMap<String, String> findConst(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        HashMap<String, String> constraints = new HashMap();        
        String constQuery1 = "SELECT CONSTRAINT_SCHEMA, CONSTRAINT_NAME, TABLE_SCHEMA, CONSTRAINT_TYPE, ENFORCED FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='";
        String constQuery2= "' AND CONSTRAINT_TYPE != 'PRIMARY KEY' OR 'FOREIGN KEY';";
        String useDB = "USE " +connectInfo.get("Database")+ ";";
        int tick = 1;
        int counter = 1;
        JFrame errFrame = new JFrame();
        
        try{
            Statement stmt = conn.createStatement();
            stmt.executeQuery(useDB);
            
            for(int ts =0; ts<=tables.size()-1; ts++){
                ResultSet rs2= stmt.executeQuery(constQuery1+ tables.get(ts)+ constQuery2);

                while(rs2.next()){
                    String test = rs2.getString("CONSTRAINT_NAME");
                    if(rs2.wasNull()){
                        continue;
                    }else{
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Constraint Schema", rs2.getString("CONSTRAINT_SCHEMA"));
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Constraint Name2", rs2.getString("CONSTRAINT_NAME"));
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Table Schema", rs2.getString("TABLE_SCHEMA"));
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Constraint Type", rs2.getString("CONSTRAINT_TYPE"));
                        constraints.put(tables.get(ts)+ "-" +tick+ "-Enforced", rs2.getString("ENFORCED"));
                        tick++;
                        counter++;
                    }
                }
                rs2.close();
                constraints.put(tables.get(ts)+ "-Total", Integer.toString(counter - 1));
                counter=1;
            }
            stmt.close();
        }catch(SQLException se){
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        return constraints;
    }

    private static void CreateTableCSV(HashMap<String, String> connectInfo, ArrayList tables, HashMap<String, String> columns, HashMap<String, String> keys, HashMap<String, String> constraints, String directory){
        FileWriter fileOut = null;
        int loopCount=1;
        int loopCount2=1;
        String filename= directory+ "\\" +connectInfo.get("Database")+ "_Tables.csv";
        JFrame errFrame = new JFrame();
        
        try{
            fileOut = new FileWriter(filename);
            
            for(int ts=0; ts<tables.size(); ts++){
                
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
                int constnum1=Integer.parseInt(keys.get(tables.get(ts)+ "-Total"));
                System.out.println(constnum1);
                fileOut.append("\n" +tables.get(ts)+ " Keys\nConstraint Name, Column Name, Referenced Column, Referenced Table\n");
                for(int cs=1; cs<= constnum1; cs++){
                    fileOut.append(keys.get(tables.get(ts)+ "-" +loopCount+ "-Constraint Name")+ "," +
                            keys.get(tables.get(ts)+ "-" +loopCount+ "-Column Name")+ "," +
                            keys.get(tables.get(ts)+ "-" +loopCount+ "-Referenced Column")+ "," +
                            keys.get(tables.get(ts)+ "-" +loopCount+ "-Referenced Table")+ "\n");
                    loopCount++;
                }
                int constnum2 = Integer.parseInt(constraints.get(tables.get(ts)+ "-Total"));
                System.out.println(constnum2);
                fileOut.append("\n"+ tables.get(ts)+ " Constraints\nConstraint Name, Constraint Type, Enforced, Constraint Schema, Constraint Table\n");
                for(int cs=1; cs<=constnum2; cs++){
                    fileOut.append(constraints.get(tables.get(ts)+ "-" +loopCount2+ "-Constraint Name2")+ "," +
                            constraints.get(tables.get(ts)+ "-" +loopCount2+ "-Constraint Type")+ "," +
                            constraints.get(tables.get(ts)+ "-" +loopCount2+ "-Enforced")+ "," +
                            constraints.get(tables.get(ts)+ "-" +loopCount2+ "-Constraint Scema")+ "," +
                            constraints.get(tables.get(ts)+ "-" +loopCount2+ "-Constraint Table")+ "\n");
                    loopCount2++;
                }
            }
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }finally {
             
            try{
                fileOut.flush();
                fileOut.close();
            }catch(IOException e){
                JOptionPane.showMessageDialog(errFrame, e.getMessage(), "File Writer Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private static void drawioCSV(HashMap<String, String> connectInfo, ArrayList tables, HashMap<String, String> columns, HashMap<String, String> keys, String directory){
        int loopCount=1;
        FileWriter fileOut = null;
        JFrame errFrame = new JFrame();
        String filename=directory+ "\\" +connectInfo.get("Database")+ "_DrawIOImport.csv";
        try{
            fileOut = new FileWriter(filename);
            fileOut.append("##shipping export\n" +
                    "##\n" +
                    "## Go to https://www.draw.io/ and start a new diagram. Select Arrange>Insert>Advanced>CSV... and copy and paste everything below this line then select Import\n" +
                    "##\n" +
                    "# label: <div style=\"alignContent:space-between;\"><b style=\"font-size:x-large; border-bottom-color:black; border-bottom-style:solid;\">%Table%</b><br><br>%ColumnName%" +
                    "<br>%ColumnName2%<br>%ColumnName3%<br>%ColumnName4%<br>%ColumnName5%<br>%ColumnName6%<br>%ColumnName7%<br>%ColumnName8%</div>\n" +
                    "# style: label;shape=rectangle;whiteSpace=wrap;html=1;rounded=1;\n" +
                    "# parentstyle: swimlane;whiteSpace=wrap;html=1;childLayout=stackLayout;horizontal=1;horizontalStack=0;resizeParent=1;resizeLast=0;collapsible=1;\n" +
                    "# connect: {\"from\": \"ReferencedTable\", \"to\": \"Table\", \"invert\": true, \"style\": \"rounded=1;endArrow=blockThin;endFill=1; fontSize=11;\"}\n" +
                    "# width: auto\n" +
                    "# height: 180\n" +
                    "#padding: 30\n" +
                    "# nodespacing: 40\n" +
                    "# levelspacing: 100\n" +
                    "# edgespacing: 40\n" +
                    "# layout: verticalflow\n" +
                    "## ---- CSV below this line. First line is column names. ----\n" +
                    "Table,ColumnName,ColumnName2,ColumnName3,ColumnName4,ColumnName5,ColumnName6,ColumnName7,ColumnName8,ReferencedTable\n");
            
            for(int ts=0; ts<tables.size(); ts++){
                int colnum=Integer.parseInt(columns.get(tables.get(ts)+"-Total"));                
                fileOut.append(tables.get(ts)+",");
                int commacount=0;
                for(int cs=1 ; cs<=colnum && cs<=8 ; cs++){
                    fileOut.append(columns.get(tables.get(ts)+ "-" +cs+ "-Field") +",");
                commacount++;
                }
                
                if(commacount<8){
                    for(int cc =commacount; cc<8 ;cc++){
                        fileOut.append(",");
                    }
                }
                
                int constnum1=Integer.parseInt(keys.get(tables.get(ts)+ "-Total"));
                ArrayList<String> reference = new ArrayList<String>();
                String fullRef = "";
                
                for(int cs=1; cs<= constnum1; cs++){
                        reference.add(keys.get(tables.get(ts)+ "-" +loopCount+ "-Referenced Table"));
                        loopCount++;                                            
                }
                System.out.println(reference);
                for(int as=1; as<=reference.size()-1; as++){
                    fullRef=fullRef + reference.get(as) + ",";
                }
                    fileOut.append("\"" +fullRef+ "\",\n");
            }
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }finally{
            try{
                fileOut.flush();
                
                
                fileOut.close();
            }catch(IOException e){
                JOptionPane.showMessageDialog(errFrame, e.getMessage(), "File Writer Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
