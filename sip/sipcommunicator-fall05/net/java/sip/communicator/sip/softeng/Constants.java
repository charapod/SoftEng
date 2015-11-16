package net.java.sip.communicator.sip.softeng;

public class Constants 
{
	public static final String SERVICE 			   = "Service";
	public static final String BLOCK      		   = "Block";
	public static final String UNBLOCK    		   = "Unblock";
	public static final String GET_BLOCKED_LIST    = "GetBlockingList";
	public static final String FORWARD    	       = "Forward";
	public static final String UNFORWARD  		   = "Unforward";
	public static final String GET_CURRENT_FORWARD = "GetCurrentForward";
	public static final String BLOCKED_LIST		   = "BlockedList";
	public static final String CURRENT_TARGET      = "CurrentTarget"; // TODO kostis 24-1-2015
	public static final String GET_BILL            = "GetBill";  //TODO foivos 24-1-2015
	public static final String BILL                = "Bill";     //TODO foivos 24-1-2015
	public static final String CHANGE_STRATEGY     = "Change Strategy";
	public static final String CURRENT_STRATEGY    = "Current Strategy";
	
	// BillingFactory constants
	public static final String CHARGE_FIXED_PRICE  = "Fixed Price"; //TODO foivos 10-4-2015
	public static final String CHARGE_BY_RATE      = "Per Second"; //TODO foivos 10-4-2015
	public static final String FREE_OF_CHARGE      = "Free!"; //TODO foivos 10-4-2015
	public static final long   RATE                = 15; //TODO foivos 10-4-2015
	public static final long   FIXED_PRICE         = 50; //TODO foivos 10-4-2015
	
	// Registration constants
	public static final String USERNAME            = "username";
	public static final String PASSWORD            = "password";
	public static final String REGISTER            = "Register";
	public static final String GET_FINAL_TARGET    = "GetFinalTarget";
}
