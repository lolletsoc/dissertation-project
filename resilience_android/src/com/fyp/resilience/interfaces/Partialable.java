
package com.fyp.resilience.interfaces;

import com.google.api.client.json.GenericJson;

public interface Partialable<T extends GenericJson> {

    public T toPartial();

}
