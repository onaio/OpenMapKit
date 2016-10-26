package org.redcross.openmapkit.querying;

import android.content.Intent;

/**
 * Created by Jason Rogena - jrogena@ona.io on 26/10/2016.
 */

public interface OnQueryCompleteListener {
    void onSuccess(Intent intent);
    void onError(Exception e);
}
