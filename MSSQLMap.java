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

public class MSSQLMap {
    
    MSSQLMap(){
        
}
    public static void MapDB(HashMap<String, String> connectInfo, String directory){
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
            
            output.append("Noting table keys...\n");
            tableKeys = findKeys(conn, connectInfo, tables);
            
            output.append("Querying for table contraints...\n");
            tableConstraints = findConst(conn, connectInfo, tables);
            
            output.append("Writing CSV containing details of all tables...\n");
            CreateTableCSV(connectInfo, tables, dbcolumns, tableKeys, tableConstraints, directory);
            
            output.append("Creating CSV file for upload to draw.io...\n");
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
        Connection conn = null;
        JFrame errFrame = new JFrame();
        try{
            conn = DriverManager.getConnection(connectInfo.get("URL"));
        }catch(SQLException se){
             JOptionPane.showMessageDialog(errFrame, "<html><body><p style='width: 200px;'>" +se.getMessage()+ "</p></body></html>", "Databse Connection Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, "<html><body><p style='width: 300px;'>" +e.getMessage()+ "</p></body></html>", "Database Connection Error", JOptionPane.ERROR_MESSAGE);
        }
        return conn;
    }
    
    private static ArrayList<String> findTables(Connection conn, HashMap<String, String> connectInfo){
        ArrayList<String> tables = new ArrayList();
        String showTables = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME != 'sysdiagrams';";
        JFrame errFrame = new JFrame();
        
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs1 = stmt.executeQuery(showTables);
            
            while(rs1.next()){
                tables.add(rs1.getString("TABLE_NAME"));
            }
            rs1.close();
            stmt.close();
        }catch(SQLException se){
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        return tables;
    }
    
    private static HashMap<String, String> findColumns(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        HashMap<String, String> columns = new HashMap();
        JFrame errFrame = new JFrame();
        
        try{
            Statement stmt = conn.createStatement();
            
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
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        return columns;
    }
    
    private static HashMap<String, String> findKeys(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        HashMap<String, String> keys = new HashMap();
        String keyQuery = "SELECT T1.TABLE_NAME [PKTABLE_NAME], KCU.COLUMN_NAME [PKCOLUMN_NAME], T2.TABLE_NAME [FKTABLE_NAME], KCU2.COLUMN_NAME [FKCOLUMN_NAME] " +
            "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T1 INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU ON T1.CONSTRAINT_SCHEMA = KCU.CONSTRAINT_SCHEMA \n" +
            "AND T1.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME INNER JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RC ON T1.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA \n" +
            "AND T1.CONSTRAINT_NAME = RC.CONSTRAINT_NAME INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS T2 ON RC.UNIQUE_CONSTRAINT_SCHEMA = T2.CONSTRAINT_SCHEMA \n" +
            "AND RC.UNIQUE_CONSTRAINT_NAME = T2.CONSTRAINT_NAME INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU2 ON T2.CONSTRAINT_SCHEMA = KCU2.CONSTRAINT_SCHEMA \n" +
            "AND T2.CONSTRAINT_NAME = KCU2.CONSTRAINT_NAME AND KCU.ORDINAL_POSITION = KCU2.ORDINAL_POSITION WHERE  T1.CONSTRAINT_TYPE = 'FOREIGN KEY' AND T1.TABLE_NAME = '";
        int tick =1;
        JFrame errFrame = new JFrame();
        
        try{
            Statement stmt = conn.createStatement();
            for(int ts=0; ts<=tables.size()-1; ts++){
                ResultSet rs1 = stmt.executeQuery(keyQuery +tables.get(ts)+ "';");
                while(rs1.next()){
                    String test = rs1.getString("PKTABLE_NAME");
                    if(rs1.wasNull()){
                        continue;
                    }else{
                        keys.put(tables.get(ts)+ "-" +tick+ "-ForeignTable", rs1.getString("FKTABLE_NAME"));
                        keys.put(tables.get(ts)+ "-" +tick+ "-ForeignColumn", rs1.getString("FKCOLUMN_NAME"));
                        keys.put(tables.get(ts)+ "-" +tick+ "-PrimaryTable", rs1.getString("PKTABLE_NAME"));
                        keys.put(tables.get(ts)+ "-" +tick+ "-PrimaryColumn", rs1.getString("PKCOLUMN_NAME"));
                        tick++;
                    }
                }
                rs1.close();
                keys.put(tables.get(ts)+ "-Total", Integer.toString(tick-1));
            }
            stmt.close();
        }catch(SQLException se){
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        return keys;
    }
    
    private static HashMap<String, String> findConst(Connection conn, HashMap<String, String> connectInfo, ArrayList tables){
        HashMap<String, String> constraints = new HashMap();
        String constQuery1 = "SELECT COLUMN_NAME, CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE WHERE TABLE_NAME = '";
        int tick=1;
        int counter=1;
        JFrame errFrame = new JFrame();
       
        try{
            Statement stmt = conn.createStatement();
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
        }catch(SQLException se){
            JOptionPane.showMessageDialog(errFrame, se.getMessage(), "SQL Exception Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception e){
            JOptionPane.showMessageDialog(errFrame, e.getMessage(), "An Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        return constraints;               
    }
    
    private static void CreateTableCSV(HashMap<String, String> connectInfo, ArrayList tables, HashMap<String, String> columns, HashMap<String, String> keys, HashMap<String, String> constraints, String directory){
        FileWriter fileOut = null;
        int loopCount =1;
        int loopCount2 =1;
        String filename= directory+ "\\" +connectInfo.get("Database")+ "_Tables.csv";
        JFrame errFrame = new JFrame();
        
        try{
            fileOut = new FileWriter(filename);
            
            for(int ts=0; ts<tables.size(); ts++){
                int colnum = Integer.parseInt(columns.get(tables.get(ts)+ "-Total"));
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
                int keysSize = Integer.parseInt(keys.get(tables.get(ts)+ "-Total"));
                fileOut.append("\n" +tables.get(ts)+ " Referential Keys\nColumn,Referenced Table,Referenced Column\n");
                
                for(int cs=1; cs<=keysSize; cs++){
                    fileOut.append(keys.get(tables.get(ts)+ "-" +loopCount2+ "-PrimaryColumn")+ ","+
                            keys.get(tables.get(ts)+ "-" +loopCount2+ "-ForeignTable")+ ","+
                            keys.get(tables.get(ts)+ "-" +loopCount2+ "-ForeignColumn")+ "\n");
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
        FileWriter fileOut = null;
        JFrame errFrame = new JFrame();
        String filename=directory+ "\\" +connectInfo.get("Database")+ "_DrawIOImport.csv";
        String fileHeader = "##shipping export\n" +
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
                    "Table,ColumnName,ColumnName2,ColumnName3,ColumnName4,ColumnName5,ColumnName6,ColumnName7,ColumnName8,ReferencedTable\n";
        
        try{
            fileOut = new FileWriter(filename);
            fileOut.append(fileHeader);
            
            for(int ts=0; ts<tables.size(); ts++){
                int colnum=Integer.parseInt(columns.get(tables.get(ts)+"-Total"));                
                fileOut.append(tables.get(ts)+",");
                int commacount=0;
                
                for(int cs=1 ; cs<=colnum && cs<=8 ; cs++){
                    fileOut.append(columns.get(tables.get(ts)+ "-" +cs+ "-Name") +",");
                commacount++;
                }
                
                if(commacount<8){
                    for(int cc =commacount; cc<8 ;cc++){
                        fileOut.append(",");
                    }
                }
                
                int keysSize = Integer.parseInt(keys.get(tables.get(ts)+ "-Total"));
                ArrayList<String> reference = new ArrayList<String>();
                String fullRef = "";
                
                for(int i=1; i<= keysSize; i++){
                        reference.add(keys.get(tables.get(ts)+ "-" +i+ "-ForeignTable"));
                }
                for(int as=0; as<reference.size(); as++){
                    fullRef=fullRef + reference.get(as) + ",";
                }
                    fileOut.append("\"" +fullRef+ "\",\n");
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
}
