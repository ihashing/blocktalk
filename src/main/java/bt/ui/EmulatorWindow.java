package bt.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import bt.Address;
import bt.Contract;
import bt.Emulator;
import bt.Register;
import bt.Transaction;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

/**
 * Graphical user interface for the blockchain emulator.
 * 
 * @author jjos
 *
 */
@SuppressWarnings("serial")
public class EmulatorWindow extends JFrame implements ActionListener {

	public static void main(String[] args) {
		new EmulatorWindow(null);
	}

	private JButton forgeButton;
	private JButton airDropButton;
	private JButton sendButton;
	private JButton createATButton;
	private JTextField airDropAddress;
	private JTextField airDropAmount;
	private JComboBox<Address> sendFrom;
	private JComboBox<Address> sendTo;
	private JTextField sendAmount;
	private HintTextField sendMessage;
	private JComboBox<Address> atCreator;
	private JTextField atClassField;
	private JTextField atActivation;
	private JTable addressesTable;
	private JTable txsTable;
	private AbstractTableModel addrsTableModel;
	private AbstractTableModel txsTableModel;
	private JTextField atAddressField;
	private JButton compileATButton;
	private JButton callButton;
	private JLabel blockLabel;

	public EmulatorWindow(Class<?> c) {
		super("BlockTalk Emulator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// tooltip configuration
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(15000);

		IconFontSwing.register(FontAwesome.getIconFont());

		try {
			Class<?> lafc = null;
			try {
				lafc = Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				lafc = Class.forName("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			}
			LookAndFeel laf = (LookAndFeel) lafc.getConstructor().newInstance();
			UIManager.setLookAndFeel(laf);
		} catch (Exception e) {
		}

		JPanel topPanel = new JPanel(new BorderLayout());
		getContentPane().add(topPanel, BorderLayout.PAGE_START);

		JPanel cmdPanel = new JPanel(new GridLayout(0, 5, 2, 0));
		cmdPanel.setBorder(new TitledBorder("ACTIONS"));
		topPanel.add(cmdPanel, BorderLayout.LINE_START);

		cmdPanel.add(forgeButton = new JButton("Forge block"));
		forgeButton.addActionListener(this);
		cmdPanel.add(blockLabel = new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());

		cmdPanel.add(airDropButton = new JButton("Air drop"));
		airDropButton.addActionListener(this);
		cmdPanel.add(airDropAddress = new HintTextField("Receiver", airDropButton));
		airDropAddress.setToolTipText("The address receiving the air drop");
		cmdPanel.add(airDropAmount = new HintTextField("Amount", airDropButton));
		airDropAmount.setToolTipText("The amount to air drop in BURST = 10\u2078 NQT");
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());

		cmdPanel.add(sendButton = new JButton("Send"));
		sendButton.addActionListener(this);
		cmdPanel.add(sendFrom = new JComboBox<Address>());
		sendFrom.setToolTipText("Sender address");
		cmdPanel.add(sendTo = new JComboBox<Address>());
		sendTo.setToolTipText("Receiver address");
		sendTo.addActionListener(this);
		cmdPanel.add(sendAmount = new HintTextField("Amount", sendButton));
		sendAmount.setToolTipText("The amount to send in BURST = 10\u2078 NQT");
		cmdPanel.add(sendMessage = new HintTextField("Message/Function", sendButton));
		sendMessage.setToolTipText("The message to send or contract function to call");

		callButton = new JButton();
		callButton.addActionListener(this);
		callButton.setToolTipText("Function call");
		callButton.setIcon(IconFontSwing.buildIcon(FontAwesome.PENCIL_SQUARE_O, 14));
		ComponentBorder cb = new ComponentBorder(callButton);
		cb.setAdjustInsets(true);
		cb.setGap(2);
		cb.install(sendMessage);

		cmdPanel.add(createATButton = new JButton("Create Contract"));
		createATButton.addActionListener(this);
		cmdPanel.add(atCreator = new JComboBox<Address>());
		atCreator.setToolTipText("Contract creator address");
		cmdPanel.add(atAddressField = new HintTextField("Contract address", createATButton));
		atAddressField.setToolTipText("Address to be assigned to this contract");
		cmdPanel.add(atActivation = new HintTextField("Activation fee", createATButton));
		atActivation.setToolTipText("Contract activation fee in BURST = 10\u2078 NQT");
		cmdPanel.add(atClassField = new HintTextField("Java class path", createATButton));
		atClassField.setToolTipText("Full path for the contract java class");
		if (c != null)
			atClassField.setText(c.getName());

		compileATButton = new JButton();
		compileATButton.addActionListener(this);
		compileATButton.setToolTipText("Compile/Publish contract");
		compileATButton.setIcon(IconFontSwing.buildIcon(FontAwesome.SERVER, 14));
		cb = new ComponentBorder(compileATButton);
		cb.setGap(0);
		cb.install(atClassField);

		JPanel accountsPanel = new JPanel(new BorderLayout());
		accountsPanel.setBorder(new TitledBorder("ACCOUNTS"));
		topPanel.add(accountsPanel, BorderLayout.CENTER);

		class CellRenderer extends DefaultTableCellRenderer {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
						column);
				c.setToolTipText(null);
				if (value instanceof Address) {
					Address add = (Address) value;
					if (add.getContract() != null)
						c.setToolTipText(add.getContract().getFieldValues());
				}
				return c;
			}
		}

		addrsTableModel = new AbstractTableModel() {
			@Override
			public String getColumnName(int column) {
				return column == 0 ? "Address" : "Balance";
			}

			@Override
			public Object getValueAt(int r, int c) {
				Address a = Emulator.getInstance().getAddresses().get(r);
				return c == 0 ? a : ((double) a.getBalance()) / Contract.ONE_BURST;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}

			@Override
			public int getRowCount() {
				return Emulator.getInstance().getAddresses().size();
			}

			@Override
			public int getColumnCount() {
				return 2;
			}
		};

		addressesTable = new JTable(addrsTableModel);
		addressesTable.getColumnModel().getColumn(0).setCellRenderer(new CellRenderer());
		JScrollPane sp = new JScrollPane(addressesTable);
		sp.setPreferredSize(new Dimension(300, 40));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		addressesTable.setFillsViewportHeight(true);
		accountsPanel.add(sp, BorderLayout.CENTER);

		JPanel txsPanel = new JPanel(new BorderLayout());
		txsPanel.setBorder(new TitledBorder("TRANSACTIONS"));
		getContentPane().add(txsPanel, BorderLayout.CENTER);

		txsTableModel = new AbstractTableModel() {
			static final int SENDER_COL = 0;
			static final int RECEIVER_COL = 1;
			static final int AMOUNT_COL = 2;
			static final int TYPE_COL = 3;
			static final int MSG_COL = 4;
			static final int CONF_COL = 5;

			@Override
			public String getColumnName(int column) {
				switch (column) {
				case SENDER_COL:
					return "Sender";
				case RECEIVER_COL:
					return "Receiver";
				case AMOUNT_COL:
					return "Amount";
				case TYPE_COL:
					return "Type";
				case MSG_COL:
					return "Message";
				case CONF_COL:
					return "Confirmations";
				default:
					return "";
				}
			}

			@Override
			public Object getValueAt(int r, int c) {
				ArrayList<Transaction> txs = Emulator.getInstance().getTxs();
				Transaction tx = txs.get(txs.size() - r - 1);
				switch (c) {
				case CONF_COL:
					return Emulator.getInstance().getCurrentBlock().getHeight() - tx.getBlock().getHeight() - 1;
				case TYPE_COL:
					if (tx.getType() == 2)
						return "New contract";
					else if (tx.getType() == 1)
						return "Message";
					else
						return "Payment";
				case SENDER_COL:
					return tx.getSenderAddress() == null ? null : tx.getSenderAddress().getRsAddress();
				case RECEIVER_COL:
					return tx.getReceiverAddress() == null ? null : tx.getReceiverAddress().getRsAddress();
				case MSG_COL:
					return tx.getMessageString() != null ? tx.getMessageString() : tx.getMessage();
				case AMOUNT_COL:
					return ((double) tx.getAmount()) / Contract.ONE_BURST;
				default:
					break;
				}
				return null;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}

			@Override
			public int getRowCount() {
				return Emulator.getInstance().getTxs().size();
			}

			@Override
			public int getColumnCount() {
				return 6;
			}
		};

		txsTable = new JTable(txsTableModel);
		new JScrollPane(txsTable);
		txsTable.setFillsViewportHeight(true);

		sp = new JScrollPane(txsTable);
		sp.setPreferredSize(new Dimension(100, 200));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		txsPanel.add(sp, BorderLayout.CENTER);

		rebuildComboboxes();

		pack();

		setLocationRelativeTo(null);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == forgeButton) {
			try {
				Emulator.getInstance().forgeBlock();
				blockLabel.setText("Block height=" + (Emulator.getInstance().getCurrentBlock().getHeight() - 1));
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
			rebuildComboboxes();
			txsTableModel.fireTableDataChanged();
			addrsTableModel.fireTableDataChanged();
		} else if (e.getSource() == airDropButton) {
			String rs = airDropAddress.getText();
			if (rs == null || rs.trim().length() == 0) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid receiver address", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			double amount = 0;
			try {
				amount = Double.parseDouble(airDropAmount.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Could not parse the amount", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			amount *= Contract.ONE_BURST;

			Address receiver = Emulator.getInstance().getAddress(airDropAddress.getText());
			Emulator emu = Emulator.getInstance();
			emu.airDrop(receiver, (long) amount);

			addrsTableModel.fireTableDataChanged();
			rebuildComboboxes();

		} else if (e.getSource() == callButton) {
			Address to = (Address) sendTo.getSelectedItem();

			if (to == null || to.getContract() == null) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid contract address", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			MethodCallDialog dlg = new MethodCallDialog(this, to.getContract());

			if (dlg.execute() == JOptionPane.OK_OPTION) {
				sendMessage.setObject(dlg.getMessage());
			} else
				sendMessage.setObject(null);
		} else if (e.getSource() == sendButton) {
			Address from = (Address) sendFrom.getSelectedItem();
			Address to = (Address) sendTo.getSelectedItem();

			if (from == null || to == null) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid address", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			double amount = 0;
			try {
				amount = Double.parseDouble(sendAmount.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Could not parse the amount", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			amount *= Contract.ONE_BURST;

			Emulator emu = Emulator.getInstance();

			Register msgReg = (Register) sendMessage.getObject();
			if (msgReg != null) {
				emu.send(from, to, (long) amount, msgReg);
			} else {
				String msg = sendMessage.isShowingHint() ? null : sendMessage.getText();
				emu.send(from, to, (long) amount, msg);
			}
			txsTableModel.fireTableDataChanged();
			addrsTableModel.fireTableDataChanged();
		} else if (e.getSource() == createATButton) {
			Emulator emu = Emulator.getInstance();

			Address creator = (Address) atCreator.getSelectedItem();
			if (creator == null) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Creator address is empty", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			String atAddrRS = atAddressField.getText();
			if (atAddrRS == null || atAddrRS.trim().length() == 0) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Contract address is empty", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			Address atAddress = emu.findAddress(atAddrRS);
			if (atAddress != null) {
				JOptionPane.showMessageDialog(EmulatorWindow.this,
						"Contract address already registered, choose a new one", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			atAddress = emu.getAddress(atAddrRS);

			double actAmount = 0;
			try {
				actAmount = Double.parseDouble(atActivation.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid activation fee", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			actAmount *= Contract.ONE_BURST;

			String atClass = atClassField.getText();
			try {
				Class.forName(atClass);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Contract instantiation: " + ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			emu.createConctract(creator, atAddress, atClass, (long) actAmount);

			addrsTableModel.fireTableDataChanged();
			txsTableModel.fireTableDataChanged();
		} else if (e.getSource() == sendTo){
			sendMessage.setObject(null);
		} else if (e.getSource() == compileATButton) {
			String atClass = atClassField.getText();
			CompileDialog dlg = new CompileDialog(this, atClass);
			dlg.execute();
		}
	}

	private void rebuildComboboxes() {
		// rebuild the from and to combo boxes
		sendFrom.removeAllItems();
		sendTo.removeAllItems();
		atCreator.removeAllItems();
		ArrayList<Address> addrs = Emulator.getInstance().getAddresses();
		for (int i = 0; i < addrs.size(); i++) {
			sendFrom.addItem(addrs.get(i));
			atCreator.addItem(addrs.get(i));
			sendTo.addItem(addrs.get(i));

			if (i == addrs.size() - 1)
				sendTo.setSelectedIndex(i);
		}
	}
}
