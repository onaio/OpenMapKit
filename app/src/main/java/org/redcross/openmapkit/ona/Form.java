package org.redcross.openmapkit.ona;

import org.redcross.openmapkit.Strings;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/16/16.
 */
public class Form {
    private final String name;
    private final int id;

    public Form(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
