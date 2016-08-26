package org.redcross.openmapkit.odkcollect;

import org.redcross.openmapkit.ExternalStorage;

import java.io.File;

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

    public String getOsmFileName() {
        return String.valueOf(id)+".osm";
    }

    public File getLocalOsmFile() {
        File osmDirectory = new File(ExternalStorage.getOSMDir());
        return new File(osmDirectory, getOsmFileName());
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof Form) {
            Form b = (Form) object;
            if(((name == null && b.name == null)|| name.equals(b.name)) && id == b.id) {
                return true;
            }
        }
        return false;
    }
}
