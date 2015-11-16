package net.java.sip.communicator.gui.softeng;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;
import net.java.sip.communicator.sip.softeng.ForwardingProcessing;

public class ForwardingFrame extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	// TODO kostis 24-1-2015
	JTextField forwardTextField;
	JLabel forwardLabel;
	private final String CMD_FORWARD   = "cmd.forward";
	private final String CMD_UNFORWARD = "cmd.unforward";
	private ForwardingProcessing forwardingProcessing;
	// TODO kostis
	
	/**
	 * Creates a new Forwarding Settings Dialog
	 * @param sipManager 
	 */
	public ForwardingFrame(Frame parent, SipManager sipManager, boolean modal)
	{
		super(parent, modal);
		this.forwardingProcessing = sipManager.getForwardingProcessing();
		initComponents();
		pack();
		centerWindow();
	}
	
	/**
	 * Centers the window on the screen.
	 */
	private void centerWindow()
	{
		Rectangle screen = new Rectangle(
				Toolkit.getDefaultToolkit().getScreenSize());
		Point center = new Point(
				(int) screen.getCenterX(), (int) screen.getCenterY());
		Point newLocation = new Point(
				center.x - this.getWidth() / 2, center.y - this.getHeight() / 2);
		if (screen.contains(newLocation.x, newLocation.y,
				this.getWidth(), this.getHeight())) {
			this.setLocation(newLocation);
		}
	}
	
	/**
	 * Creates the contents of the Forwarding Settings Dialog
	 */
	private void initComponents()
	{
		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		
		setTitle("Forwarding Settings");
		getAccessibleContext().setAccessibleDescription("Forwarding Settings");
 		JPanel centerPane = new JPanel();
		centerPane.setLayout(new GridBagLayout());
		int gridy = 1;

		// label on top of window
		// TODO kostis 24-1-2015
		forwardLabel = new JLabel("Currently forwarding to:");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=gridy++;
		c.gridwidth = 2;
		c.anchor=GridBagConstraints.WEST;
		c.weighty = 0;
		c.insets=new Insets(12,12,0,12);
		centerPane.add(forwardLabel, c);
		// TODO end
		
		// Unforward button		
		JButton unforwardButton = new JButton();
		unforwardButton.setText("Unforward");
		unforwardButton.setPreferredSize(new Dimension(125, 25));
		unforwardButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				clickHandler(CMD_UNFORWARD);
			}
		});
		
		c = new GridBagConstraints();
		c.gridx   = 0;
		c.gridy   = gridy++;
		c.anchor  = GridBagConstraints.WEST;
		c.fill    = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.insets  = new Insets(12,12,0,12);
		centerPane.add(unforwardButton, c);

		// Forward button
		JButton forwardButton = new JButton();
		forwardButton.setText("Forward to User");
		forwardButton.setPreferredSize(new Dimension(125, 25));
		forwardButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				clickHandler(CMD_FORWARD);
			}
		});
		
		c = new GridBagConstraints();
		c.gridx   = 0;
		c.gridy   = gridy;
		c.anchor  = GridBagConstraints.WEST;
		c.fill    = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.insets  = new Insets(12,12,12,12);
		centerPane.add(forwardButton, c);

		// Forward to user text field
        forwardTextField = new JTextField();
        forwardTextField.setPreferredSize(new Dimension(125, 25));
        
        c = new GridBagConstraints();
        c.gridx   = 1;
        c.gridy   = gridy++;
        c.anchor  = GridBagConstraints.WEST;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.insets  = new Insets(12,12,12,12);
        centerPane.add(forwardTextField, c);
		
		contents.add(centerPane, BorderLayout.CENTER);
		getRootPane().setDefaultButton(forwardButton);
	}
	
	/**
	 * Handles a click event.
	 * If the blocked user list is updated as a result the proxy will 
 	 * respond with the new list asynchronously.
	 * 
	 * @param actionCommand the action performed by the user
	 */
	private void clickHandler(Object actionCommand)
	{
		System.out.println("Received command: "+actionCommand+"\tText = "+forwardTextField.getText());
		
		if (actionCommand.equals(CMD_FORWARD)) {
			String forwarding = forwardTextField.getText();
			if (forwarding == null || "".equals(forwarding)) {
				JOptionPane.showMessageDialog(this,  "You must provide the username of the user to forward your calls.");
				return;
			}
			else if (!isAlphaNumeric(forwarding)) {
				JOptionPane.showMessageDialog(this, "The username must be Alphanumeric");
				return;
			}
			try {
				forwardingProcessing.processForward(forwarding);
			} catch (CommunicationsException e) {
				JOptionPane.showMessageDialog(this, "Unable to send the forward request. (T__T)");
			}
		} else if (actionCommand.equals(CMD_UNFORWARD)) {
			try {
				forwardingProcessing.processUnforward();
			} catch (CommunicationsException e) {
				JOptionPane.showMessageDialog(this,  "Unable to process unforward request. (T__T)");
			}
		}
	}
	
	// TODO kostis 24-1-2015
	/**
	 * Sets the displayed forwarding target to the given string
	 * 
	 * @param target the target the user is currently forwarding to
	 */
	public void setCurrentTarget(String target)
	{
		forwardLabel.setText("Currently forwarding to:    " + target);
	}
	// TODO end
	
	// source: stackoverflow.org
	private boolean isAlphaNumeric(String s){
	    String pattern= "^[a-zA-Z0-9]*$";
	        if(s.matches(pattern)){
	            return true;
	        }
	        return false;   
	}
}