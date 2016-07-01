package com.gaohong.BIM;

import java.io.File;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-1
 */
public class FileListEntry {

    public String label=null;
    public File file=null;
    public boolean isDirectory=false;
    public int type=NORMAL;
    public int recentNumber=-1;
    public static final int NORMAL=0;
    public static final int HOME=1;
    public static final int RECENT=2;
    public String size=null;

    public FileListEntry() {
    }

    public FileListEntry(int type, int recentNumber, File file, String label) {
        this.type=type;
        this.recentNumber=recentNumber;
        this.file=file;
        this.label=label;
    }

    public FileListEntry(int type, String label) {
        this.type=type;
        this.label=label;
    }

    public boolean isUpFolder() {
        return this.isDirectory&&null!=label&&this.label.equals("..");
    }
}
