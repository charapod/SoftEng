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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.sip.CommunicationsException;
import net.java.sip.communicator.sip.SipManager;
import net.java.sip.communicator.sip.softeng.BillingProcessing;
import net.java.sip.communicator.sip.softeng.Constants;

// TODO kostis 10-4-2015
public class BillingFrame extends JDialog
{
	private static final long serialVersionUID = 1L;
	private JLabel billLabel;
	private JLabel billStrategy;
	private JComboBox<String> billStrategies;
	private BillingProcessing billingProcessing;
	private final String CMD_CHANGE = "cmd.change";
	
	/**
	 * Creates a new Billing Settings Dialog
	 * @param sipManager 
	 * @param sipManager 
	 */
	public BillingFrame(Frame parent, SipManager sipManager, boolean modal)
	{
		super(parent, modal);
		this.billingProcessing = sipManager.getBillingProcessing();
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
	 * Creates the contents of the Billing Settings Dialog
	 */
	private void initComponents()
	{
		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		
		setTitle("Billing Settings");
		getAccessibleContext().setAccessibleDescription("Billing Settings");
 		JPanel centerPane = new JPanel();
		centerPane.setLayout(new GridBagLayout());
		int gridy = 1;

		// Current billing strategy title
		JLabel strategyLabel = new JLabel("Current Strategy:");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx     = 0;
		c.gridy     = gridy;
		c.anchor    = GridBagConstraints.WEST;
		c.fill      = GridBagConstraints.HORIZONTAL;
		c.weighty   = 0;
		c.insets    = new Insets(12,12,0,12);
		centerPane.add(strategyLabel, c);
		
		// The name of the current billing strategy
		billStrategy = new JLabel(Constants.CHARGE_BY_RATE);
		c = new GridBagConstraints();
		c.gridx     = 1;
		c.gridy     = gridy++;
		c.anchor    = GridBagConstraints.WEST;
		c.fill      = GridBagConstraints.HORIZONTAL;
		c.weighty   = 0;
		c.insets    = new Insets(12,0,0,12);
		centerPane.add(billStrategy, c);
		
		// Drop down menu to select the billing package
		String[] strategies = {Constants.CHARGE_BY_RATE, Constants.CHARGE_FIXED_PRICE, Constants.FREE_OF_CHARGE};
		billStrategies = new JComboBox<String>(strategies);
		billStrategies.setPreferredSize(new Dimension(120, 25));
		c = new GridBagConstraints();
		c.gridx   = 0;
		c.gridy   = gridy;
		c.anchor  = GridBagConstraints.WEST;
		c.fill    = GridBagConstraints.HORIZONTAL;
		c.weighty = 0;
		c.insets=new Insets(12,12,0,12);
		centerPane.add(billStrategies, c);
		
		// Change strategy button
		JButton changeButton = new JButton();
		changeButton.setText("Change");
		changeButton.setPreferredSize(new Dimension(120, 25));
		changeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				clickHandler(CMD_CHANGE);
			}

		});
		c = new GridBagConstraints();
		c.gridx   = 1;
		c.gridy   = gridy++;
		c.anchor  = GridBagConstraints.WEST;
		c.fill    = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.insets  = new Insets(12,0,0,12);
		centerPane.add(changeButton, c);
		
		// label showing the current bill
		billLabel = new JLabel(" Retreiving Data...");
		c = new GridBagConstraints();
		c.gridx   = 0;
		c.gridy   = gridy++;
		c.anchor  = GridBagConstraints.WEST;
		c.weighty = 0;
		c.insets  = new Insets(12,12,12,12);
		centerPane.add(billLabel, c);
		
		contents.add(centerPane, BorderLayout.CENTER);
	}

	/**
	 * Handles a click event
	 */
	private void clickHandler(String cmd)
	{
		if (cmd.equals(CMD_CHANGE)) {
			String strategy = (String) billStrategies.getSelectedItem();
			try {
				billingProcessing.setStrategy(strategy);
			} catch (CommunicationsException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets the total bill label
	 * @param bill the new total bill
	 */
	public void setTotalBill(Long bill)
	{
		billLabel.setText("Total Bill :   " + bill + " euros.");
	}

	public void setCurrentStrategy(String strategy)
	{
		billStrategy.setText(strategy);
	}
	// TODO kostis
}
