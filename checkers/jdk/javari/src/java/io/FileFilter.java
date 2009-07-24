package java.io;

import checkers.javari.quals.*;

public interface FileFilter {
    public boolean accept(@ReadOnly File pathname);
}
