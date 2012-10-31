package checkers.eclipse.util;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;

public class Paths
{
    private Paths()
    {
        throw new AssertionError("Cannot be instantiated");
    }

    public static String absolutePathOf(IClasspathEntry entry)
    {
        IFile jarFile = ResourceUtils.workspaceRoot().getFile(entry.getPath());
        IPath location = jarFile.getLocation();
        IPath path = (location != null) ? location : jarFile.getFullPath();
        return path.toOSString();
    }

    public static class ClasspathBuilder
    {
        private static final String PATH_SEPARATOR = File.pathSeparator;

        private final StringBuilder classpath = new StringBuilder();

        public ClasspathBuilder()
        {
            // noop
        }

        public ClasspathBuilder append(String path)
        {
            if (classpath.length() != 0)
                classpath.append(PATH_SEPARATOR);
            classpath.append(path);
            return this;
        }

        @Override
        public String toString()
        {
            return classpath.toString();
        }
    }
}
