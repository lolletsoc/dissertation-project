
package com.fyp.resilience;

import java.util.concurrent.TimeUnit;

/**
 * Contains application-specific Constants.
 */
public final class Constants {

    public static final long MAXIMUM_BACKOFF = TimeUnit.MINUTES.toMillis(10);
    public static final String WIDERST_DOWNLOAD_URL = "http://resilience-fyp.appspot.com/serveWhole?datawholekeyparam=";
    public static final int MAX_SWARM_CLIENT_RETRIES = 3;
    public static final int CLIENT_DATA_PORT = 6050;
    public static final int CLIENT_VERIFICATION_PORT = 6060;
}
