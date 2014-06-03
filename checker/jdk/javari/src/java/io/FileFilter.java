package java.io;

import org.checkerframework.checker.javari.qual.*;

public interface FileFilter {
    public boolean accept(@ReadOnly File pathname);
}
