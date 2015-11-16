package net.java.sip.communicator.sip.softeng;

import java.text.ParseException;

import javax.sip.header.HeaderFactory;
import javax.sip.message.Request;

import net.java.sip.communicator.sip.SipManager;

public class RegisterRequestSender extends RequestSender 
{
	private String username;
	private String password;
	HeaderFactory headerFactory;
	
	public RegisterRequestSender(SipManager sipManCallback, String username, String password)
	{
		super(sipManCallback);
		this.username = username;
		this.password = password;
		this.headerFactory = sipManCallback.headerFactory;
	}

	@Override
	protected void addCustomHeaders(Request request) throws ParseException
	{
		// service header
        request.addHeader(headerFactory.createHeader(Constants.SERVICE, Constants.REGISTER));
    	// username header
        request.addHeader(headerFactory.createHeader(Constants.USERNAME, username));
    	// password header
        request.addHeader(headerFactory.createHeader(Constants.PASSWORD, password));
	}
}
