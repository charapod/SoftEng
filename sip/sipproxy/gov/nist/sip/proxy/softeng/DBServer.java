package gov.nist.sip.proxy.softeng;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBServer {
	
	private Connection conn = null;
	private final String dbURL = "jdbc:mysql://localhost:3306/softeng";
	private final String userName = "SoftEng";
	private final String password = "pass";
	
	// TODO kostis 25-3-2015
	private static DBServer pInstance;
	
	private DBServer() {}
	
	public static DBServer getInstance()
	{
		if (pInstance == null) {
			pInstance = new DBServer();
			pInstance.init();
		}
		
		return pInstance;
	}
	
	private void init() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(dbURL, userName, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// TODO end
	
	public ResultSet execute(String query) {
		System.out.println("Querying:\n" + query);
		try {
			PreparedStatement stmt = conn.prepareStatement(query);
			return stmt.executeQuery();
		} catch (Exception e) {
			return null;
		}
	}
	
	public int executeUpdate(String query) {
		System.out.println("Querying:\n" + query);
		try {
			PreparedStatement stmt = conn.prepareStatement(query);
			return stmt.executeUpdate();
		} catch (Exception e) {
			return -1;
		}
	}

}
