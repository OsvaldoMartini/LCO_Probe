package com.lcoprobe.acbs.wrapper;

import com.lcoprobe.acbs.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Probes the LANSA LCO listener to establish if the listener is active or not.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
@Configuration
public class LCOEchoWrapper {
    private static Logger logger = LoggerFactory.getLogger(LCOEchoWrapper.class);

    Properties properties;

    //Globals variables
    private String displayName;
    private String host;
    private String user;
    private String pswd;
    private Long timeout = Long.MAX_VALUE;
    private Optional<String> profile = Optional.ofNullable(null);
    private String[] command = new String[8];

    // Debug mode
    private boolean debug = false;

    private StringBuilder sb = new StringBuilder(); //unsynchronized

    /**
     * Default constructor.
     */
    public LCOEchoWrapper() {
    }

    /**
     * It loads all properties from "/Conf" folder
     *
     * @param args arguments from application start
     */
    public void loadConfig(String... args) {

      checkEnvironmentPath();

        boolean serveLocal = false;
        boolean oneTimeExec = false;
        boolean reWriteProps = false;
        if (args.length == 0) {
            logger.info("Serve Local activated");
            serveLocal = true;
        } else if (args.length == 4 || args.length == 0) {
            logger.info("Usage: One Time execution");
            oneTimeExec = true;
        } else if (args.length == 5) {
            logger.info("Usage: LCOEchoWrapper <host> <user> <password> <timeout>");
            logger.info("Profile Active: {}", args[4]);
            serveLocal = true;
            reWriteProps = true;
            this.profile = Optional.of(args[4]);
        }

        // loads all Properties
        loadProperties();

        LCOEchoWrapper lco = null;
        if (serveLocal) {

            if (reWriteProps) {
                getProperties().setProperty(Constants.PROBE_HOST, args[0]); //host
                getProperties().setProperty(Constants.PROBE_HOST, args[0]); //host
                getProperties().setProperty(Constants.PROBE_USER_NAME, args[1]); //user
                getProperties().setProperty(Constants.PROBE_PASSWORD, args[2]); //pwd
                getProperties().setProperty(Constants.PROBE_TIMEOUT, args[3]); //timeout
            }

            setDisplayName(getProperties().containsKey("lcoProbe.displayName") ? getProperties().getProperty("lcoProbe.displayName") : "");
            setHost(getProperties().containsKey(Constants.PROBE_HOST) ? getProperties().getProperty(Constants.PROBE_HOST) : "");
            setUser(getProperties().containsKey(Constants.PROBE_USER_NAME) ? getProperties().getProperty(Constants.PROBE_USER_NAME) : "");
            setPswd(getProperties().containsKey(Constants.PROBE_PASSWORD) ? getProperties().getProperty(Constants.PROBE_PASSWORD) : "");
            setTimeout(getProperties().containsKey(Constants.PROBE_TIMEOUT) ? Long.parseLong(getProperties().getProperty(Constants.PROBE_TIMEOUT)) : Long.MAX_VALUE);
            boolean pDebug = false;

            if (getProperties().containsKey("lcoProbe.debug") && getProperties().getProperty("lcoProbe.debug").equalsIgnoreCase("true")) {
                pDebug = true;
            }

            lco = new LCOEchoWrapper(getHost(), getUser(), getPswd(), getTimeout(), profile, properties, pDebug);
        } else {
            lco = new LCOEchoWrapper(args[0], args[1], args[2], Long.parseLong(args[3]), profile, properties, true);

        }

        printOutput(lco, oneTimeExec);
    }

    private void checkEnvironmentPath() {
        String path = System.getenv("PATH");
        if (!path.toUpperCase().contains("LANSA\\CONNECT")) {
            logger.error("Environment variable 'PATH' must contain the LANSA Connect folder e.g. C:\\Program Files\\ACBS\\LANSA\\Connect.");
            System.exit(-1);
        }
    }

    private void printOutput(LCOEchoWrapper lco, boolean oneTimeExec) {
        String statusResult = lco.lcoEcho();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        // Produce output to simulate the JSP output that will be produced
        System.out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        System.out.println("<lco>");
        System.out.println("\t<lco-host>" + lco.getHost() + "</lco-host>");
        System.out.println("\t<lco-user>" + lco.getUser() + "</lco-user>");
        System.out.println("\t<lco-timeout>" + lco.getTimeout() + "</lco-timeout>");
        System.out.println("\t<lco-status>" + statusResult + "</lco-status>");
        System.out.println("\t<lco-time>" + sdf.format(Calendar.getInstance().getTime()) + "</lco-time>");
        System.out.println("</lco>");
        if (oneTimeExec) {
            System.exit(-1);
        }
    }

    /**
     * loads all Properties
     */
    private void loadProperties() {
        // Read in the LCO Probe properties file
        try (InputStream input = new FileInputStream(new File(".").getCanonicalPath() + File.separator + "conf" + File.separator + "lco-probe.properties")) {
            Properties props = new Properties();
            // load a properties file
            props.load(input);
            logger.info("Fetched LCOProbe properties");
            setProperties(props);
        } catch (IOException ex) {
            logger.error("Could not load properties file: ".concat(ex.getMessage()));
            System.exit(-1);
        }
    }

    /**
     * loads all Commands
     */
    private void loadCommands() {
        String activeProfile = this.profile.isPresent() ? this.profile.get() : "lcoechotp";

        this.command[0] = this.command[0] == null ? "java.exe" : this.command[0];
        this.command[1] = this.command[1] == null ? "-jar" : this.command[1];
        this.command[2] = this.command[2] == null ? this.properties.getProperty("lcoProbe.pathConnect") : this.command[2];
        this.command[3] = this.command[3] == null ? this.properties.getProperty(Constants.PROBE_HOST) : this.command[3];
        this.command[4] = this.command[4] == null ? activeProfile : this.command[4];
        this.command[5] = this.command[5] == null ? this.properties.getProperty(Constants.PROBE_USER_NAME) : this.command[5];
        this.command[6] = this.command[6] == null ? this.properties.getProperty(Constants.PROBE_PASSWORD) : this.command[6];
        this.command[7] = this.command[7] == null ? "testconn" : this.command[7];
        this.timeout = this.timeout == Long.MAX_VALUE ? Long.parseLong(this.properties.getProperty(Constants.PROBE_TIMEOUT)) : this.timeout;
    }


    /**
     * Constructor.
     *
     * @param pHost       LCO host name.
     * @param pUser       Service user account.
     * @param pPasswd     Service user password.
     * @param pTimeout    Service time out.
     * @param pProfile    Active profile
     * @param pProperties Application properties
     * @param pDebug      Debug mode
     */
    public LCOEchoWrapper(String pHost, String pUser, String pPasswd, Long pTimeout, Optional<String> pProfile, Properties pProperties, boolean pDebug) {

        // Accept the parameters

        this.host = pHost;
        this.user = pUser;
        this.pswd = pPasswd;
        this.timeout = pTimeout;
        this.profile = pProfile;
        this.properties = pProperties;
        this.debug = pDebug;

        // Build the LCO echo command string

        this.command[0] = "java.exe";
        this.command[1] = "-jar";
        this.command[2] = this.properties.getProperty("lcoProbe.pathConnect");
        this.command[3] = this.host;
        this.command[4] = this.profile.isPresent() ? this.profile.get() : "lcoechotp";
        this.command[5] = this.user;
        this.command[6] = this.pswd;
        this.command[7] = "testconn";
    }

    /**
     * Constructor for non-debug mode
     *
     * @param pHost       LCO host name.
     * @param pUser       Service user account.
     * @param pPasswd     Service user password.
     * @param pTimeout    Service time out.
     * @param pProfile    Active profile
     * @param pProperties Application properties
     */
    public LCOEchoWrapper(String pHost, String pUser, String pPasswd, Long pTimeout, Optional<String> pProfile, Properties pProperties) {

        // Call default constructor
        this(pHost, pUser, pPasswd, pTimeout, pProfile, pProperties, false);
    }

    /**
     * Run the LCO echo command.
     *
     * @return Listener status
     */
    public String lcoEcho() {
        try {
            // Status conditions
            boolean stsAuth;
            boolean stsConn;
            boolean stsDisc;
            boolean stsMsng;
            boolean stsSent;
            boolean stsRcvd;
            boolean stsStop;

            // Reset the status flags
            stsAuth = stsConn = stsDisc = stsRcvd = stsSent = stsStop = stsMsng = false;

            // Check cmds from properties
            loadCommands();

            // Execute the LCO echo command
            Process p = Runtime.getRuntime().exec(this.command);

            // Set a timer to interrupt the process if it does not return within the timeout period
            Long tTimeout = getTimeout() != null ? getTimeout() : this.timeout.longValue();
            Timer timer = new Timer();
            timer.schedule(new InterruptScheduler(Thread.currentThread()), tTimeout);

            try {
                p.waitFor();
            } catch (InterruptedException e) {
                // Stop the process from running
                p.destroy();
                logger.error("InterruptedException -> An error has occurred: ", e);
                Thread.currentThread().interrupt();
                return Constants.STATUS_TIMEOUT;
            } finally {
                // Stop the timer
                timer.cancel();
            }

            // Read back the results from stdError
            BufferedReader stdResp = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String lineError = null;
            while ((lineError = stdError.readLine()) != null) {

                // Build the string buffer in debug mode only
                if (this.debug) {
                    sb.append(lineError + "\n");
                }

                // Parse the read line

                if (lineError.indexOf(Constants.ERROR) != -1) {
                    logger.error(lineError);
                    break;
                }
            }
            String lineResp = null;
            while ((lineResp = stdResp.readLine()) != null) {

                // Build the string buffer in debug mode only
                if (this.debug) {
                    sb.append(lineResp + "\n");
                }

                // Parse the read line
                if (lineResp.indexOf(Constants.ECHO_BAD_AUTH) != -1) {
                    logger.info(Constants.ECHO_BAD_AUTH);
                    stsAuth = true;
                } else if (lineResp.indexOf(Constants.ECHO_STOPPED) != -1) {
                    logger.info(Constants.ECHO_STOPPED);
                    stsStop = true;
                } else if (lineResp.indexOf(Constants.ECHO_NOT_IMPLEMENTED) != -1) {
                    logger.info(Constants.ECHO_NOT_IMPLEMENTED);
                    stsMsng = true;
                } else if (lineResp.indexOf(Constants.ECHO_CONNECT) != -1) {
                    logger.info(Constants.ECHO_CONNECT);
                    stsConn = true;
                } else if (lineResp.indexOf(Constants.ECHO_SENT) != -1) {
                    logger.info(Constants.ECHO_SENT);
                    stsSent = true;
                } else if (lineResp.indexOf(Constants.ECHO_RECEIVED) != -1) {
                    logger.info(Constants.ECHO_RECEIVED);
                    stsRcvd = true;
                } else if (lineResp.indexOf(Constants.ECHO_DISCONNECT) != -1) {
                    logger.info(Constants.ECHO_DISCONNECT);
                    stsDisc = true;
                }
            }

            // Check the results
            if (stsAuth) {
                logger.info(Constants.STATUS_BAD_AUTH);
                return Constants.STATUS_BAD_AUTH;
            } else if (stsStop) {
                logger.info(Constants.STATUS_STOPPED);
                return Constants.STATUS_STOPPED;
            } else if (stsMsng) {
                logger.info(Constants.STATUS_NOT_IMPLEMENTED);
                return Constants.STATUS_NOT_IMPLEMENTED;
            } else if (stsConn && stsSent && stsRcvd && stsDisc) {
                logger.info(Constants.STATUS_RUNNING);
                return Constants.STATUS_RUNNING;
            } else {
                logger.info(Constants.STATUS_FAILED);
                return Constants.STATUS_FAILED;
            }
        } catch (Exception e) {
            logger.error("Exception -> An error has occurred: ", e);
            Thread.currentThread().interrupt();
            logger.info(Constants.STATUS_FAILED);
            return Constants.STATUS_FAILED;
        }
    }


    /**
     * Interrupts the active thread after a pre-determined timeout.
     *
     * @author Osvaldo Martini
     * @version 1.0
     */
    private class InterruptScheduler extends TimerTask {
        Thread target = null;

        public InterruptScheduler(Thread target) {
            this.target = target;
        }

        @Override
        public void run() {
            target.interrupt();
        }
    }

    /**
     * Get the supplied host name.
     *
     * @return Supplied host name.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Get the supplied user name.
     *
     * @return Supplied user name.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Get the timeout value as a string.
     *
     * @return Timeout value string.
     */
    public Long getTimeout() {
        return this.timeout;
    }

    /**
     * Get the lcoecho test result output string buffer (in debug mode).
     *
     * @return lcoecho.exe command output
     */
    public String getEchoResult() {
        return this.sb.toString();
    }

    /**
     * Get the properties.
     *
     * @return properties value Properties.
     */
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Get the displayName.
     *
     * @return displayName value string.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPswd(String pswd) {
        this.pswd = pswd;
    }

    public String getPswd() {
        return pswd;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }
}