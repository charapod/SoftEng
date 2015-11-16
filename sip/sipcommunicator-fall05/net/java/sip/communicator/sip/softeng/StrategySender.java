package net.java.sip.communicator.sip.softeng;

import java.text.ParseException;

import javax.sip.header.Header;
import javax.sip.message.Request;

import net.java.sip.communicator.sip.SipManager;

public class StrategySender extends RequestSender 
{
	private String strategy;
	
	public StrategySender(SipManager sipManCallback, String strategy)
	{
		super(sipManCallback);
		this.strategy = strategy;
	}

	@Override
	protected void addCustomHeaders(Request request) throws ParseException 
	{
        String headerName = Constants.SERVICE;
        Header header = sipManCallback.headerFactory.createHeader(headerName, strategy);
    	request.addHeader(header);
	}
}