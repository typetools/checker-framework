package java.io;

import checkers.javari.quals.*;

public interface FilenameFilter {
    public boolean accept(@ReadOnly File dir, String name);
}
