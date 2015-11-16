package net.java.sip.communicator.sip.softeng;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;

public class BlockingProcessing {

	private BlockRequestSender blockRequestSender;
	private UnblockRequestSender unblockRequestSender;
	private BlockedListRequestSender blockedListRequestSender;

	public BlockingProcessing(SipManager sipManCallback)
	{
        blockRequestSender 		 = new BlockRequestSender(sipManCallback);
        unblockRequestSender 	 = new UnblockRequestSender(sipManCallback);
        blockedListRequestSender = new BlockedListRequestSender(sipManCallback);
	}

	public void processBlock(String blocked) throws CommunicationsException
	{
		blockRequestSender.sendRequest(blocked);
	}
	
	public void processUnblock(String target) throws CommunicationsException
	{
		unblockRequestSender.sendRequest(target);
	}
	
	public void requestBlockedList() throws CommunicationsException
	{
		blockedListRequestSender.sendRequest("dummy"); // dummy target
	}
}
