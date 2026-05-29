package queries;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JPing;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.ArrayList;
import javax.swing.JTextArea;

/**
 * Display list for selection SQL scripts and calls a program to run the selected script.
 *
 * @author Vladimír Župka 2016, 2026
 *
 */
public class Q_ScriptRunCall {

    AS400 remoteServer;
    String host, userName;
    Connection connnection;

    final String PARAM_SEPARATOR = ";";

    String scriptName;
    String scriptDescription;

    Path scriptIn;
    BufferedReader infileScript;

    // Area for text of the script result
    JTextArea resultTextArea = new JTextArea();

    // Name of the file containing an SQL script
    String fileName;
    // Buffer containing text of one SQL statement
    StringBuilder statementBuf;

    String[] markerValues;
    static ArrayList<String[]> markerArrayList = new ArrayList<>();
    final String PARAM_PREFIX = "--;?";

    String[] headerValues;
    static ArrayList<String[]> headerArrayList = new ArrayList<>();
    final String HEADER_PREFIX = "--;H";

    String[] totalValues;
    static ArrayList<String[]> totalArrayList = new ArrayList<>();
    final String TOTAL_PREFIX = "--;T";

    String[] patternValues;
    static ArrayList<String[]> patternArrayList = new ArrayList<>();
    final String DECIMAL_PREFIX = "--;D";

    String[] printValues;
    static ArrayList<String[]> printArrayList = new ArrayList<>();
    final String PRINT_PREFIX = "--;P";

    // Exception in title headings - array list has
    // single String elements only, not String[] elements.
    static ArrayList<String> titleArrayList = new ArrayList<>();
    final String TITLE_PREFIX = "--;t";

    static ArrayList<String[]> levelArrayList = new ArrayList<>();
    String[] levelValues;
    final String LEVEL_PREFIX = "--;L";

    String[] summaryValues;
    static ArrayList<String[]> summaryArrayList = new ArrayList<>();
    final String SUMMARY_PREFIX = "--;S";

    String[] summaryIndValues;
    static ArrayList<String[]> summaryIndArrayList = new ArrayList<>();
    final String SUM_IND_PREFIX = "--;s";

    String[] omitValues;
    static ArrayList<String[]> omitArrayList = new ArrayList<>();
    final String OMIT_PREFIX = "--;O";

    // Return code array: [0] - tag "runScript" or "runPrompt",
    // [1] - error message text
    String[] retCode = new String[2];

    /**
     * Constructor
     *
     * @param remoteServer
     */
    public Q_ScriptRunCall() {
        
        Q_Properties prop = new Q_Properties();

        host = prop.getProperty("HOST");
        userName = prop.getProperty("USER_NAME");
        // Create AS400 object for IBM i SERVER for changing Signon dialog.
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        remoteServer = new AS400(host, userName /*  , PASSWORD  */ );
        // The third parameter (password) should NOT be specified. The user must sign on.
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        remoteServer.setShowCheckboxes(false);  // No check boxes in Signon dialog
    }

    /**
     * Perform the selected script ("QUERY" or "UPDATE")
     *
     * @param selectedScript
     * @param scriptDescription
     * @return
     */
    public String[] performScript(String selectedScript, String scriptDescription) {
        if (remoteServer.isConnected()) {
            retCode[0] = "";
            retCode[1] = "! Server " + host + " reconnected.";
            return retCode;
        } else {
            // --------------------------------------------
            // First, ping on the server if connection is possible. If not, return message. 
            AS400JPing pingObj = new AS400JPing(host);
            long timeoutMilliscconds = 8000;
            pingObj.setTimeout(timeoutMilliscconds);
            if (!pingObj.ping()) {
                retCode[0] = "ERROR";
                retCode[1] = "! Server " + host + " timed out "
                        + timeoutMilliscconds + " milliseconds.";
                return retCode;
            }

            // Connect to the database server
            if (Q_ConnectDB.connection == null) {
                Q_ConnectDB.connection = Q_ConnectDB.connect();
            }

            if (Q_ConnectDB.connection == null) {
                retCode[0] = "ERROR";
                retCode[1] = "Statement performed. TADY";
                return retCode;
            }
            // --------------------------------------------

            scriptName = selectedScript.substring(0, selectedScript.indexOf("."));
            fileName = selectedScript;

            // Create path to the script file
            scriptIn = Paths.get(System.getProperty("user.dir"), "scriptfiles", fileName);
            // Create paths to text files
            Path workfilesTxt = Paths.get(System.getProperty("user.dir"), "workfiles", "Print.txt");
            Path printfilesTxt = Paths.get(System.getProperty("user.dir"), "printfiles", scriptName + ".txt");

            // Count of "UPDATE" statements
            int numberOfUpdates = 0;

            try {
                // Process the script file
                // -----------------------
                /*
          * // Prepend title lines to the result and the text files //
          * ---------------------------------------------------- // - script
          * description String title = scriptDescription + "\n\n";
          * resultTextArea.setText(title); // - date and time - localized
          * currentLocale = Locale.forLanguageTag(language); DateFormat
          * formatter = DateFormat.getDateTimeInstance(DateFormat.FULL,
          * DateFormat.DEFAULT, currentLocale); Date date = new Date(); title =
          * formatter.format(date) + "\n\n"; resultTextArea.append(title);
                 */
                ArrayList<String> lines = new ArrayList<>();
                // The first entry of the array list (and the text files)
                // is the title of the script (script description and date)
                lines.add(resultTextArea.getText());

                // Create two text files from the array list "lines"
                Files.write(workfilesTxt, lines, Charset.forName("UTF-8"), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                Files.write(printfilesTxt, lines, Charset.forName("UTF-8"), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                // Open the script file
                infileScript = Files.newBufferedReader(scriptIn, Charset.forName("UTF-8"));

                // Begin assembly of a new SQL statement
                // -------------------------------------
                statementBuf = new StringBuilder();

                // Clear marker array list
                markerArrayList = new ArrayList<>();
                // Clear header array list
                headerArrayList = new ArrayList<>();
                // Clear total array list
                totalArrayList = new ArrayList<>();
                // Clear decimal pattern arrayList
                patternArrayList = new ArrayList<>();
                // Clear print arrayList
                printArrayList = new ArrayList<>();
                // Clear title arrayList
                titleArrayList = new ArrayList<>();
                // Clear level arrayList
                levelArrayList = new ArrayList<>();
                // Clear summary arrayList
                summaryArrayList = new ArrayList<>();
                // Clear summary indicators arrayList
                summaryIndArrayList = new ArrayList<>();
                // Clear omitted column names arrayList
                omitArrayList = new ArrayList<>();

                // Read the first line of the script file
                String scriptLine = infileScript.readLine();

                // Read all lines of the script and write them to a string buffer
                while (scriptLine != null) {
                    // A semicolon is on the line and is before the first
                    // simple comment mark (--)
                    // or there is no simple comment mark (--).
                    // This ensures that this is not a semicolon belonging to
                    // a definition line specification.
                    if (scriptLine.indexOf(";") >= 0 && (scriptLine.indexOf(";") < scriptLine.indexOf("--"))
                            || scriptLine.indexOf(";") >= 0 && scriptLine.indexOf("--") == -1) {

                        // Semicolon ends the SQL statement in the script
                        // but is not allowed as part of the SQL statement itself
                        scriptLine = scriptLine.replace(";", " ");

                        // Add the input line to the statement buffer
                        statementBuf.append(scriptLine);
                        // ???? statementBuf.append("\n");

                        // Perform the SQL statement that ends with the semicolon
                        // ------------------------------------------------------
                        retCode = performSqlStatement(connnection, scriptDescription);
                        if (retCode[0].contains("UPDATE")) {
                            numberOfUpdates++;
                        }

                        // Clear the buffer for a next SQL statement
                        statementBuf.setLength(0);
                        // Clear marker array list
                        markerArrayList = new ArrayList<>();
                        // Clear header array list
                        headerArrayList = new ArrayList<>();
                        // Clear total array list
                        totalArrayList = new ArrayList<>();
                        // Clear decimal pattern arrayList
                        patternArrayList = new ArrayList<>();
                        // Clear print arrayList
                        printArrayList = new ArrayList<>();
                        // Clear title arrayList
                        titleArrayList = new ArrayList<>();
                        // Clear level arrayList
                        levelArrayList = new ArrayList<>();
                        // Clear summary arrayList
                        summaryArrayList = new ArrayList<>();
                        // Clear summary indicators arrayList
                        summaryIndArrayList = new ArrayList<>();
                        // Clear omitted column names arrayList
                        omitArrayList = new ArrayList<>();

                        // Read next line of the script file
                        scriptLine = infileScript.readLine();

                        // Continue with the next SQL statement
                        continue;
                    }
                    // Process definition lines
                    // ------------------------
                    // Line beginning with prefix "--;?" is the description
                    // of the form:
                    // --;? number; type; description; value;
                    // example:
                    // --;? 1; CHAR; Číslo zboží od:; 00001;
                    if (scriptLine.indexOf(PARAM_PREFIX) == 0) {
                        // The values are delimited by semicolons and are placed
                        // in the array "markerValues".
                        markerValues = getMarkerValues(scriptLine);
                        // Array list has as many elements (markerValues) as
                        // there is --;? lines and should be the same as
                        // there are markers in the SQL statement.
                        markerArrayList.add(markerValues);
                    }
                    // Line beginning with prefix "--;H" is the description
                    // of the form:
                    // --;H header1 ; header2 ; ...;
                    // example with three headers:
                    // --;H Číslo zboží, Název, Součet ceny,
                    if (scriptLine.indexOf(HEADER_PREFIX) == 0) {
                        // The values are delimited by semicolons and are placed
                        // in the array "headerValues".
                        headerValues = getHeaderValues(scriptLine);
                        // Array list has as many elements (headerValues) as
                        // there are --;H lines.
                        headerArrayList.add(headerValues);
                    }
                    // Line beginning with prefix "--;T" is the description
                    // of the form:
                    // --;T spaceBefore; spaceAfter; nullPrintMark;
                    // example with three headers:
                    // --;T 1; 1; -;
                    if (scriptLine.indexOf(TOTAL_PREFIX) == 0) {
                        // The values are delimited by semicolons and are placed
                        // in the array "totalValues".
                        totalValues = getTotalValues(scriptLine);
                        // Array list has as many elements (totalValues) as
                        // there are --;T lines but it should be only one.
                        // If more than one, the FIRST line will be used.
                        totalArrayList.add(totalValues);
                    }
                    // Line beginning with prefix --;D is the description of a mask
                    // formatting the output decimal number for a column name
                    if (scriptLine.indexOf(DECIMAL_PREFIX) == 0) {
                        patternValues = getPatternValues(scriptLine);
                        // Array list has as many elements (patternValues) as
                        // there are --;D lines
                        patternArrayList.add(patternValues);
                    }
                    // Line beginning with prefix --;P is the description of printing:
                    // - printer size A4 or A3
                    // - font size FSn (n is number in print points)
                    // - page orientation PORTRAIT (default) or LANDSCAPE
                    // - LMn left margin
                    // - RMn right margin
                    // - TMn top margin
                    // - BMn bottom margin
                    if (scriptLine.indexOf(PRINT_PREFIX) == 0) {
                        printValues = getPrintValues(scriptLine);
                        // Array list has as many elements (printValues) as
                        // there are --;P lines but it should be only one.
                        // If more than one, the first --;P the FIRST line will be used.
                        printArrayList.add(printValues);
                    }
                    // Line beginning with prefix --;t is the title line with
                    // possibly inserted variables &column_name for values
                    // of omitted columns
                    if (scriptLine.indexOf(TITLE_PREFIX) == 0) {
                        // Array list has as many elements (titles) as
                        // there are --;t lines
                        titleArrayList.add(getTitleValues(scriptLine));
                    }
                    // Line beginning with prefix --;L is the description of a level
                    // break
                    if (scriptLine.indexOf(LEVEL_PREFIX) == 0) {
                        levelValues = getLevelValues(scriptLine);
                        // Array list has as many elements (levelValues) as
                        // there are --;L lines
                        levelArrayList.add(levelValues);
                    }
                    // Line beginning with prefix --;S is the description of summary
                    // functions SUM, AVG, MAX, MIN, COUNT
                    if (scriptLine.indexOf(SUMMARY_PREFIX) == 0) {
                        summaryValues = getSummaryValues(scriptLine);
                        // Array list has as many elements (summaryValues) as
                        // there are --;S lines.
                        summaryArrayList.add(summaryValues);
                    }
                    // Line beginning with prefix --;s is the description of
                    // indicator texts for summary functions SUM, AVG, MAX, MIN, COUNT
                    if (scriptLine.indexOf(SUM_IND_PREFIX) == 0) {
                        summaryIndValues = getSummaryIndValues(scriptLine);
                        // Array list has as many elements (summaryIndValues) as
                        // there are --;s lines but it should be only one.
                        // If more than one, the FIRST line will be used.
                        summaryIndArrayList.add(summaryIndValues);
                    }
                    // Line beginning with prefix --;O is the description of
                    // column names to be omitted from output
                    if (scriptLine.indexOf(OMIT_PREFIX) == 0) {
                        omitValues = getOmitValues(scriptLine);
                        // Array list has as many elements (sumValues) as
                        // there are --;O lines.
                        omitArrayList.add(omitValues);
                    }
                    // Append the line to the buffer
                    statementBuf.append(scriptLine).append("\n");

                    // Read tne next line
                    scriptLine = infileScript.readLine();
                } // end while

                // Perform the last SQL statement
                // ------------------------------
                retCode = performSqlStatement(connnection, scriptDescription);
                if (retCode[0].contains("UPDATE")) {
                    numberOfUpdates++;
                }

                // Close the script file
                infileScript.close();

                // If at least one statement was UPDATE type (non-query)
                // display the whole script (with possible QUERY results)
                // at the end of multi-script processing.
                // ------------------------------------------------------
                if (numberOfUpdates > 0) {
                    ArrayList<String> lineList = null;
                    // Read all lines of the text file Print.txt into the array list
                    try {
                        lineList = (ArrayList<String>) Files.readAllLines(workfilesTxt);
                    } catch (IOException ioe) {
                        ioe.getStackTrace();
                    }
                    // Concatenate all text lines from the list obtained from the print
                    // file with new lines.
                    String printText = lineList.stream().reduce("", (a, b) -> a + b + "\n");
                    // Set the text to the resultTextArea (containing titles already)
                    resultTextArea.setText(printText);

                    // Display the result window (without column headings)
                    int nbrHdrLines = 0;
                    headerArrayList = null;
                    // Column separating spaces are empty
                    ArrayList<String> columnHeaders = new ArrayList<>();
                    new Q_PrintOneFile(scriptName, resultTextArea, nbrHdrLines, headerArrayList, printArrayList, columnHeaders);
                }
                // This is the end of script processing

            } catch (IOException ioe) {
                ioe.printStackTrace();
//            scriptListMsg.setText("IOException!");
            }
            //Q_ConnectDB.disconnect(connection);
            return retCode;
        }
    }

    /**
     *
     * @param connection
     * @param scriptDescription
     * @return
     */
    protected String[] performSqlStatement(Connection connection, String scriptDescription) {

        retCode[1] = "";

        // Set the SQL statement description
        // ---------------------------------
        String stmtDescription = "";
        // If the first line is a simple comment beginning with "--"
        // in the first position and it is not "--;",
        // the description is the text following the -- characters.
        // Otherwise the description will be empty.
        String[] lines = statementBuf.toString().split("\n", 0);
        for (String line : lines) {
            if (!line.isEmpty() && line.length() >= 3) {
                if (line.substring(0, 2).equals("--") && !line.substring(0, 3).equals("--;")) {
                    stmtDescription = line.substring(2);
                    break;
                } else {
                    stmtDescription = "";
                    break;
                }
            }
        }

        // When no markers are in the statement, perform the statement directly.
        // ---------------
        if (markerArrayList.isEmpty()) {
            // Call statement performer for query or update
            Q_ScriptRun scriptRun = new Q_ScriptRun();
            retCode = scriptRun.runScript(connection, scriptName, stmtDescription, statementBuf.toString(),
                    markerArrayList, headerArrayList, totalArrayList, patternArrayList, printArrayList,
                    titleArrayList, levelArrayList, summaryArrayList, summaryIndArrayList,
                    omitArrayList);
            // If OK set info message
            retCode[0] += "runScript"; // Append to "QUERY" or "UPDATE"
        } // When there are some markers, display prompt for parameters.
        // ---------------------------
        else {
            // First check marker definition entries.
            // If entries are less or more than 4, correct them.
            for (int idx = 0; idx < markerArrayList.size(); idx++) {
                markerValues = markerArrayList.get(idx);
                if (markerValues.length != 4) {
                    int len = Math.min(markerValues.length, 4);
                    // Create a new array
                    // Copy existing entries
                    String[] markerVals = new String[4];
                    System.arraycopy(markerValues, 0, markerVals, 0, len);
                    // Add missing missing entries with empty strings
                    for (int in = len; in < 4; in++) {
                        markerVals[in] = "";
                    }
                    markerArrayList.set(idx, markerVals);
                }
            }
            // Prompt for marker values.
            Q_PromptParameters pmt = new Q_PromptParameters();
            // Method runPrompt returns a message string (is empty if OK)
            retCode = pmt.runPrompt(connection, scriptName, stmtDescription, statementBuf.toString(),
                    markerArrayList, headerArrayList, totalArrayList, patternArrayList, printArrayList,
                    titleArrayList, levelArrayList, summaryArrayList, summaryIndArrayList,
                    omitArrayList);
            retCode[0] += "runPrompt"; // Append to "QUERY" or "UPDATE"
        }

        return retCode;
    }

    /**
     * Extract marker parameter values from the --;? comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getMarkerValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line,
        markerValues = scriptLine.substring(PARAM_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Spaces on both sides of parameters are trimmed
        for (int i = 0; i < markerValues.length; i++) {
            // Marker parameter values are trimmed due to numbers
            markerValues[i] = markerValues[i].trim();
        }
        return markerValues;
    }

    /**
     * Extract header values from the --;H comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getHeaderValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line,
        headerValues = scriptLine.substring(HEADER_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Header values are NOT trimmed, they allow spaces before and after
        // text.
        return headerValues;
    }

    /**
     * Extract values from --;T comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getTotalValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line to an auxiliary
        // array
        totalValues = scriptLine.substring(TOTAL_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Replace original array elements only if there is a value between
        // semicolons
        if (totalValues.length > 0) {
            for (int i = 0; i < totalValues.length; i++) {
                // Replace original elements by the non-empty (trimmed) values
                // in the array
                totalValues[i] = totalValues[i].trim();
            }
        }
        return totalValues;
    }

    /**
     *
     * @param scriptLine
     * @return
     */
    protected String[] getPatternValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line,
        patternValues = scriptLine.substring(DECIMAL_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Spaces on both sides of parameters are trimmed
        for (int i = 0; i < patternValues.length; i++) {
            // Marker parameter values are trimmed due to numbers
            patternValues[i] = patternValues[i].trim();
        }
        return patternValues;
    }

    /**
     * Extract values from --;P comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getPrintValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line to an auxiliary
        // array
        ArrayList<String> printParams = new ArrayList<>();
        printValues = scriptLine.substring(PRINT_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Replace original array elements only if there is a value between
        // semicolons
        if (printValues.length > 0) {
            for (String printValue : printValues) {
                // System.out.println("printValues[idx]: " + printValues[idx]);
                // Trim original elements and add to array list
                printParams.add(printValue.trim());
            }
            // 6 print parameters
            if (printValues.length < 6) {
                for (int in = 0; in < 7 - printValues.length; in++) {
                    printParams.add("");
                }
            }
        }
        printValues = printParams.toArray(printValues);
        return printValues;
    }

    /**
     *
     * @param scriptLine
     * @return
     */
    protected String getTitleValues(String scriptLine) {
        // Return the whole text of the title line
        // with possible one or more variables &column_name
        return scriptLine.substring(TITLE_PREFIX.length());
    }

    /**
     * Extract values from --;L comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getLevelValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line to an auxiliary
        // array
        ArrayList<String> levelParams = new ArrayList<>();
        levelValues = scriptLine.substring(LEVEL_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Replace original array elements only if there is a value between
        // semicolons
        if (levelValues.length > 0) {
            for (String levelValue : levelValues) {
                // Trim original elements and add to array list
                levelParams.add(levelValues[0].trim()); // level number or empty
                if (levelValues.length > 1) {
                    // Level text is not trimmed!
                    levelParams.add(levelValues[1]); // level text of separation line
                }
                if (levelValues.length > 2) {
                    levelParams.add(levelValues[2].trim()); // level column name
                }
                if (levelValues.length > 3) {
                    levelParams.add(levelValues[3].trim()); // new page flag
                }
                // There are 4 level parameters - set missing ones to empty string
                if (levelValues.length < 4) {
                    for (int in = 0; in < 5 - levelValues.length; in++) {
                        levelParams.add("");
                    }
                }
            }
        }
        levelValues = levelParams.toArray(levelValues);
        return levelValues;
    }

    /**
     * Extract values from --;S comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getSummaryValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line to an auxiliary
        // array
        ArrayList<String> sumParams = new ArrayList<>();
        summaryValues = scriptLine.substring(SUMMARY_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Replace original array elements only if there is a value between
        // semicolons
        if (summaryValues.length > 0) {
            for (String summaryValue : summaryValues) {
                // Trim original elements and add to array list
                sumParams.add(summaryValue.trim());
            }
            // 6 summary parameters - pad missing with empty strings
            if (summaryValues.length < 6) {
                for (int in = 0; in < 7 - summaryValues.length; in++) {
                    sumParams.add("");
                }
            }
        }
        summaryValues = sumParams.toArray(summaryValues);
        return summaryValues;
    }

    /**
     * Extract values from --;s comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getSummaryIndValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line to an auxiliary
        // array
        ArrayList<String> sumParams = new ArrayList<>();
        summaryIndValues = scriptLine.substring(SUM_IND_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Replace original array elements only if there is a value between
        // semicolons
        if (summaryIndValues.length > 0) {
            for (String summaryIndValue : summaryIndValues) {
                // Trim original elements and add to array list
                sumParams.add(summaryIndValue.trim());
            }
            // 5 summary parameters - pad missing with empty strings
            if (summaryIndValues.length < 5) {
                for (int in = 0; in < 6 - summaryIndValues.length; in++) {
                    sumParams.add("");
                }
            }
        }
        summaryIndValues = sumParams.toArray(summaryIndValues);
        return summaryIndValues;
    }

    /**
     * Extract values from --;O comment line
     *
     * @param scriptLine
     * @return
     */
    protected String[] getOmitValues(String scriptLine) {
        // Extract parameters separated by semicolon from the line to an auxiliary
        // array
        ArrayList<String> sumParams = new ArrayList<>();
        omitValues = scriptLine.substring(OMIT_PREFIX.length()).split(PARAM_SEPARATOR, 0);
        // Replace original array elements only if there is a value between
        // semicolons
        if (omitValues.length > 0) {
            for (String omitValue : omitValues) {
                // Trim original elements and add to array list
                sumParams.add(omitValue.trim());
            }
        }
        omitValues = sumParams.toArray(omitValues);
        return omitValues;
    }
}
