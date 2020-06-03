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
    }
}
