package org.fusesource.mvnplugins.uberize;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An UberEntry represents a file path in an uber jar.  It
 * will keep track of overlapping source files until
 * a transformation can apply a merge strategy to them.
 * When a transformation is applied, a new UberEntry will
 * replace the previous one but it will maintain a reference
 * to it so that the transformation history of the file path
 * can be inspected.
 */
public class UberEntry {

    private final String path;
    private final List<File> sources = new ArrayList<File>();
    private final List<UberEntry> previous;

    /**
     * Creates an UberEntry at located at the specified path.
     * @param path
     */
    public UberEntry(String path) {
        this(path, (List<UberEntry>)null);
    }

    /**
     * Creates an UberEntry that is an update of a previous
     * UberEntry.  The path of the new UberEntry will match
     * the previous one.
     *
     * @param previous
     */
    public UberEntry(UberEntry previous) {
        this(previous.getPath(), previous);
    }

    /**
     * Creates na UberEntry at located at the specified path, which
     * is an updated of a previous UberEntry.
     *
     * @param path
     */
    public UberEntry(String path, UberEntry previous) {
        this.path = path;
        this.previous = toList(previous);
    }

    static private List<UberEntry> toList(UberEntry entry) {
        if( entry == null ) {
            return null;
        }
        ArrayList rc = new ArrayList(1);
        rc.add(entry);
        return rc;
    }

    public UberEntry(String path, List<UberEntry> previous) {
        this.path = path;
        this.previous = previous;
    }

    /**
     * A list which can be used to track all the overlapping source files associated with the path entry.
     *
     * @return
     */
    public List<File> getSources() {
        return sources;
    }

    /**
     *
     * @return the source file associated with entry if there is only one, otherwise returns null.
     */
    public File getSource() {
        if( sources.size()!=1 ) {
            return null;
        }
        return sources.get(0);
    }

    /**
     * The path of the entry.
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * @return The previous version of the UberEntry or null if this is the original version.
     */
    public UberEntry getPrevious() {
        if( previous==null || previous.isEmpty() ) {
            return null;
        }
        return previous.get(0);
    }

    /**
     * If a transformer agregates mutliple UberEntry paths into a single path
     * then the prvious version of thise node will be a list of UberEntrys
     * @return
     */
    public List<UberEntry> getAllPrevious() {
        if( previous==null ) {
            return null;
        }
        return previous;
    }

    public UberEntry addSource(File file) {
        sources.add(file);
        return this;
    }
}
