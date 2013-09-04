
package com.fyp.resilience.interfaces;

import com.google.protobuf.GeneratedMessage;

public interface Messagable<T extends GeneratedMessage> {

    public T toMessage();

}
