package queries;

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.swing.GroupLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/**
 * Edit and maintain SQL query scripts
 *
 * @author Vladimír Župka 2016
 *
 */
public final class Q_ScriptEditCall extends JFrame {

    Q_Properties prop;

    ResourceBundle titles;
    String titEdit, scriptNam, scriptDes, searchScript;
    ResourceBundle buttons;
    String exit, new_script, edit_sel, run_sel, del_sel;
    ResourceBundle locMessages;
    String curDir, noRowEdt, noRowRun, noRowDel, noRowSav, file, script, exists, scriptSuccess, wasDelLoc, wasCreated, wasDel, notInDir,
            wasSavedTo, ioError, inputError;
    // Object of calling class
    //static Q_Menu menu = Q_Menu.getQ_Menu();

    String[] fileNames;
    String fileName = "";
    String selFileName = "";  // file name to be selected as a new script
    int tableSelIndex = 0;
    String scriptName;
    String scriptDescription;

    // scriptNames with the key of scriptName
    TreeMap<String, String> scriptNames = new TreeMap<>();
    Map.Entry<String, String> mapEntry;

    // Variables for stream files
    Path scriptDirectoryPath;
    Path scriptOutPath;
    Path scriptIn;
    BufferedReader infileScript;
    static String language;

    // Components for scriptListGlobalPanel
    JScrollPane scrollPane;
    static JPanel scriptListMsgPanel;
    JPanel scriptListButtonPanel;

    JLabel scriptListTitle;
    JLabel scriptListMsgLabel;
    String msgText;

    JButton scriptListExitButton;
    JButton scriptListAddButton;
    JButton scriptListEdtButton;
    JButton scriptListRunButton;
    JButton scriptListDelButton;

    JTextField searchField;
    JLabel searchLabel;
    String searchText;
    String searchPattern;
    String searchWildCard;

    // Dimensions of different stage views
    final Integer scriptListGlobalWidth = 1050;
    final Integer scriptListGlobalHeight = 700;
    final Integer scriptListPanelWidth = scriptListGlobalWidth;
    final Integer scriptListPanelHeight = scriptListGlobalHeight - 210;

    final Integer scriptListWidth = scriptListPanelWidth;
    final Integer xLocation = 400;
    final Integer yLocation = 50;

    final Integer tableRowHeight = 24;
    final Integer firstTableColumnWidth = 200;
    final Integer secondTableColumnWidth = 660;

    // List (table) of records
    JTable scriptList;
    ScriptTableMouseAdapter scriptTableMouseAdapter;

    // Data model for the table
    DefaultTableModel tableModel;

    // Index of table row selected (by the user or the program)
    int scriptListIndexSel;
    // List selection model
    ListSelectionModel rowIndexList;

    Object[][] records; // table contents
    int nbrOfRows; // number of rows in table

    JPanel scriptListGlobalPanel;
    Container listContentPane;

    JMenuBar menuBar;
    JMenu helpMenu;
    JMenuItem helpMenuItemEN;
    JMenuItem helpMenuItemCZ;

    ListSelectionModel selModel;

    // Table columns for the list
    TableColumn colscriptName;
    TableColumn colQueryDesc;

    JPopupMenu tableRowPopupMenu = new JPopupMenu();
    JMenuItem editScriptMenuItem;
    JMenuItem runScriptMenuItem;

    final Color DIM_GRAY = new Color(100, 100, 100);
    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = new Color(190, 60, 50);

    Properties sysProp;

    Q_ScriptEdit scriptEdit;
    WindowScriptEditCallAdapter windowScriptEditCallAdapter;

    // Selected script file name
    String selScriptFileName;

    /**
     * Constructor
     */
    public Q_ScriptEditCall() {

        // Get or set application properties
        // ---------------------------------
        sysProp = System.getProperties();

        // Menu bar in Mac operating system will be in the system menu bar
        if (sysProp.get("os.name").toString().toUpperCase().contains("MAC")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        prop = new Q_Properties();
        language = prop.getProperty("LANGUAGE");

        menuBar = new JMenuBar();
        helpMenu = new JMenu("Help");
        helpMenuItemEN = new JMenuItem("Help English");
        helpMenuItemCZ = new JMenuItem("Nápověda česky");

        helpMenu.add(helpMenuItemEN);
        helpMenu.add(helpMenuItemCZ);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar); // In macOS on the main system menu bar above, in Windows on the window menu bar

        tableModel = new Q_TableModel();

        // Directory with script files
        scriptDirectoryPath = Paths.get(System.getProperty("user.dir"), "scriptfiles");
        // Files for storing and reading records
        scriptOutPath = Paths.get(System.getProperty("user.dir"), "scriptfiles", scriptName + ".sql");
        scriptIn = Paths.get(System.getProperty("user.dir"), "scriptfiles", scriptName + ".sql");

        Locale currentLocale = Locale.forLanguageTag(language);
        // Get resource bundle classes
        titles = ResourceBundle.getBundle("locales.L_TitleLabelBundle", currentLocale);
        buttons = ResourceBundle.getBundle("locales.L_ButtonBundle", currentLocale);
        locMessages = ResourceBundle.getBundle("locales.L_MessageBundle", currentLocale);

        scriptTableMouseAdapter = new ScriptTableMouseAdapter();

        // Localized titles
        titEdit = titles.getString("TitEdit");
        scriptNam = titles.getString("ScriptNam");
        scriptDes = titles.getString("ScriptDes");
        searchScript = titles.getString("SearchScript");

        // Localized button labels
        exit = buttons.getString("Exit");
        new_script = buttons.getString("New_script");
        edit_sel = buttons.getString("Edit_sel");
        run_sel = buttons.getString("Run_sel");
        del_sel = buttons.getString("Del_sel");
        //refresh = buttons.getString("Refresh");

        editScriptMenuItem = new JMenuItem(edit_sel);
        runScriptMenuItem = new JMenuItem(run_sel);

        scrollPane = new JScrollPane();
        scriptListMsgPanel = new JPanel();
        scriptListButtonPanel = new JPanel();

        scriptListTitle = new JLabel();
        searchField = new JTextField("");
        searchLabel = new JLabel(searchScript);
        searchLabel.setForeground(DIM_BLUE); // Dim blue

        scriptListMsgLabel = new JLabel();

        scriptListExitButton = new JButton(exit);
        scriptListExitButton.setMinimumSize(new Dimension(70, 35));
        scriptListExitButton.setMaximumSize(new Dimension(70, 35));
        scriptListExitButton.setPreferredSize(new Dimension(70, 35));

        scriptListAddButton = new JButton(new_script);
        scriptListAddButton.setMinimumSize(new Dimension(150, 35));
        scriptListAddButton.setMaximumSize(new Dimension(150, 35));
        scriptListAddButton.setPreferredSize(new Dimension(150, 35));

        scriptListEdtButton = new JButton(edit_sel);
        scriptListEdtButton.setMinimumSize(new Dimension(130, 35));
        scriptListEdtButton.setMaximumSize(new Dimension(130, 35));
        scriptListEdtButton.setPreferredSize(new Dimension(130, 35));

        scriptListRunButton = new JButton(run_sel);
        scriptListRunButton.setMinimumSize(new Dimension(130, 35));
        scriptListRunButton.setMaximumSize(new Dimension(130, 35));
        scriptListRunButton.setPreferredSize(new Dimension(130, 35));

        scriptListDelButton = new JButton(del_sel);
        scriptListDelButton.setForeground(DIM_GRAY);
        //scriptListDelButton.setMinimumSize(new Dimension(130, 35));
        //scriptListDelButton.setMaximumSize(new Dimension(130, 35));
        //scriptListDelButton.setPreferredSize(new Dimension(130, 35));

        // Empty table scriptList with data model
        scriptList = new JTable(tableModel);
        scriptList.addMouseListener(scriptTableMouseAdapter);

        // Attributes of the list table
        scriptList.setFont(new Font("Helvetica", Font.PLAIN, 13));
        scriptList.getTableHeader().setFont(new Font("Helvetica", Font.BOLD, 12));
        scriptList.getTableHeader().setPreferredSize(new Dimension(0, 26));
        scriptList.setGridColor(Color.LIGHT_GRAY);
        scriptList.setRowHeight(tableRowHeight);
        scriptList.setGridColor(Color.WHITE);

        // Behavior when manual changing column width
        scriptList.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        scriptListTitle.setText(titEdit);
        scriptListTitle.setFont(new Font("Helvetica", Font.PLAIN, 20));
        scriptListTitle.setMinimumSize(new Dimension(scriptListPanelWidth, 20));
        scriptListTitle.setPreferredSize(new Dimension(scriptListPanelWidth, 20));
        scriptListTitle.setMaximumSize(new Dimension(scriptListPanelWidth, 20));

        searchField.setMaximumSize(new Dimension(200, 25));
        searchField.setPreferredSize(new Dimension(200, 25));
        searchField.setMinimumSize(new Dimension(200, 25));

        // Scroll pane contains the scriptList table inside
        scrollPane = new JScrollPane(scriptList);
        scrollPane.setMaximumSize(new Dimension(scriptListWidth - 20, scriptListPanelHeight));
        scrollPane.setMinimumSize(new Dimension(scriptListWidth - 20, scriptListPanelHeight));
        scrollPane.setPreferredSize(new Dimension(scriptListWidth - 20, scriptListPanelHeight));
        scrollPane.setBackground(scriptList.getBackground());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Localized messages       
        curDir = locMessages.getString("CurDir");
        noRowEdt = locMessages.getString("NoRowEdt");
        noRowRun = locMessages.getString("NoRowRun");
        noRowDel = locMessages.getString("NoRowDel");
        file = locMessages.getString("File");
        wasDelLoc = locMessages.getString("WasDelLoc");
        wasDel = locMessages.getString("WasDel");
        notInDir = locMessages.getString("NotInDir");
        noRowSav = locMessages.getString("NoRowSav");
        script = locMessages.getString("Script");
        scriptSuccess = locMessages.getString("ScriptSuccess");
        exists = locMessages.getString("Exists");
        wasCreated = locMessages.getString("WasCreated");
        wasSavedTo = locMessages.getString("WasSavedTo");
        ioError = locMessages.getString("IOError");
        inputError = locMessages.getString("InputError");

        // Message panel in the list window
        scriptListMsgPanel.setMinimumSize(new Dimension(0, 30));
        scriptListMsgPanel.setPreferredSize(new Dimension(scriptListWidth, 30));
        scriptListMsgPanel.setMaximumSize(new Dimension(3000, 30));
        scriptListMsgPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        BoxLayout msgLayoutX = new BoxLayout(scriptListMsgPanel, BoxLayout.X_AXIS);
        scriptListMsgPanel.setLayout(msgLayoutX);

        // Button panel in the list window
        BoxLayout buttonLayoutX = new BoxLayout(scriptListButtonPanel, BoxLayout.X_AXIS);
        scriptListButtonPanel.setLayout(buttonLayoutX);
        scriptListButtonPanel.add(scriptListExitButton);
        scriptListButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        scriptListButtonPanel.add(scriptListAddButton);
        scriptListButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        scriptListButtonPanel.add(scriptListEdtButton);
        scriptListButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        scriptListButtonPanel.add(scriptListRunButton);
        scriptListButtonPanel.add(Box.createRigidArea(new Dimension(350, 0)));
        scriptListButtonPanel.add(scriptListDelButton);
        //scriptListButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        scriptListButtonPanel.setMinimumSize(new Dimension(scriptListPanelWidth, 50));
        scriptListButtonPanel.setPreferredSize(new Dimension(scriptListPanelWidth, 50));
        scriptListButtonPanel.setMaximumSize(new Dimension(scriptListPanelWidth, 50));

        // Set contents of the global panel in the list window
        scriptListGlobalPanel = new JPanel();
        GroupLayout scriptListGlobalPanelLayout = new GroupLayout(scriptListGlobalPanel);
        scriptListGlobalPanelLayout.setVerticalGroup(scriptListGlobalPanelLayout.createSequentialGroup()
                .addGap(5)
                .addComponent(scriptListTitle)
                .addGap(5)
                .addComponent(searchLabel)
                .addComponent(searchField)
                .addGap(5)
                .addComponent(scrollPane)
                .addComponent(scriptListButtonPanel)
                .addComponent(scriptListMsgPanel)
        );
        scriptListGlobalPanelLayout.setHorizontalGroup(scriptListGlobalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                .addComponent(scriptListTitle)
                .addComponent(searchLabel)
                .addComponent(searchField)
                .addComponent(scrollPane)
                .addComponent(scriptListButtonPanel)
                .addComponent(scriptListMsgPanel)
        );
        scriptListGlobalPanel.setLayout(scriptListGlobalPanelLayout);

        // Register HelpWindow menu item listener
        helpMenuItemEN.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Help English")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiSqlQueryDocEn.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemCZ.addActionListener((ActionEvent ae) -> {
            String command = ae.getActionCommand();
            if (command.equals("Nápověda česky")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiSqlQueryDocCz.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });

        // Column headings with text
        tableModel.addColumn(scriptNam);
        tableModel.addColumn(scriptDes);

        // Properties of table columns
        // ---------------------------
        colscriptName = scriptList.getColumnModel().getColumn(0);
        //colscriptName.setMaxWidth(firstTableColumnWidth);
        //colscriptName.setMinWidth(firstTableColumnWidth);
        colscriptName.setPreferredWidth(firstTableColumnWidth);
        colQueryDesc = scriptList.getColumnModel().getColumn(1);
        //colQueryDesc.setMaxWidth(secondTableColumnWidth);
        //colQueryDesc.setMinWidth(secondTableColumnWidth);
        colQueryDesc.setPreferredWidth(secondTableColumnWidth);

        // Initial message indicating current directory
        scriptListMsgLabel.setForeground(Color.BLACK);
        scriptListMsgLabel.setText(curDir + System.getProperty("user.dir"));
        scriptListMsgPanel.add(scriptListMsgLabel);

        // Row selection model (selection of single row)
        selModel = scriptList.getSelectionModel();
        scriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Row selection model registration
        selModel.addListSelectionListener(sl -> {
            rowIndexList = (ListSelectionModel) sl.getSource();
            scriptListIndexSel = rowIndexList.getLeadSelectionIndex();

            if (!rowIndexList.isSelectionEmpty()) {
                scriptListIndexSel = rowIndexList.getLeadSelectionIndex();
                selScriptFileName = (String) records[scriptListIndexSel][0];
            } else { // No row was selected
                scriptListIndexSel = -1;
            }
        });

        // Register window listener
        // ------------------------
        windowScriptEditCallAdapter = new WindowScriptEditCallAdapter();
        this.addWindowListener(windowScriptEditCallAdapter);

        // Set Search field activity
        // -------------------------
        searchField.addActionListener(a -> {
            // Read records from database and put it into a list
            msgText = readInputFiles();
            readLinesForScriptList();
            scriptListMsgLabel.setText("");
            scriptListMsgPanel.add(scriptListMsgLabel);
            this.setVisible(true);
        });

        // Set Return button listener (return to the previous window)
        // --------------------------
        scriptListExitButton.addActionListener(a -> {
            this.dispose();
        });

        // Set Add button listener (on mouse click)
        // ----------------------------------------
        scriptListAddButton.addActionListener(a -> {
            // Prompt for a new script name in a dialog
            Q_PromptAddScript pmtAdd = new Q_PromptAddScript(this);
            fileName = pmtAdd.textField.getText();  // May be blank if the dialog was canceled
            // If the dialog was not canceled, continue
            if (!pmtAdd.canceled) {
                // The dialog was entered. Continue in creating a new file
                String scriptFileName = fileName;
                try {
                    // Create a new empty file with the script name entered in the dialog
                    Files.createFile(Paths.get("scriptfiles" + "/" + fileName));
                    // Read all scripts (no script added yet)
                    selFileName = "";
                    readInputFiles();
                    readLinesForScriptList();
                    // Read all scripts with new script highlighted and visible in the scrollPane 
                    selFileName = fileName;
                    readInputFiles();
                    readLinesForScriptList();
                    Rectangle cellRect = scriptList.getCellRect(tableSelIndex, 0, true); // new row will be visible
                    scriptList.scrollRectToVisible(cellRect);
                    scriptListMsgLabel.setText(script + scriptFileName + wasCreated);
                    scriptListMsgLabel.setForeground(DIM_BLUE); // blue
                    scriptListMsgPanel.add(scriptListMsgLabel);
                    this.setVisible(true);
                } catch (IOException exc) {
                    // File already exists.
                    readInputFiles();
                    readLinesForScriptList();
                    scriptListMsgLabel.setText(file + scriptFileName + exists);
                    scriptListMsgLabel.setForeground(DIM_RED); // red
                    scriptListMsgPanel.add(scriptListMsgLabel);
                    this.setVisible(true);
                }
            }
        });

        // Set Edit button listener (on mouse click)
        // -----------------------------------------
        scriptListEdtButton.addActionListener(a -> {
            scriptListMsgPanel.removeAll();
            scriptListMsgPanel.repaint();
            if (rowIndexList != null) { // row index not empty
                if (scriptListIndexSel >= 0) {
                    scriptListIndexSel = rowIndexList.getLeadSelectionIndex();
                    // Get index and seq. number of the selected row
                    selScriptFileName = (String) records[scriptListIndexSel][0];
                    // Call script editing program
                    JTextArea textArea = new JTextArea();
                    JTextArea textArea2 = new JTextArea();
                    scriptEdit = new Q_ScriptEdit(this, textArea, textArea2, selScriptFileName, "rewritePcFile");
                } else {
                    scriptListMsgLabel.setText(noRowEdt);
                    scriptListMsgLabel.setForeground(DIM_RED); // red
                    scriptListMsgPanel.add(scriptListMsgLabel);
                    this.setVisible(true);
                }
            } else {
                scriptListMsgLabel.setText(noRowEdt);
                scriptListMsgLabel.setForeground(DIM_RED); // red
                scriptListMsgPanel.add(scriptListMsgLabel);
                this.setVisible(true);
            }
        });

        // Set Run button listener (on mouse click)
        // ----------------------------------------
        scriptListRunButton.addActionListener(a -> {
            scriptListMsgPanel.removeAll();
            scriptListMsgPanel.repaint();
            runScript();
        });

        // Set Delete button listener
        // --------------------------
        scriptListDelButton.addActionListener(a -> {
            // System.out.println("Row Index List: " + rowIndexList);
            // If the list is not empty
            if (rowIndexList != null) {
                if (scriptListIndexSel >= 0) {
                    // Get sequential number from selected row
                    scriptListIndexSel = rowIndexList.getLeadSelectionIndex();
                    selScriptFileName = (String) records[scriptListIndexSel][0];
                    String selPrintFileName = selScriptFileName.substring(0, selScriptFileName.indexOf(".")) + ".txt";
                    // Clear message panel
                    scriptListMsgPanel.removeAll();
                    scriptListMsgPanel.repaint();
                    String messageText;
                    // Delete the file from local directory
                    Path scriptDelPath = Paths.get(System.getProperty("user.dir"), "scriptfiles", selScriptFileName);
                    Path printDelPath = Paths.get(System.getProperty("user.dir"), "printfiles", selPrintFileName);
                    try {
                        Files.delete(scriptDelPath);  // Delete script file
                        Files.delete(printDelPath);   // Delete also print text file
                        // Remove line with selected sequential number from the map
                        scriptNames.remove(selScriptFileName);
                        if (nbrOfRows == 1) {
                            tableModel.removeRow(0);  // remove the last row from table
                        }
                        messageText = file + selScriptFileName + wasDelLoc;
                        scriptListMsgLabel.setText(messageText);
                        scriptListMsgLabel.setForeground(DIM_BLUE); // blue
                        scriptListMsgPanel.add(scriptListMsgLabel);
                    } catch (IOException ioe) {
                        messageText = ioError + ioe.getLocalizedMessage();
                        scriptListMsgLabel.setText(messageText);
                        scriptListMsgLabel.setForeground(DIM_RED); // red
                        System.out.println(messageText);
                    }
                    readInputFiles();  // Read the files again for refresh
                    readLinesForScriptList();  // and refresh the table view
                    searchField.setText("");  // erase search field
                    this.setVisible(true);
                } else {
                    scriptListMsgLabel.setText(noRowDel);
                    scriptListMsgLabel.setForeground(DIM_RED); // red
                    scriptListMsgPanel.add(scriptListMsgLabel);
                    this.setVisible(true);
                }
            } else {
                scriptListMsgLabel.setText(noRowDel);
                scriptListMsgLabel.setForeground(DIM_RED); // red
                scriptListMsgPanel.add(scriptListMsgLabel);
                this.setVisible(true);
            }
        });

        // Set Edit script menu item listener
        // ----------------------------------
        editScriptMenuItem.addActionListener(a -> {
            scriptListMsgPanel.repaint();
            // Call script editing program
            JTextArea textArea = new JTextArea();
            JTextArea textArea2 = new JTextArea();
            scriptEdit = new Q_ScriptEdit(Q_ScriptEditCall.this, textArea, textArea2, selScriptFileName, "rewritePcFile");
        });

        // Set Run script menu item listener
        // ---------------------------------
        runScriptMenuItem.addActionListener(a -> {
            scriptListMsgPanel.removeAll();
            scriptListMsgPanel.repaint();
            runScript();
        });

        // Read script files from directory "scriptfiles" 
        // and puts data in tree map "scriptNames" (String, StringBuilder)
        scriptListMsgPanel.removeAll();
        msgText = readInputFiles();
        scriptListMsgLabel.setText(msgText);
        scriptListMsgPanel.add(scriptListMsgLabel);

        if (msgText.isEmpty()) {
            ///scriptListMsgLabel.setText(curDir + System.getProperty("user.dir"));
            scriptListMsgLabel.setForeground(DIM_BLUE);
        } else {
            scriptListMsgLabel.setText(msgText);
            scriptListMsgLabel.setForeground(DIM_RED);
        }
        scriptListMsgPanel.add(scriptListMsgLabel);

        // Prepare scriptList table for display
        // ------------------------------------
        // Read all records from the input file and put its data into the corresponding rows/columns.
        readLinesForScriptList();

        // Activate content of the window
        // ------------------------------
        listContentPane = this.getContentPane();
        // ( top, left, bottom, right )
        scriptListGlobalPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        listContentPane.add(scriptListGlobalPanel);

        setSize(scriptListGlobalWidth, scriptListGlobalHeight);
        setSize(scriptListGlobalWidth, scriptListGlobalHeight);
        this.setLocation(xLocation, yLocation);
        //pack();
        this.setVisible(true);
        //this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Read all keys (script names) and values (script data) from the tree map "scriptNames" and place them into array
     * "records" to fill the JTable in the script list
     *
     * @return int - last index of the array of table records
     */
    protected int readLinesForScriptList() {
        // Array of rows and columns with values for the script list table
        records = new Object[nbrOfRows][2];

        // If scriptNames TreeMap is not empty, fill the list with these scriptNames
        if (!scriptNames.isEmpty()) {
            int rows = 0;

            // Read first line
            mapEntry = scriptNames.firstEntry();
            while (mapEntry != null) {
                // Add record data from scriptNames to the array
                records[rows][0] = mapEntry.getKey();
                records[rows][1] = mapEntry.getValue();
                if (mapEntry.getKey().equals(selFileName)) {
                    tableSelIndex = rows;  // get index to programmatically select the table row
                }
                // Read next line from the file
                mapEntry = scriptNames.higherEntry(mapEntry.getKey());
                rows++;
            }
            // Fill table model with data from records array
            // Delete table rows
            tableModel.setRowCount(0);
            // Fill table with data from the array: records[][]
            for (int i = 0; i < nbrOfRows; i++) {
                tableModel.addRow(records[i]);
            }
            // Force indexed row selected
            if (nbrOfRows != 0) {
                //scriptList.setRowSelectionInterval(tableSelIndex, tableSelIndex); 
            }
        }
        // This is the row index of the last record in the list
        return scriptNames.size();
    }

    /**
     * Reads script files from directory "scriptfiles" and puts data to tree map "scriptNames" (String, StringBuilder)
     *
     * @return
     */
    protected String readInputFiles() {
        // Prepare list (tree map) of queries from script files placed in directory "scriptfiles"
        scriptNames.clear();
        try {
            // Read script file names from the directory "scriptfiles" in the window list of queries
            if (Files.isDirectory(scriptDirectoryPath)) {
                fileNames = scriptDirectoryPath.toFile().list();
                nbrOfRows = 0;
                if (fileNames.length != 0) {
                    scriptNames.clear();
                    searchText = searchField.getText().toUpperCase();

                    ///// Prepare wild card with * and ? for searching files
                    ///searchPattern = searchText;
                    ///if (searchText.isEmpty()) {
                    ///    searchPattern = "*";
                    ///}
                    ///searchWildCard = searchPattern.replace("*", ".*");
                    ///searchWildCard = searchWildCard.replace("?", ".");

                    // Process list of script files
                    //for (String file_name : fileNames) {
                    for (int idx = 0; idx < fileNames.length; idx++) {
                        ///if (fileNames[idx].toUpperCase().equals(selFileName.toUpperCase())) {
                        ///    tableSelIndex = idx;
                        ///}
                        // Get only files that conform to the search text.
                        ///if (fileNames[idx].toUpperCase().matches(searchWildCard.toUpperCase())) {
                        if (fileNames[idx].toUpperCase().contains(searchText.toUpperCase())) {
                            // Create path to the script file
                            scriptIn = Paths.get(System.getProperty("user.dir"), "scriptfiles", fileNames[idx]);
                            // Open the script file
                            infileScript = Files.newBufferedReader(scriptIn, Charset.forName("UTF-8"));

                            // If the first line is a simple comment beginning with "--" in the first position, and it is not "--;",
                            // the description is the text following the -- characters.
                            // Otherwise the description is empty.
                            String scriptLine = infileScript.readLine();
                            while (scriptLine != null) {
                                if (!scriptLine.isEmpty() && scriptLine.length() >= 3) {
                                    if (scriptLine.substring(0, 2).equals("--")
                                            && !scriptLine.substring(0, 3).equals("--;")) {
                                        scriptDescription = scriptLine.substring(2);
                                        break;
                                    } else {
                                        scriptDescription = "";
                                        break;
                                    }
                                }
                                scriptLine = infileScript.readLine();
                            }
                            if (scriptLine == null) {
                                scriptDescription = "";
                            }

                            // Close the script file
                            infileScript.close();
                            // Put the file name in the tree map
                            scriptNames.put(fileNames[idx], scriptDescription);
                            nbrOfRows++;
                        }
                    }
                }
            }
            return "";
        } catch (IOException exc) {
            exc.printStackTrace();
            String message = inputError + fileName;
            System.out.println(message);
            return message;
        }
    }

    /**
     * Table model for the scriptList (JTable)
     */
    class Q_TableModel extends DefaultTableModel {

        // Determines type of data in the cell
        @Override
        public Class<? extends Object> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            // Determines what cells are editable
            // The data/cell address is constant,
            // no matter where the cell appears on screen.
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return records[row][col];
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            // System.out.println("setValueAt: (" + row + "," + col + "): " + value);
            records[row][col] = value;
        }
    }

    /**
     * Run selected script (on context menu item
     */
    void runScript() {
        scriptListMsgPanel.removeAll();
        if (rowIndexList != null) { // row index not empty
            // A table row was selected
            if (scriptListIndexSel >= 0) {
                scriptListIndexSel = rowIndexList.getLeadSelectionIndex();
                // Get script name and desctiption from the selected row
                selScriptFileName = (String) records[scriptListIndexSel][0];
                scriptDescription = (String) records[scriptListIndexSel][1];
                // Perform the script
                Q_ScriptRunCall scriptRunCall = new Q_ScriptRunCall();
                scriptRunCall.retCode = scriptRunCall.performScript(selScriptFileName, scriptDescription);
                if (scriptRunCall.retCode[0].contains( "ERROR")) {
                    scriptListMsgLabel.setText(scriptRunCall.retCode[1]);
                    scriptListMsgPanel.add(scriptListMsgLabel);
                    scriptListMsgLabel.setForeground(DIM_RED); // red
                    setVisible(true); // This message made visible in Q_ScriptEditCall window
                    repaint();
                    toFront(); // Brings editor window to the front and may make it the focused window
                } else {
                    scriptListMsgLabel.setText(scriptRunCall.retCode[1]);
                    scriptListMsgPanel.add(scriptListMsgLabel);
                    scriptListMsgLabel.setForeground(DIM_BLUE); // blue
                    setVisible(true); // This message made visible in Q_ScriptEditCall window
                    repaint();
                    toFront(); // Brings editor window to the front and may make it the focused window
                }
            } else {
                scriptListMsgLabel.setText(noRowRun);
                scriptListMsgLabel.setForeground(DIM_RED); // red
                scriptListMsgPanel.add(scriptListMsgLabel);
                this.setVisible(true);
                repaint();
                toFront(); // Brings editor window to the front and may make it the focused window
            }
        } else {
            scriptListMsgLabel.setText(noRowRun);
            scriptListMsgLabel.setForeground(DIM_RED); // red
            scriptListMsgPanel.add(scriptListMsgLabel);
            this.setVisible(true);
            repaint();
            toFront(); // Brings editor window to the front and may make it the focused window
        }
    }


/**
 * Launch script editor on double click
 */
class ScriptTableMouseAdapter extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
            // Right click - show context menu
            tableRowPopupMenu.removeAll();
            tableRowPopupMenu.add(editScriptMenuItem);
            tableRowPopupMenu.add(runScriptMenuItem);
            tableRowPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        } else if (mouseEvent.getClickCount() == 2) {
            // On double click on a table row - call Q_ScriptEdit for the selected file
            scriptListMsgPanel.removeAll();
            scriptListMsgPanel.repaint();
            // Call script editing program
            JTextArea textArea = new JTextArea();
            JTextArea textArea2 = new JTextArea();
            scriptEdit = new Q_ScriptEdit(Q_ScriptEditCall.this, textArea, textArea2, selScriptFileName, "rewritePcFile");
        }
    }
}

/**
 * Window adapter closes the Q_ScriptEdit and also this window.
 */
class WindowScriptEditCallAdapter extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent we) {
        Q_ScriptEditCall.this.dispose();
        if (scriptEdit.columnLists != null) {
            scriptEdit.columnLists.dispose();
        }
        if (scriptEdit != null) {
            scriptEdit.dispose();
        }
    }
}
}
