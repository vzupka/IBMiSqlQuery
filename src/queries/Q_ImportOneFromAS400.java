package queries;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400FTP;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.FTP;
import java.io.IOException;

/**
 * Transfers ONE file from IBM i (IFS directory) to local directory "scriptfiles".
 *
 * @author Vladimír Župka 2016
 *
 */
public class Q_ImportOneFromAS400 {
    static Path outPath = Paths.get(System.getProperty("user.dir"), "scriptfiles");
    static ResourceBundle locMessages;
    static String language;
    static String host;
    static String userName;
    static String ifsDirectory;
    static AS400 as400Host;
    static AS400FTP client;
    static String[] retCode;
    static String file, wasImported, toDir, notFoundInDir;

    /**
     * Obtains connection to IBM i and creates an FTP client. Then it transfers
     * the script file from the IFS directory given in parameters to local directory "scriptfiles".
     *
     * @param scriptFileName
     * name of the file to transfer
     * @return retCode String array with 2 elements:
     * 0 = POSITIVE/ERROR,
     * 1 = Error message
     */
    public static String[] transferOneFromAS400(String scriptFileName) {
        Q_Properties prop = new Q_Properties();
        language = prop.getProperty("LANGUAGE");
        host = prop.getProperty("HOST");
        userName = prop.getProperty("USER_NAME");
        ifsDirectory = prop.getProperty("IFS_DIRECTORY");

        // Append forward slash if not not present at the end of the path
        int len = ifsDirectory.length();
        if (len == 0) {
            ifsDirectory = "/";
            len = 1;
        }
        if (!ifsDirectory.substring(len - 1, len).equals("/")) {
            ifsDirectory += "/";
        }
        Locale currentLocale = Locale.forLanguageTag(language);
        locMessages = ResourceBundle.getBundle("locales.L_MessageBundle", currentLocale);
        // Localized messages
        file = locMessages.getString("File");
        wasImported = locMessages.getString("WasImported");
        toDir = locMessages.getString("ToDir");
        notFoundInDir = locMessages.getString("NotFoundInDir");
        // Return code has 2 parts: 1. incicator "ERROR" / "POSITIVE", 2. message text
        retCode = new String[2];

        // Try ping on the server if connection is possible. If not, return message.
        AS400JPing pingObj = new AS400JPing(host);
        long timeoutMilliscconds = 8000;
        pingObj.setTimeout(timeoutMilliscconds);
        if (!pingObj.ping()) {
            retCode[0] = "ERROR";
            retCode[1] = "! Server " + host + " timed out "
                    + timeoutMilliscconds + " milliseconds.";
            return retCode;
        }
        // Get access to AS400
        as400Host = new AS400(host, userName);
        // Create an FTP client
        client = new AS400FTP(as400Host);

        // Transfer script file from the local directory to IFS directory using FTP
        try {
            outPath = Paths.get(System.getProperty("user.dir"), "scriptfiles", scriptFileName);
            client.setDataTransferType(FTP.BINARY);
            // FTP get
            client.get(ifsDirectory + scriptFileName, outPath.toString());
            retCode[0] = "POSITIVE";
            retCode[1] = file + scriptFileName + wasImported + ifsDirectory + toDir;
            return retCode;
        } catch (IOException e) {
            retCode[0] = "ERROR";
            retCode[1] = file + scriptFileName + notFoundInDir + ifsDirectory;
            return retCode;
        }
    }
}
