package io.ona.openmapkit;

import java.io.File;

/**
 * Created by coder on 9/28/15.
 */
public class FileHolder implements Comparable<FileHolder> {
    String name;
    File file;

    public FileHolder(String name, File file) {
        this.name = name;
        this.file = file;
    }

    @Override
    public int compareTo(FileHolder fileParams) {
        return this.name.compareTo(fileParams.name);
    }
}
