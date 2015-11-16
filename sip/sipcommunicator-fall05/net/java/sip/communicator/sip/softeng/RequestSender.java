package net.java.sip.communicator.sip.softeng;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import net.java.sip.communicator.common.Console;
import net.java.sip.communicator.common.PropertiesDepot;
import net.java.sip.communicator.common.Utils;
import net.java.sip.communicator.sip.CallProcessing;
import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;

public abstract class RequestSender
{
	protected static final Console console = Console.getConsole(CallProcessing.class);
	protected SipManager sipManCallback;

	public RequestSender(SipManager sipManCallback)
	{
        this.sipManCallback = sipManCallback;
	}
	
	protected abstract void addCustomHeaders(Request request) throws ParseException;
	
	/* 
	 * Template Method used to send INFO messages with custom headers
	 */
	public void sendRequest(String target) throws CommunicationsException
	{
        try
        {
            console.logEntry();

            target = target.trim();
            // Remove excessive characters from phone numbers such as ' ','(',')','-'
            String excessiveChars = Utils.getProperty("net.java.sip.communicator.sip.EXCESSIVE_URI_CHARACTERS");

            //---------------------------------------------------------------------------
            // an ugly hack to override old xml configurations (todo: remove at some point)
            // define excessive chars for sipphone.com users
            String isSipphone = Utils.getProperty("net.java.sip.communicator.sipphone.IS_RUNNING_SIPPHONE");
            if(excessiveChars == null && isSipphone != null && isSipphone.equalsIgnoreCase("true"))
            {
                excessiveChars = "( )-";
                PropertiesDepot.setProperty("net.java.sip.communicator.sip.EXCESSIVE_URI_CHARACTERS", excessiveChars);
                PropertiesDepot.storeProperties();
            }
            // ---------------------------------------------------------------------------

            if(excessiveChars != null )
            {
                StringBuffer calleeBuff = new StringBuffer(target);
                for(int i = 0; i < excessiveChars.length(); i++)
                {
                    String charToDeleteStr = excessiveChars.substring(i, i+1);

                    int charIndex = -1;
                    while( (charIndex = calleeBuff.indexOf(charToDeleteStr)) != -1)
                        calleeBuff.delete(charIndex, charIndex + 1);
                }
                target = calleeBuff.toString();
            }

            // Handle default domain name (i.e. transform 1234 -> 1234@sip.com
            String defaultDomainName =
                Utils.getProperty("net.java.sip.communicator.sip.DEFAULT_DOMAIN_NAME");
            if (defaultDomainName != null //no sip scheme
                && !target.trim().startsWith("tel:")
                && target.indexOf('@') == -1 //most probably a sip uri
                ) {
                target = target + "@" + defaultDomainName;
            }

            // Let's be uri fault tolerant
            if (target.toLowerCase().indexOf("sip:") == -1 //no sip scheme
                && target.indexOf('@') != -1 //most probably a sip uri
                ) {
                target = "sip:" + target;

            }
            
            // Request URI
            URI requestURI;
            try {
                requestURI = sipManCallback.addressFactory.createURI(target);
            }
            catch (ParseException ex) {
                console.error(target + " is not a legal SIP uri!", ex);
                throw new CommunicationsException(target + " is not a legal SIP uri!", ex);
            }
            
            // Call ID
            CallIdHeader callIdHeader = sipManCallback.sipProvider.getNewCallId();
            // CSeq
            CSeqHeader cSeqHeader;
            try {
                cSeqHeader = sipManCallback.headerFactory.createCSeqHeader(1, Request.INFO);
            }
            catch (ParseException ex) { //Shouldn't happen
                console.error(ex, ex);
                throw new CommunicationsException(
                    "An unexpected error occurred while"
                    + "constructing the CSeqHeadder", ex);
            }
            catch (InvalidArgumentException ex) { //Shouldn't happen
                console.error(
                    "An unexpected error occurred while"
                    + "constructing the CSeqHeadder", ex);
                throw new CommunicationsException(
                    "An unexpected error occurred while"
                    + "constructing the CSeqHeadder", ex);
            }

            // FromHeader
            FromHeader fromHeader = sipManCallback.getFromHeader();
            // ToHeader
            Address toAddress = sipManCallback.addressFactory.createAddress(requestURI);
            ToHeader toHeader;
            try {
                toHeader = sipManCallback.headerFactory.createToHeader(toAddress, null);
            }
            catch (ParseException ex) { //Shouldn't happen
                console.error(
                    "Null is not an allowed tag for the to header!", ex);
                throw new CommunicationsException(
                    "Null is not an allowed tag for the to header!", ex);
            }
            
            // ViaHeaders
            @SuppressWarnings("rawtypes")
			ArrayList viaHeaders = sipManCallback.getLocalViaHeaders();
            // MaxForwards
            MaxForwardsHeader maxForwards = sipManCallback.getMaxForwardsHeader();

            
            Request request = null;
            try {
                request = sipManCallback.messageFactory.createRequest(requestURI,
                    Request.INFO, callIdHeader, cSeqHeader, fromHeader, 
                    toHeader, viaHeaders, maxForwards);
            }
            catch (ParseException ex) {
                console.error(
                    "Failed to create Request!", ex);
                throw new CommunicationsException(
                    "Failed to create Request!", ex);
            }
            
            // Add the custom headers for each service
            try {
				addCustomHeaders(request);
			} catch (ParseException e) {
				console.error("An unexpected error occurred while constructing a header", e);
				throw new CommunicationsException(
						"An unexpected error occurred while constructing a header", e);
			}
			
            // Transaction
            ClientTransaction transaction;
            try {
                transaction = sipManCallback.sipProvider.
                    getNewClientTransaction(request);
            }
            catch (TransactionUnavailableException ex) {
                console.error(
                    "Failed to create transaction.\n" +
                    "This is most probably a network connection error.", ex);
                throw new CommunicationsException(
                    "Failed to create transaction.\n" +
                    "This is most probably a network connection error.", ex);
            }
            try {
                transaction.sendRequest();
                if( console.isDebugEnabled() )
                    console.debug("sent request: " + request);
            }
            catch (SipException ex) {
                console.error(
                    "An error occurred while sending a request", ex);
                throw new CommunicationsException(
                    "An error occurred while sending a request", ex);
            }
        }
        finally
        {
            console.logExit();
        }
	}
}
