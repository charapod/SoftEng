package gov.nist.sip.proxy.softeng;

import gov.nist.sip.proxy.Proxy;
import gov.nist.sip.proxy.ProxyDebug;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class BlockingServer {
	
	private DBServer dbServer;
	private Proxy    proxy;
	
	// TODO kostis 25-3-2015
	private static BlockingServer pInstance;

	private BlockingServer() {}
	
	public static BlockingServer getInstance()
	{
		if (pInstance == null)
			pInstance = new BlockingServer();
		
		return pInstance;
	}
	
	public void init(DBServer dbServer, Proxy proxy)
	{
		this.dbServer = dbServer;
		this.proxy    = proxy;
	}
	// TODO end
	
	private boolean isBlocked(String caller, String callee) 
	{
		String query = "Select * from blocks where blocker = \"" + callee + 
					   "\" and blocked = \"" + caller + "\";";
		ResultSet res = dbServer.execute(query);
		boolean blocked = false;
		try {
			blocked = res.first();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return blocked;
	}
	
	public boolean isBlocked(Request request)
	{
		Header from = request.getHeader("From");
		Header to   = request.getHeader("To");
		String caller = null;
		String callee = null;
		try {
			caller = getName(from);
			callee = getName(to);
		} catch (Exception e) {
			return true;
		}
		return isBlocked(caller, callee);
	}

	private String getName(Header header)
	{
		String pattern = "<sip:([^;@]*)[@;]";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(header.toString());
		m.find();
		return m.group(1);
	}

	private void blockUser(String blocker, String blocked)
	{
		String query = "Insert into blocks values(\"" + blocker + 
				       "\", \"" + blocked + "\");";
		if (dbServer.executeUpdate(query) == -1)
			System.out.println("Insert Query Failed");
	}
	
	public void handleBlockRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		Header to   = request.getHeader("To");
		String blocker = null;
		String blocked = null;
		try {
			blocker = getName(from);
			blocked = getName(to);
		} catch (Exception e) {
			return;
		}
		blockUser(blocker, blocked);
		sendBlockingList(blocker, requestEvent);
	}
	
	private void sendBlockingList(String blocker, RequestEvent requestEvent)
	{
		// Construct the blocked users list string
		ArrayList<String> blockedUsers = getBlockedList(blocker);
		String blockedList = "";
		for (String user : blockedUsers)
			blockedList += user + "%";
		if (!blockedList.equals("")) // remove the ;; at the end
			blockedList = blockedList.substring(0, blockedList.length() -1);
		
		System.out.println("Sending blocking list: " + blockedList);
		
		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		Request request = requestEvent.getRequest();
		HeaderFactory headerFactory = proxy.getHeaderFactory();
		MessageFactory messageFactory = proxy.getMessageFactory();
		Response response;
		try {
			response = messageFactory.createResponse(Response.OK, request);
		} catch (ParseException e) {
			ProxyDebug.println("Failed to create response");
			e.printStackTrace();
			return;
		}

        // Blocking List Service header
        String headerName  = Constants.SERVICE;
        String headerValue = Constants.GET_BLOCKED_LIST;
        Header serviceHeader;
		try {
			serviceHeader = headerFactory.createHeader(headerName, headerValue);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Service Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(serviceHeader);

        // Blocking List Content header
        String blockedListName = Constants.BLOCKED_LIST;
        Header blockedListHeader;
		try {
			blockedListHeader = headerFactory.createHeader(blockedListName, blockedList);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Service Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(blockedListHeader);	
		
		ServerTransaction serverTransaction = requestEvent.getServerTransaction();
		try {
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else
				sipProvider.sendResponse(response);
		} catch (SipException e) {
			ProxyDebug.println("Failed to send blockingList response");
			e.printStackTrace();
		}
	}

	private void unblockUser(String blocker, String blocked)
	{
		String query = "delete from blocks where blocker = \"" + blocker + 
			           "\" and blocked = \"" + blocked + "\";";
		if (dbServer.executeUpdate(query) == -1)
			System.out.println("Delete Query Failed.");
	}

	public void handleUnblockRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		Header to   = request.getHeader("To");
		String blocker = null;
		String blocked = null;
		try {
			blocker = getName(from);
			blocked = getName(to);
		} catch (Exception e) {
			return;
		}
		unblockUser(blocker, blocked);
		sendBlockingList(blocker, requestEvent);
	}
	
	public void handleBlockedListRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String blocker = null;
		try {
			blocker = getName(from);
		} catch (Exception e) {
			ProxyDebug.println("Could not get the user name from the request");
			return;
		}
		sendBlockingList(blocker, requestEvent);
	}
	
	private ArrayList<String> getBlockedList(String blocker)
	{
		String query = "Select blocked from blocks where blocker = \"" + blocker + "\";";
		ResultSet res = dbServer.execute(query);
		ArrayList<String> results = new ArrayList<String>();
		try {
			while (res.next()) {
				String blocked = res.getString("blocked");
				results.add(blocked);
			}
		} catch (Exception e) {
			return null;
		}
		return results;
	}
}
