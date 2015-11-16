package gov.nist.sip.proxy.softeng;

import gov.nist.sip.proxy.Configuration;
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
import javax.sip.address.URI;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;


public class ForwardingServer
{
	// TODO kostis 25-3-2015
	private DBServer dbServer;
	private Proxy    proxy;
	
	private static ForwardingServer pInstance;
	
	private ForwardingServer() {}
	
	public static ForwardingServer getInstance()
	{
		if (pInstance == null)
			pInstance = new ForwardingServer();
		
		return pInstance;
	}
	
	public void init(DBServer dbServer, Proxy proxy)
	{
		this.dbServer = dbServer;
		this.proxy    = proxy;
	}
	
	/*
	 * Returns the name of the immediate target the callee is forwarding to,
	 * null if he does not exist.
	 */
	private String getImmediateForward(String callee)
	{
		String target = null;
		
		String query = "Select * from forwards where forwarder = \"" + callee + "\";";
		ResultSet res = dbServer.execute(query);
		try {
			if (res.first())
				target = res.getString("forwardTarget");
			res.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return target;
	}
	// TODO end

	private String getFinalCallee(String callee)
	{
		String query = "Select * from forwards where forwarder = \"" + callee + "\";";
		ResultSet res = dbServer.execute(query);
		// loop until the final callee does not forward to any user himself
		try {
			while (res.first()) {
				callee = res.getString("forwardTarget");
				query = "select * from forwards where forwarder = \""
						+ callee + "\";";
				res.close();
				res = dbServer.execute(query);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return callee;
	}
	
	public String getFinalCallee(Request request)
	{
		Header to     = request.getHeader("To");
		String callee = getName(to);
		return getFinalCallee(callee);
	}

	private String getName(Header header)
	{
		String pattern = "<sip:([^;@]*)[@;]";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(header.toString());
		m.find();
		return m.group(1);
	}

	// TODO kostis 25-3-2015
	private boolean checkForCircles(String forwarder, String forwardTarget)
	{
		if (forwarder == null || forwardTarget == null)
			return false;
		
		while (forwardTarget != null) {
			if (forwarder.equals(forwardTarget))
				return false;

			forwardTarget = getImmediateForward(forwardTarget);
		}
		
		return true;
	}
	
	// TODO end

	// TODO kostis 24-1-2015
	private boolean forwardToUser(String forwarder, String forwardTarget) 
	{
		if (!checkForCircles(forwarder, forwardTarget)) {
			System.out.println("Cycle detected!");
			// TODO kostis 25-3-2015
			unForwardUser(forwarder);
			// TODO end
			return false;
		}

		// delete the previous forwarding entry (if any) for the forwarder
		// of this request
		String query = "Select * from forwards where forwarder = \"" + forwarder + "\";";
		ResultSet res = dbServer.execute(query);
		try {
			if (res.first()) {
				query = "delete from forwards where forwarder = \""
						+ forwarder + "\";";
				if (dbServer.executeUpdate(query) == -1) {
					System.out.println("Delete Query Failed.");
					return false;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		// insert the new forwarding entry
		query = "Insert into forwards values(\"" + forwarder + "\", \""
				+ forwardTarget + "\");";
		if (dbServer.executeUpdate(query) == -1) {
			System.out.println("Insert Query Failed.");
			return false;
		}

		return true;
	}

	public void handleForwardRequest(RequestEvent requestEvent) 
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		Header to   = request.getHeader("To");
		String forwarder = null;
		String forwardTarget = null;
		try {
			forwarder = getName(from);
			forwardTarget = getName(to);
		} catch (Exception e) {
			return;
		}
		
		if (forwardToUser(forwarder, forwardTarget))
			sendForwardTarget(forwardTarget, requestEvent);
		else
			sendForwardTarget("", requestEvent);
	}
	
	private void unForwardUser(String forwarder)
	{
		String query = "delete from forwards where forwarder = \"" + forwarder + "\"";
		if (dbServer.executeUpdate(query) == -1)
			System.out.println("Delete Query Failed");
	}

	public void handleUnforwardRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String forwarder = getName(from);
		unForwardUser(forwarder);
		sendForwardTarget("", requestEvent);
	}
	
	public void handleForwardQuestionRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String forwarder = null;
		forwarder = getName(from);
		String forwardTarget = findForwardTarget(forwarder);
		sendForwardTarget(forwardTarget, requestEvent);
	}

	private void sendForwardTarget(String forwardTarget, RequestEvent requestEvent)
	{
		System.out.println("Sending current forward target: " + forwardTarget);
		
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

        // Current Forward Service header
        String headerName  = Constants.SERVICE;
        String headerValue = Constants.GET_CURRENT_FORWARD;
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
        String currentTarget = Constants.CURRENT_TARGET;
        Header currentTargetHeader;
		try {
			currentTargetHeader = headerFactory.createHeader(currentTarget, forwardTarget);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Service Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(currentTargetHeader);	
		
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

	private String findForwardTarget(String forwarder)
	{
		String query = "Select * from forwards where forwarder = \""
				+ forwarder + "\";";
		ResultSet res = dbServer.execute(query);
		String forwardTarget = "";
		try {
			if (res.first()) {
				forwardTarget = res.getString("forwardTarget");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return forwardTarget;
	}

	public boolean applyForwarding(RequestEvent requestEvent) {
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String caller = null;
		String forwardTarget = getFinalCallee(request);
		try {
			caller = getName(from);
		} catch (Exception e) {
			System.out.println("Could not get the caller's name");
			return false;
		}
		if (caller.equals(forwardTarget))
			return false;
		Header to = request.getHeader("To"); 
		request.removeHeader("To");
		to = replaceName(to, forwardTarget);
		if (to == null)
			return false;
		request.addHeader(to);
		
		Configuration configuration = proxy.getConfiguration();
        String defaultDomainName = configuration.stackIPAddress + ":4000";
        String callee = forwardTarget + "@" + defaultDomainName;

        //Let's be uri fault tolerant
        if (callee.toLowerCase().indexOf("sip:") == -1 //no sip scheme
            && callee.indexOf('@') != -1 //most probably a sip uri
            ) {
            callee = "sip:" + callee;

        }

        URI requestURI;
        try {
            requestURI = proxy.getAddressFactory().createURI(callee);
        }
        catch (ParseException ex) {
        	return false;
        }
        
        request.setRequestURI(requestURI);
		return true;
	}
	
	private Header replaceName(Header header, String forwardTarget)
	{
		String pattern = "<sip:[^;@]*([@;].*)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(header.toString());
		m.find();
		String to = "<sip:" + forwardTarget + m.group(1);
		HeaderFactory headerFactory = proxy.getHeaderFactory();
		Header toHeader;
		try {
			toHeader = headerFactory.createHeader("To", to);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		return toHeader;
	}
	
	public void sendDecline(RequestEvent requestEvent) {
		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		Request request = requestEvent.getRequest();
		MessageFactory messageFactory = proxy.getMessageFactory();
		Response response;
		try {
			response = messageFactory.createResponse(Response.DECLINE, request);
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
			ProxyDebug.println("Failed to send Decline response");
			e.printStackTrace();
		}
	}

	public void handleFinalTargetRequest(RequestEvent requestEvent)
	{
		Request request = requestEvent.getRequest();
		Header from = request.getHeader("From");
		String caller = null;
		try {
			caller = getName(from);
		} catch (Exception e) {
			return;
		}
		
		String target = getFinalCallee(request);
		if (target.equals(caller))
			sendFinalTarget(requestEvent, "");
		else
			sendFinalTarget(requestEvent, target);
	}

	private void sendFinalTarget(RequestEvent requestEvent, String target)
	{
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

        // Current Forward Service header
        String headerName  = Constants.SERVICE;
        String headerValue = Constants.GET_FINAL_TARGET;
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
        String currentTarget = Constants.CURRENT_TARGET;
        Header currentTargetHeader;
		try {
			currentTargetHeader = headerFactory.createHeader(currentTarget, target);
		} catch (ParseException e) {
            ProxyDebug.println("An unexpected error occurred while constructing the Service Header");
            e.printStackTrace();
            return;
		}
		response.addHeader(currentTargetHeader);	
		
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
}