package com.lcoprobe.acbs.utils;

public final class Constants {

    // Define constants for the various test and response conditions
    public static final String PROBE_HOST = "lcoProbe.host";
    public static final String PROBE_ENVIRONMENT = "lcoProbe.environment";
    public static final String PROBE_USER_NAME = "lcoProbe.userName";
    public static final String PROBE_SERVER_NAME = "lcoProbe.serverName";
    public static final String PROBE_PASSWORD = "lcoProbe.password";
    public static final String PROBE_TIMEOUT = "lcoProbe.timeout";


    public static final String ECHO_CONNECT = "### CONNECTED OK";
    public static final String ECHO_DISCONNECT = "### DISCONNECTED OK";
    public static final String ECHO_STOPPED = "### CONNECTED FAILED (20)";
    public static final String ECHO_BAD_AUTH = "### CONNECTED FAILED (6)";
    public static final String ECHO_NOT_IMPLEMENTED = "Failed to receive with error code 28!";
    public static final String ECHO_SENT = "*** Sent";
    public static final String ECHO_RECEIVED = "** Received";

    public static final String STATUS_BAD_AUTH = "INVALID CREDENTIALS";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_NOT_IMPLEMENTED = "NOT IMPLEMENTED";
    public static final String STATUS_TIMEOUT = "TIME OUT";
    public static final String ERROR = "Error";

}
