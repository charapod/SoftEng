package net.java.sip.communicator.sip.softeng;

import java.text.ParseException;

import javax.sip.header.Header;
import javax.sip.message.Request;

import net.java.sip.communicator.sip.SipManager;

public class BlockRequestSender extends RequestSender {

	public BlockRequestSender(SipManager sipManCallback)
	{
		super(sipManCallback);
	}

	@Override
	protected void addCustomHeaders(Request request) throws ParseException
	{
        String headerName = Constants.SERVICE;
        String headerValue = Constants.BLOCK;
        Header header = sipManCallback.headerFactory.createHeader(headerName, headerValue);
    	request.addHeader(header);
	}
}