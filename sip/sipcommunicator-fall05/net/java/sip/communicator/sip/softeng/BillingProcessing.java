package net.java.sip.communicator.sip.softeng;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;

public class BillingProcessing
{
	private TotalBillRequestSender totalBillRequestSender;
	private StrategySender byRateStrategySender;
	private StrategySender fixedStrategySender;
	private StrategySender freeStrategySender;
	private GetStrategySender getStrategySender;

	public BillingProcessing(SipManager sipManCallback)
	{
        totalBillRequestSender = new TotalBillRequestSender(sipManCallback);
        byRateStrategySender   = new StrategySender(sipManCallback, Constants.CHARGE_BY_RATE);
        fixedStrategySender    = new StrategySender(sipManCallback, Constants.CHARGE_FIXED_PRICE);
        freeStrategySender     = new StrategySender(sipManCallback, Constants.FREE_OF_CHARGE);
        getStrategySender      = new GetStrategySender(sipManCallback);
	}

	public void requestTotalBill() throws CommunicationsException
	{
		totalBillRequestSender.sendRequest("dummy"); // dummy target
	}

	public void setStrategy(String strategy) throws CommunicationsException
	{
		System.out.println("Sending request to change strategy to: " + strategy);

		if (strategy.equals(Constants.CHARGE_BY_RATE))
			byRateStrategySender.sendRequest("dummy");
		else if (strategy.equals(Constants.CHARGE_FIXED_PRICE))
			fixedStrategySender.sendRequest("dummy");
		else if (strategy.equals(Constants.FREE_OF_CHARGE))
			freeStrategySender.sendRequest("dummy");
	}
	
	public void getStrategy() throws CommunicationsException
	{
		getStrategySender.sendRequest("dummy");
	}
}
