package org.redcross.openmapkit.querying;

import android.content.Intent;

/**
 * Created by Jason Rogena - jrogena@ona.io on 25/10/2016.
 */

public abstract class QueryHandler {
    public static final String DEFAULT_RESULT = null;//value to be returned if query() gets no result
    public static final String KEY_INTENT_RESULT = "value";//the key of the extra field in the response intent holding the query result

    /**
     * This method is intended to return the final value to be sent back to an intent that started
     * the QueryActivity for results
     *
     * @param intent                    The intent to get the query parameters from
     * @param onQueryCompleteListener   The listener to call when the query is done executing
     */
    public abstract void handle(Intent intent, OnQueryCompleteListener onQueryCompleteListener);

    /**
     * Determines whether a query handler can handle the provided intent
     *
     * @param intent
     * @return
     */
    public abstract boolean canHandle(Intent intent);
}
