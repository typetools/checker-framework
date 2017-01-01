package org.checkerframework.eclipse.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;

public class Paths {
    private Paths() {
        throw new AssertionError("Cannot be instantiated");
    }

    public static String absolutePathOf(IClasspathEntry entry) {
        IFile jarFile = ResourceUtils.workspaceRoot().getFile(entry.getPath());
        IPath location = jarFile.getLocation();
        IPath path = (location != null) ? location : jarFile.getFullPath();
        String osString = path.toOSString();

        if (PluginUtil.isWindows() && !hasDriveLetter(osString)) {
            osString = kludgeFixAddDriveLetterToFilePath(osString);
        }
        return osString;
    }

    private static boolean equalsIgnoreCase(char c1, char c2) {
        return String.valueOf(c1).toLowerCase().equals(String.valueOf(c2).toLowerCase());
    }

    private static char driveLetter(final File file) {
        return file.getAbsolutePath().charAt(0);
    }

    /**
     * Ordered by preference. Use listRoots to get drive letters for windows and put C (if
     * available) up front and (A:) in the back.
     */
    private static List<File> determineOrderedDriveRoots() {
        final File[] files = File.listRoots();
        final List<File> driveRoots = new ArrayList<File>();

        final Pattern pattern = Pattern.compile("^([A-Z]):\\\\$");

        for (final File file : files) {
            final Matcher match = pattern.matcher(file.getAbsolutePath());
            if (match.matches()) {
                driveRoots.add(file);
            }
        }

        // C up front, a in the back, everything else is equal
        Collections.sort(
                driveRoots,
                new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        if (equalsIgnoreCase(driveLetter(o1), driveLetter(o2))) {
                            return 0;
                        }
                        if (equalsIgnoreCase(driveLetter(o1), 'c')
                                || equalsIgnoreCase(driveLetter(o2), 'a')) {
                            return -1;
                        }
                        if (equalsIgnoreCase(driveLetter(o1), 'a')
                                || equalsIgnoreCase(driveLetter(o2), 'c')) {
                            return 1;
                        }

                        return 0;
                    }
                });

        return driveRoots;
    }

    private static List<File> orderedDriveRoots =
            Collections.unmodifiableList(determineOrderedDriveRoots());

    private static boolean hasDriveLetter(String filePath) {
        for (File file : orderedDriveRoots) {
            if (filePath.startsWith(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    private static String toDriveLetter(final char c) {
        return c + File.pathSeparator + File.separator;
    }

    private static String toDrivePath(final char c, String filePath) {
        filePath = (filePath.startsWith(File.separator)) ? filePath.substring(1) : filePath;
        return toDriveLetter(c) + filePath;
    }

    /**
     * I am having a heck of a time trying to get the drive letter to appear on the filepath for
     * jars added via Eclipse classpath entries. For now, use the potentially error prone technique
     * of listing all drive letters and testing to see if the path exists on that drive, prefer C:.
     * This is error prone because a path can exist on multiple drives. In the case of mirroring it
     * could potentially use an archived version rather than the latest version of the jar. This is
     * not a huge risk but still a kludge.
     *
     * @param pathStr A file path that does not start with a drive letter
     * @return pathStr
     */
    public static String kludgeFixAddDriveLetterToFilePath(final String pathStr) {
        for (final File root : orderedDriveRoots) {

            final File cPath =
                    new File(root, (pathStr.startsWith("\\") ? pathStr.substring(1) : pathStr));
            if (cPath.exists()) {
                return cPath.getAbsolutePath();
            }
        }

        return pathStr;
    }
}
