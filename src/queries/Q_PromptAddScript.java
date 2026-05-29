package queries;

import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Prompt for entering selection data and run query
 *
 * @author Vladimír Župka 2016
 *
 */
public class Q_PromptAddScript extends JDialog {

    Q_Properties prop;

    String language;
    ResourceBundle titles;
    ResourceBundle buttons;
    ResourceBundle messages;
    String addScript, defAddScr;
    String exit, enter, isEmpty;

    // Text field
    JTextField textField;

    // ..Dimensions of different views
    final Integer xLocation = 300;
    final Integer yLocation = 100;

    int windowWidth = 400;
    int windowHeight = 220;

    JPanel dataPanel;
    GroupLayout layout;
    JPanel dataGlobalPanel;
    Container dataContentPane;
    JScrollPane scrollPaneData;
    JLabel message;
    JButton returnButton;
    JButton enterButton;

    // Title of the window
    JPanel titlePanel;
    JPanel buttonPanel;

    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = new Color(190, 60, 50);
    
    boolean canceled = false;

    public Q_PromptAddScript(Q_ScriptEditCall scriptEditCall) {
        this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        
        Q_ScriptEditCall.scriptListMsgPanel.removeAll();
        scriptEditCall.setVisible(true);
        scriptEditCall.repaint();

        // Get application properties
        prop = new Q_Properties();
        language = prop.getProperty("LANGUAGE");
        Locale currentLocale = Locale.forLanguageTag(language);

        // Get resource bundle classes
        titles = ResourceBundle.getBundle("locales.L_TitleLabelBundle", currentLocale);
        buttons = ResourceBundle.getBundle("locales.L_ButtonBundle", currentLocale);
        messages = ResourceBundle.getBundle("locales.L_MessageBundle", currentLocale);

        // Localized button labels
        exit = buttons.getString("Exit");
        enter = buttons.getString("Enter");
        isEmpty = messages.getString("IsEmpty");

        returnButton = new JButton(exit);
        returnButton.setMinimumSize(new Dimension(140, 35));
        returnButton.setMaximumSize(new Dimension(140, 35));
        returnButton.setPreferredSize(new Dimension(140, 35));

        enterButton = new JButton(enter);
        enterButton.setMinimumSize(new Dimension(140, 35));
        enterButton.setMaximumSize(new Dimension(140, 35));
        enterButton.setPreferredSize(new Dimension(140, 35));
        enterButton.setForeground(DIM_BLUE); // Dim blue
        enterButton.setFont(returnButton.getFont().deriveFont(Font.PLAIN, 15));

        titlePanel = new JPanel();
        buttonPanel = new JPanel();
        dataGlobalPanel = new JPanel();
        dataPanel = new JPanel();
        layout = new GroupLayout(dataPanel);

        // Localized titles and labels
        addScript = titles.getString("AddScript");
        defAddScr = titles.getString("DefAddScr");

        JLabel inset = new JLabel(" ");
        inset.setPreferredSize(new Dimension(0, 0));

        JLabel title1 = new JLabel(addScript);
        title1.setFont(new Font("Helvetica", Font.PLAIN, 20));
        title1.setBackground(titlePanel.getBackground());
        JLabel title2 = new JLabel(defAddScr);
        title2.setBackground(titlePanel.getBackground());
        title2.setForeground(DIM_BLUE); // Dim blue

        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.PAGE_AXIS));
        titlePanel.setAlignmentX(Box.LEFT_ALIGNMENT);
        titlePanel.add(title1);
        titlePanel.add(inset);  // empty row  
        titlePanel.add(title2);

        textField = new JTextField();
        textField.setPreferredSize(new Dimension(250, 20));
        textField.setMaximumSize(new Dimension(250, 20));
        textField.setMinimumSize(new Dimension(250, 20));

        // Build button row
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setAlignmentX(Box.LEFT_ALIGNMENT);
        buttonPanel.add(returnButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(enterButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 50)));

        // Set empty message initially
        message = new JLabel("");

        // Arrange panels in group layout
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(
                layout.createParallelGroup(LEADING)
                        .addComponent(titlePanel)
                        .addComponent(textField)
                        .addComponent(buttonPanel)
                        .addComponent(message)));
        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(
                layout.createSequentialGroup()
                        .addComponent(titlePanel)
                        .addComponent(textField)
                        .addComponent(buttonPanel)
                        .addComponent(message)));
        dataPanel.setLayout(layout);

        dataGlobalPanel.add(dataPanel);

        scrollPaneData = new JScrollPane(dataGlobalPanel);
        scrollPaneData.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        scrollPaneData.setBackground(dataPanel.getBackground());

        dataGlobalPanel.add(dataPanel);
        dataContentPane = getContentPane();
        dataContentPane.add(scrollPaneData);
        
        // Set "Return" button activity
        // ----------------------------
        returnButton.addActionListener(a -> {
            canceled = true;
            dispose();
        });

        // Set "Enter" button activity
        // ---------------------------
        enterButton.addActionListener(a -> {
            String name = textField.getText();
            String bareName = "";  // name before dot
            int posDot = name.indexOf(".", 0);
            if (posDot > -1) {
                bareName = name.substring(0, posDot);
            }
            if (name.isBlank() 
                    || name.toLowerCase().equals(".sql") 
                    || !name.toLowerCase().endsWith(".sql")
                    || bareName.isBlank()) {
                // Script name is empty or does not equal .sql or does not end with .sql 
                message.setText(isEmpty);  
                message.setForeground(DIM_RED); // red
                setVisible(true);
            } else {
                canceled = false;
                dispose();
            }
        });

        // Enable ENTER key to save and return action
        // ------------------------------------------
        dataGlobalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("ENTER"), "enter");
        dataGlobalPanel.getActionMap().put("enter", new EnterAction());

        // Display the window
        // ------------------
        setLocation(xLocation, yLocation);
        setSize(windowWidth, windowHeight);
        //pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Inner class for ENTER key
     */
    class EnterAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            enterButton.doClick();
        }
    }
}