package net.java.sip.communicator.sip.softeng;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;

public class RegisterRequestProcessing
{
	private RegisterRequestSender registerRequestSender;
	private SipManager sipManCallback;

	public RegisterRequestProcessing(SipManager sipManCallback)
	{
		this.sipManCallback = sipManCallback;
	}

	public void processRegister(String username, String password) throws CommunicationsException
	{
		registerRequestSender = new RegisterRequestSender(sipManCallback, username, password);
		registerRequestSender.sendRequest("dummy");
	}
}
