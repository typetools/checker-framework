package java.io;

import org.checkerframework.checker.javari.qual.*;

public interface FilenameFilter {
    public boolean accept(@ReadOnly File dir, String name);
}
