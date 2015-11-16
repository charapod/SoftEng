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
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;
import net.java.sip.communicator.sip.softeng.BlockingProcessing;

public class BlockingFrame extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	JTextField blockTextField;
	JList<String> blockedList;
	DefaultListModel<String> blockedListModel;
	private final String CMD_BLOCK   = "cmd.block";
	private final String CMD_UNBLOCK = "cmd.unblock";
	private BlockingProcessing blockingProcessing;
	
	
	/**
	 * Creates a new Blocking Settings Dialog
	 * @param sipManager 
	 */
	public BlockingFrame(Frame parent, SipManager sipManager, boolean modal)
	{
		super(parent, modal);
		this.blockingProcessing = sipManager.getBlockingProcessing();
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
	 * Creates the contents of the Blocking Settings Dialog
	 */
	private void initComponents()
	{
		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		
		setTitle("Blocking Settings");
		getAccessibleContext().setAccessibleDescription("Blocking Settings");
 		JPanel centerPane = new JPanel();
		centerPane.setLayout(new GridBagLayout());
		int gridy = 1;

		// label on top of window
		JLabel topLabel = new JLabel("Blocked Users:");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=gridy++;
		c.gridwidth = 2;
		c.anchor=GridBagConstraints.WEST;
		c.weighty = 0;
		c.insets=new Insets(12,12,0,12);
		centerPane.add(topLabel, c);

		// list of blocked users
		blockedListModel = new DefaultListModel<String>();
		blockedList = new JList<String>(blockedListModel);
		JScrollPane pane = new JScrollPane(blockedList);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = gridy++;
		c.gridwidth = 3;
		c.anchor=GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.insets=new Insets(12,12,0,12);
		centerPane.add(pane, c);
		
		// Unblock button		
		JButton unblockButton = new JButton();
		unblockButton.setText("Unblock");
		unblockButton.setPreferredSize(new Dimension(100, 25));
		unblockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				clickHandler(CMD_UNBLOCK);
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
		centerPane.add(unblockButton, c);

		// Block button
		JButton blockButton = new JButton();
		blockButton.setText("Block User");
		blockButton.setPreferredSize(new Dimension(100, 25));
		blockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				clickHandler(CMD_BLOCK);
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
		centerPane.add(blockButton, c);

		// Block user text field
        blockTextField = new JTextField();
        blockTextField.setPreferredSize(new Dimension(100, 25));
        
        c = new GridBagConstraints();
        c.gridx   = 1;
        c.gridy   = gridy++;
        c.anchor  = GridBagConstraints.WEST;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.insets  = new Insets(12,12,12,12);
        centerPane.add(blockTextField, c);
		
		contents.add(centerPane, BorderLayout.CENTER);
		getRootPane().setDefaultButton(blockButton);
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
		System.out.println("Received command: "+actionCommand+"\tText = "+blockTextField.getText());
		
		if (actionCommand.equals(CMD_BLOCK)) {
			String blocked = blockTextField.getText();
			if (blocked == null || "".equals(blocked)) {
				JOptionPane.showMessageDialog(this,  "You must provide the username of the user to be blocked.");
				return;
			} else if (!isAlphaNumeric(blocked)) {
				JOptionPane.showMessageDialog(this, "The username must be alphanumeric");
				return;
			}
			try {
				blockingProcessing.processBlock(blocked);
			} catch (CommunicationsException e) {
				JOptionPane.showMessageDialog(this, "Unable to send the block request. (T__T)");
			}
		} else if (actionCommand.equals(CMD_UNBLOCK)) {
			String unblocked = blockedList.getSelectedValue();
			if (unblocked == null || "".equals(unblocked)) {
				JOptionPane.showMessageDialog(this,  "Please select the user to be unblocked.");
				return;
			}
			try {
				blockingProcessing.processUnblock(unblocked);
			} catch (CommunicationsException e) {
				JOptionPane.showMessageDialog(this,  "Unable to process block request. (T__T)");
			}
		}
	}

	/**
	 * Sets the list of blocked users
	 * 
	 * @param blockedUsers the new list of blocked users to display
	 */
	public void setBlockedList(ArrayList<String> blockedUsers)
	{
		blockedListModel.clear();
		for (String user : blockedUsers)
			blockedListModel.addElement(user);
	}
	
	// source: stackoverflow.org
	private boolean isAlphaNumeric(String s){
	    String pattern= "^[a-zA-Z0-9]*$";
	        if(s.matches(pattern)){
	            return true;
	        }
	        return false;   
	}
}
