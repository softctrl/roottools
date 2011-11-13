package com.stericson.RootTools;

import java.io.File;

public class Symlink {
    protected final File file;
    protected final File symlinkPath;

    Symlink(File file, File path) {
        this.file = file;
        symlinkPath = path;
    }

    public File getFile() {
        return this.file;
    }

    public File getSymlinkPath() {
        return symlinkPath;
    }
}
