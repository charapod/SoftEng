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
import javax.sip.header.CallIdHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class BillingServer {

	protected class Call {
		private String caller;
		private String callId;
		private String callee;
		private String callerBillingStrategy;
		private RequestEvent callerRequestEvent;
		private CostCalculator costCalculator;
		
		public Call(String caller, String callId, String callee, RequestEvent requestEvent, String callerBillingStrategy) {
			this.caller     = caller;
			this.callId     = callId;
			this.callee     = callee;
			this.callerRequestEvent = requestEvent;
			this.callerBillingStrategy = callerBillingStrategy;
			this.costCalculator = BillingFactory.createCostCalculator(this.callerBillingStrategy);
		}
		
		public String getCallId() {
			return this.callId;
		}
		
		public String getCaller() {
			return this.caller;
		}
		
		public String getCallee() {
			return this.callee;
		}

		public void setTimeStart(long timeStart) {
			this.costCalculator.setTimeStart(timeStart);
		}
		
		public void printCall() {
			System.out.println("CallId : " + this.callId + ", Caller : " + this.caller + ", Callee : " + this.callee);
		}

		public RequestEvent getCallerRequestEvent() {
			return this.callerRequestEvent;
		}
		
		public String getCallerBillingStrategy() {
			return this.callerBillingStrategy;
		}

		public void setTimeEnd(long timeEnd) {
			this.costCalculator.setTimeEnd(timeEnd);
		}

		public long calculateCost() {
			return this.costCalculator.calculateCost();
		} 
	}
	
	private DBServer dbServer;
	private Proxy proxy;
	private ArrayList<Call> calls = null;
	private static String defaultStrategy;
	
	private static BillingServer pInstance;

	private BillingServer()
	{
		defaultStrategy = Constants.CHARGE_BY_RATE;
		calls           = new ArrayList<Call>();	
	}
	
	public static BillingServer getInstance()
	{
		if (pInstance == null)
			pInstance = new BillingServer();
		
		return pInstance;
	}
	
	public void init(DBServer dbServer, Proxy proxy)
	{
		this.dbServer = dbServer;
		this.proxy    = proxy;
	}
	
	public void callCreated(RequestEvent requestEvent)
	{ 
		String caller = getName(requestEvent.getRequest().getHeader("From"));
		String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
		String callee = getName(requestEvent.getRequest().getHeader("To"));
		String callerBillingStrategy = getCurrentStrategy(caller);
		System.out.println("Call created. Caller: " + caller 
				+ " has strategy: " + callerBillingStrategy);
		Call call = new Call(caller, callId, callee, requestEvent, callerBillingStrategy);
		calls.add(call);
		System.out.println("A new call was added");
		System.out.println("These are the active calls:");
		printCalls();
	}

	private void printCalls() {
		for (Call call: calls) {
			call.printCall();
		}
	}
	
	public void callStarted(RequestEvent requestEvent) {
		String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
		long timeStart = System.currentTimeMillis();
        for (Call call: calls) {
			if (call.getCallId().equals(callId)) {
				call.setTimeStart(timeStart);
				break;
			}
		}
	}
	
	public void callEnded(RequestEvent requestEvent)
	{
		String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
		long timeEnd = System.currentTimeMillis();
		String caller = "";
		Call callToRemove = null;
        for (Call call: calls) {
			if (call.getCallId().equals(callId)) {
				caller = call.getCaller();
				callToRemove = call;
				break;
			}
		}
        if (callToRemove == null) {
        	System.out.println("No call with that callId exists.");
        	return;
        }
        callToRemove.setTimeEnd(timeEnd);
		long bill = callToRemove.calculateCost();
		updateTotalBill(caller, bill);
		calls.remove(callToRemove);
	}

	private String getName(Header header)
	{
		String pattern = "<sip:([^;@]*)[@;]";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(header.toString());
		m.find();
		return m.group(1);
	}
	
	public void handleShowBillRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String billedUser = null;
		try {
			billedUser = getName(from);
		} catch (Exception e) {
			ProxyDebug.println("Could not get the user name from the request");
			return;
		}
		long totalBill = getTotalBill(billedUser);
		sendBillToUser(billedUser, requestEvent, totalBill);
	}
	
	private long getTotalBill(String billedUser)
	{
		String query = "Select * from bills where user = \"" + billedUser + "\";";
		ResultSet res = dbServer.execute(query);
		long billTotal = 0;
		try {
			if (res.first()) {
				billTotal = res.getLong("totalBill");
			}
		} catch (Exception e) {
			return 0L;
		}
		return billTotal;
	}

	private void updateTotalBill(String user, long bill) {
		String query = "Select * from bills where user = \"" + user + "\";";
		ResultSet res = dbServer.execute(query);
		try {
			if (res.first()) {
				query = "Update bills set totalBill = totalBill + \"" + bill + "\" where user = \"" + user + "\";";
				if (dbServer.executeUpdate(query) == -1)
					System.out.println("Update Query on bills Failed");
			}
			else {
				query = "Insert into bills values(\"" + user + 
					       "\", \"" + bill + "\");";
				if (dbServer.executeUpdate(query) == -1)
				System.out.println("Insert Query Failed");
			}	
		} catch (Exception e) {
			return;
		}
	}
	
	private void sendBillToUser(String billedUser, RequestEvent requestEvent, long bill)
	{
		System.out.println("Sending bill of " + bill + " euros to user: " + billedUser);
		
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

        // Get Bill Service header
        String headerName  = Constants.SERVICE;
        String headerValue = Constants.GET_BILL;
        Header serviceHeader;
		try {
			serviceHeader = headerFactory.createHeader(headerName, headerValue);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Service Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(serviceHeader);

        // Get Bill Content header
        String billHeaderName = Constants.BILL;
        String billString = Long.toString(bill);
        Header billHeader;
		try {
			billHeader = headerFactory.createHeader(billHeaderName, billString);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Bill Content Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(billHeader);	
		
		ServerTransaction serverTransaction = requestEvent.getServerTransaction();
		try {
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else
				sipProvider.sendResponse(response);
		} catch (SipException e) {
			ProxyDebug.println("Failed to send Bill response");
			e.printStackTrace();
		}
	}
	
	/**
	 * Handles a request to change the current billing strategy
	 * @param requestEvent
	 */
	public void handleChangeStrategyRequest(RequestEvent requestEvent) 
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String username = null;
		try {
			username = getName(from);
		} catch (Exception e) {
			return;
		}
		
		Header billHeader = request.getHeader(Constants.SERVICE);
		String strategy = billHeader.toString().trim().replace(Constants.SERVICE + ": ", "");
		
		changeStrategy(username, strategy);
		sendStrategy(requestEvent, strategy);
	}
	
	/**
	 * Changes the strategy for that user to the given strategy.
	 * @param username
	 * @param newStrategy
	 */
	private void changeStrategy(String username, String newStrategy)
	{
		removeStrategy(username);
		setStrategy(username, newStrategy);
	}
	
	/**
	 * Returns a response to the user informing him that this is his current billing strategy
	 * @param requestEvent
	 * @param strategy
	 */
	private void sendStrategy(RequestEvent requestEvent, String strategy)
	{
		System.out.println("Sending current strategy: " + strategy);
		
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

        // Current Strategy Service header
        String headerName  = Constants.SERVICE;
        String headerValue = Constants.CURRENT_STRATEGY;
        Header serviceHeader;
		try {
			serviceHeader = headerFactory.createHeader(headerName, headerValue);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Service Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(serviceHeader);

        // Current forwarding target header
        String currentStrategy = Constants.CURRENT_STRATEGY;
        Header currentStrategyHeader;
		try {
			currentStrategyHeader = headerFactory.createHeader(currentStrategy, strategy);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing a Message Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(currentStrategyHeader);	
		
		ServerTransaction serverTransaction = requestEvent.getServerTransaction();
		try {
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else
				sipProvider.sendResponse(response);
		} catch (SipException e) {
			ProxyDebug.println("Failed to send current target response");
			e.printStackTrace();
		}
	}

	/**
	 * Removes the strategy for that user if it exists
	 * @param username
	 */
	private boolean removeStrategy(String username)
	{
		String query = "Delete from BillingStrategies where username = \"" + username + "\";";
		if (dbServer.executeUpdate(query) == -1) {
			System.out.println("Delete Query Failed for BillingStrategies");
			return false;
		}
		return true;
	}

	/**
	 * Sets the strategy for the given user. 
	 * Assumes no strategy exists for that user.
	 * @param username
	 * @param newStrategy
	 */
	private boolean setStrategy(String username, String newStrategy)
	{
		String query = "Insert into BillingStrategies values(\"" + 
						username + "\", \"" + newStrategy + "\");";
		
		if (dbServer.executeUpdate(query) == -1) {
			System.out.println("Setting Billing Strategy Failed.");
			return false;
		}
		return true;
	}

	/**
	 * Returns the current billing strategy for the user.
	 * Inserts the default strategy to the database and returns it if none exists.
	 * @param username
	 */
	public String getCurrentStrategy(String username)
	{
		String query = "Select * from BillingStrategies where username = \"" + username + "\"";
		ResultSet res = dbServer.execute(query);
		String strategy = null;
		try {
			if (res.first())
				strategy = res.getString("strategy");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (strategy == null) { // no strategy exists
			setStrategy(username, defaultStrategy);
			return defaultStrategy;
		}

		return strategy;
	}

	/**
	 * Handles a request to get the current billing strategy.
	 * @param requestEvent
	 */
	public void handleCurrentStrategyRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String username = null;
		try {
			username = getName(from);
		} catch (Exception e) {
			return;
		}
		
		String strategy = getCurrentStrategy(username);
		sendStrategy(requestEvent, strategy);
	}
	
	public static String getDefaultStrategy()
	{
		return defaultStrategy;
	}
}
