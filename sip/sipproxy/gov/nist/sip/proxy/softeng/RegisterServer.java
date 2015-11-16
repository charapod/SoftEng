package gov.nist.sip.proxy.softeng;

import gov.nist.sip.proxy.Proxy;
import gov.nist.sip.proxy.ProxyDebug;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.header.Header;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class RegisterServer {
	private DBServer dbServer;
	private Proxy proxy;
	
	private static RegisterServer pInstance;

	private RegisterServer()
	{
		
	}
	
	public static RegisterServer getInstance()
	{
		if (pInstance == null)
			pInstance = new RegisterServer();
		
		return pInstance;
	}
	
	public void init(DBServer dbServer, Proxy proxy)
	{
		this.dbServer = dbServer;
		this.proxy    = proxy;
	}
	
	public void handleRegisterRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header user = request.getHeader("Username");
		Header pass = request.getHeader("Password");
		String username = null;
		String password = null;
		try {
			username = getValue(user, Constants.USERNAME);
			password = getValue(pass, Constants.PASSWORD);
		} catch (Exception e) {
			ProxyDebug.println("Could not get the username or password from the register request");
			return;
		}
		if (isUsernameInDB(username)) {
			System.out.println("Sending existingUsername response to user: " + username);
			sendExistingUsername(requestEvent);
			return;
		}
		registerUser(username, password);
		sendRegisterOk(requestEvent);
		System.out.println("Sending registerOk response to user: " + username);
	}

    private String getValue(Header header, String name)
    {
    	String headerStr = header.toString().trim();
		String value = headerStr.replace(name + ": ", "");
		return value;
    }
	
	private boolean registerUser(String username, String password) {
		String query = "Insert into RegisteredUsers values(\"" + username
				+ "\", \"" + password + "\");";
		if (dbServer.executeUpdate(query) == -1) {
			System.out.println("Registering User Failed.");
			return false;
		}
		return true;
	}

	private boolean isUsernameInDB(String username) {
		String query = "Select * from RegisteredUsers where username = \""
				+ username + "\"";
		ResultSet res = dbServer.execute(query);
		try {
			if (res.first())
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void sendRegisterOk(RequestEvent requestEvent) {
		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		Request request = requestEvent.getRequest();
		MessageFactory messageFactory = proxy.getMessageFactory();
		Response response;
		try {
			response = messageFactory.createResponse(Response.AMBIGUOUS, request);
		} catch (ParseException e) {
			ProxyDebug.println("Failed to create response");
			e.printStackTrace();
			return;
		}
		ServerTransaction serverTransaction = requestEvent.getServerTransaction();
		try {
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else
				sipProvider.sendResponse(response);
		} catch (SipException e) {
			ProxyDebug.println("Failed to send RegisterOk response");
			e.printStackTrace();
		}
	}
	
	public void sendBadCredentials(Request request, SipProvider sipProvider,
			ServerTransaction serverTransaction) {
		MessageFactory messageFactory = proxy.getMessageFactory();
		Response response;
		try {
			response = messageFactory.createResponse(Response.BAD_EXTENSION, request);
		} catch (ParseException e) {
			ProxyDebug.println("Failed to create response");
			e.printStackTrace();
			return;
		}
		try {
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else
				sipProvider.sendResponse(response);
		} catch (SipException e) {
			ProxyDebug.println("Failed to send RegisterOk response");
			e.printStackTrace();
		}
	}
	
	private void sendExistingUsername(RequestEvent requestEvent) {
		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		Request request = requestEvent.getRequest();
		MessageFactory messageFactory = proxy.getMessageFactory();
		Response response;
		try {
			response = messageFactory.createResponse(Response.BAD_EVENT, request);
		} catch (ParseException e) {
			ProxyDebug.println("Failed to create response");
			e.printStackTrace();
			return;
		}
		ServerTransaction serverTransaction = requestEvent.getServerTransaction();
		try {
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else
				sipProvider.sendResponse(response);
		} catch (SipException e) {
			ProxyDebug.println("Failed to send RegisterOk response");
			e.printStackTrace();
		}
	}

	public boolean areCredentialsValid(String username, String password) {
		String query = "Select * from RegisteredUsers where username = \""
				+ username + "\"";
		ResultSet res = dbServer.execute(query);
		String pass = "";
		try {
			if (res.first())
				pass = res.getString("password");
			else return false;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("DB pass = " + pass + ", Message pass = " + password);
		return (password.equals(pass));
	}
}
