import java.awt.*;
import javax.swing.*;
import java.util.HashMap;
import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.filechooser.*;
import java.io.File;

public class SQLMapper {
    
    public static void main(String[] args) {
        getInfoGUI();
    }
    
    public static void getInfoGUI(){
        //Declare variables
        String incomError = "One or more fields contain no data.\nPlease review the form and verify all fields have been correctly filled.";
        String invalid = "There was an error processing your request.\nPlease reivew all fiels and verify information was entered corretly.";
        String intError = "An error occured while attempting to load the Draw.io web page. Please open your browser to navigate to \"https://app.diagrams.net\"";
        HashMap<String, String> connectInfo = new HashMap();
        
        //Declare frame and panels
        JFrame main = new JFrame("Relational Database Mapper");
        JFrame errFrame = new JFrame();
        JFrame fileFrame = new JFrame();
        JPanel mainP = new JPanel();
        JPanel top = new JPanel(new FlowLayout());
        JPanel topMid = new JPanel(new FlowLayout());
        JPanel bottomMid = new JPanel(new FlowLayout());
        JPanel bottom = new JPanel(new FlowLayout());
        
        //Declare Textfields
        JTextField host = new JTextField();
        host.setColumns(20);
        JTextField port = new JTextField();
        port.setColumns(20);
        JTextField database = new JTextField();
        database.setColumns(20);
        JTextField user = new JTextField();
        user.setColumns(20);
        JPasswordField pass = new JPasswordField();
        pass.setColumns(20);
        
        //Declare all buttons
        JButton cont = new JButton("Continue");
        JButton clear = new JButton("Clear");
        JButton page = new JButton("Draw.io");
        JRadioButton mysql = new JRadioButton("MySQL");
        JRadioButton sql = new JRadioButton("MS SQL");
        
        //Decalre button group
        ButtonGroup radios = new ButtonGroup();
        radios.add(mysql);
        radios.add(sql);
        
        //Declare labels
        JLabel lab1 = new JLabel("Hostname:");
        JLabel lab2 = new JLabel("Port:");
        JLabel lab3 = new JLabel("Database:");
        JLabel lab4 = new JLabel("Type:");
        JLabel lab5 = new JLabel("Username:");
        JLabel lab6 = new JLabel("Password:");
        
        //Assign labels to their components
        lab1.setLabelFor(host);
        lab2.setLabelFor(port);
        lab3.setLabelFor(database);
        lab5.setLabelFor(user);
        lab6.setLabelFor(pass);    
        
        //Add components to their panels
        top.add(lab1);
        top.add(host);
        top.add(lab2);
        top.add(port);
        topMid.add(lab3);
        topMid.add(database);
        topMid.add(lab4);
        topMid.add(mysql);
        topMid.add(sql);
        bottomMid.add(lab5);
        bottomMid.add(user);
        bottomMid.add(lab6);
        bottomMid.add(pass);
        bottom.add(cont);
        bottom.add(clear);
        bottom.add(page);
        
        //Add panels to JFrame
        mainP.add(top);
        mainP.add(topMid);
        mainP.add(bottomMid);
        mainP.add(bottom);
        main.getContentPane().add(mainP);
        
        //Set Frame properties
        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        main.setSize(650,200);
        main.setLocation(650,450);
        main.setVisible(true);
        
        //Action listener to map database
        cont.addActionListener(e->{
            String directory = "";
            JFileChooser folder = new JFileChooser();
            folder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int i = folder.showOpenDialog(fileFrame);
            if(i==JFileChooser.APPROVE_OPTION){
                File thisF = folder.getSelectedFile();
                directory = thisF.getPath();
            }else{

            }
            
            connectInfo.put("Database", database.getText());
            connectInfo.put("User", user.getText());
            connectInfo.put("Pass", pass.getText());
            if(mysql.isSelected()){
                String URL = "jdbc:mysql://" +host.getText()+ ":" +port.getText()+ "/" +database.getText();
                connectInfo.put("URL", URL);
                connectInfo.put("DBType", "mysql");
                MySQLMap mymap = new MySQLMap();
                mymap.MapDB(connectInfo, directory);
            }else if(sql.isSelected()){
                String URL = "jdbc:sqlserver://" +host.getText()+ ":" +port.getText()+ ";databaseName=" +database.getText()+ ";user=" +user.getText()+ ";password=" +pass.getText();
                connectInfo.put("URL", URL);
                connectInfo.put("DBType", "mssql");
                MSSQLMap msmap = new MSSQLMap();
                msmap.MapDB(connectInfo, directory);
            }else{
                IncompleteFormError error = new IncompleteFormError(incomError);
                JOptionPane.showMessageDialog(errFrame, error, "Incomplete Form", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        //Action listener to clear form
        clear.addActionListener(e->{
           host.setText("");
           port.setText("");
           database.setText("");
           user.setText("");
           pass.setText("");
           sql.setSelected(false);
           mysql.setSelected(false);
        });
        
        //Action listener to launch web browser to draw.io website
        page.addActionListener(e->{
            Desktop desk = java.awt.Desktop.getDesktop();
            try{
                URI thisURL = new URI("https://app.diagrams.net");
                desk.browse(thisURL);
            }catch(URISyntaxException uriError){
                JOptionPane.showMessageDialog(errFrame, uriError, "Web Browser Error", JOptionPane.ERROR_MESSAGE);
            }catch(IOException ioError){
                JOptionPane.showMessageDialog(errFrame, ioError, "Web Browser Error", JOptionPane.ERROR_MESSAGE);
            }           
        });
    }
}
