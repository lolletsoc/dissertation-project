
package com.fyp.resilience.util;

import java.io.DataOutputStream;
import java.io.IOException;

public final class ConnectionUtils {

    public static void writeByteArrayToStreamWithLengthPrefix(final byte[] message, final DataOutputStream outStream)
            throws IOException {
        outStream.writeInt(message.length);
        outStream.write(message);
    }
}
