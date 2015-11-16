
package net.java.sip.communicator.sip.softeng;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;

public class ForwardingProcessing 
{
	private ForwardRequestSender forwardRequestSender;
	private UnforwardRequestSender unforwardRequestSender;
	private CurrentForwardRequestSender currentForwardRequestSender;
	private FinalTargetRequestSender finalTargetRequestSender;

	public ForwardingProcessing(SipManager sipManCallback)
	{
        forwardRequestSender 		= new ForwardRequestSender(sipManCallback);
        unforwardRequestSender 		= new UnforwardRequestSender(sipManCallback);
        currentForwardRequestSender = new CurrentForwardRequestSender(sipManCallback);
        finalTargetRequestSender    = new FinalTargetRequestSender(sipManCallback);
	}

	public void processForward(String target) throws CommunicationsException
	{
		forwardRequestSender.sendRequest(target);
	}
	
	public void processUnforward() throws CommunicationsException
	{
		unforwardRequestSender.sendRequest("dummy"); // dummy target
	}

	public void getCurrentForwardTarget() throws CommunicationsException
	{
		currentForwardRequestSender.sendRequest("dummy"); // dummy target
	}

	public void getFinalForwardTarget(String callee) throws CommunicationsException
	{
		finalTargetRequestSender.sendRequest(callee);
	}
}