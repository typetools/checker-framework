package org.checkerframework.javacutil;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.UtilPlume;

/** This file contains basic utility functions. */
public class SystemUtil {

    /** The system-specific line separator. */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Return a list of Strings, one per line of the file.
     *
     * @param argFile argument file
     * @return a list of Strings, one per line of the file
     * @throws IOException when reading the argFile
     */
    public static List<String> readFile(final File argFile) throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(argFile));
        String line;

        List<String> lines = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
        return lines;
    }

    /**
     * Returns a new String composed of the string representations of the elements joined together
     * with a copy of the specified delimiter.
     *
     * @param <T> the type of array elements
     * @param delimiter the delimiter that separates each element
     * @param objs the values whose string representations to join together
     * @return a new string that concatenates the string representations of the elements
     * @deprecated use {@code UtilPlume.join}
     */
    @Deprecated // use UtilPlume.join
    public static <T> String join(CharSequence delimiter, T[] objs) {
        if (objs == null) {
            return "null";
        }
        return UtilPlume.join(delimiter, objs);
    }

    /**
     * Returns a new String composed of the string representations of the elements joined together
     * with a copy of the specified delimiter.
     *
     * @param delimiter the delimiter that separates each element
     * @param values the values whose string representations to join together
     * @return a new string that concatenates the string representations of the elements
     * @deprecated use {@code UtilPlume.join}
     */
    @Deprecated // use UtilPlume.join
    public static String join(CharSequence delimiter, Iterable<?> values) {
        if (values == null) {
            return "null";
        }
        return UtilPlume.join(delimiter, values);
    }

    /**
     * Concatenate the string representations of the objects, placing the system-specific line
     * separator between them.
     *
     * @param <T> the type of array elements
     * @param a array of values to concatenate
     * @return the concatenation of the string representations of the values, each on its own line
     * @deprecated use {@code UtilPlume.joinLines}
     */
    @Deprecated // use UtilPlume.joinLines
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> String joinLines(T... a) {
        return join(LINE_SEPARATOR, a);
    }

    /**
     * Concatenate the string representations of the objects, placing the system-specific line
     * separator between them.
     *
     * @param v list of values to concatenate
     * @return the concatenation of the string representations of the values, each on its own line
     * @deprecated use {@code UtilPlume.joinLines}
     */
    @Deprecated // use UtilPlume.joinLines
    public static String joinLines(Iterable<? extends Object> v) {
        return join(LINE_SEPARATOR, v);
    }

    /**
     * Return true if the system property is set to "true". Return false if the system property is
     * not set or is set to "false". Otherwise, errs.
     *
     * @param key system property to check
     * @return true if the system property is set to "true". Return false if the system property is
     *     not set or is set to "false". Otherwise, errs.
     */
    public static boolean getBooleanSystemProperty(String key) {
        return Boolean.parseBoolean(System.getProperty(key, "false"));
    }

    /**
     * Return its boolean value if the system property is set. Return defaultValue if the system
     * property is not set. Errs if the system property is set to a non-boolean value.
     *
     * @param key system property to check
     * @param defaultValue value to use if the property is not set
     * @return the boolean value of {@code key} or {@code defaultValue} if {@code key} is not set
     */
    public static boolean getBooleanSystemProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        throw new Error(
                String.format(
                        "Value for system property %s should be boolean, but is \"%s\".",
                        key, value));
    }

    /**
     * Returns the major JRE version.
     *
     * <p>This is different from the version passed to the compiler via --release; use {@link
     * #getReleaseValue(ProcessingEnvironment)} to get that version.
     *
     * <p>Extract the major version number from the "java.version" system property. Two possible
     * formats are considered. Up to Java 8, from a version string like `1.8.whatever`, this method
     * extracts 8. Since Java 9, from a version string like `11.0.1`, this method extracts 11.
     *
     * @return the major version number from "java.version"
     */
    public static int getJreVersion() {
        final String jreVersionStr = System.getProperty("java.version");

        final Pattern oldVersionPattern = Pattern.compile("^1\\.(\\d+)\\..*$");
        final Matcher oldVersionMatcher = oldVersionPattern.matcher(jreVersionStr);
        if (oldVersionMatcher.matches()) {
            String v = oldVersionMatcher.group(1);
            assert v != null : "@AssumeAssertion(nullness): inspection";
            return Integer.parseInt(v);
        }

        // See http://openjdk.java.net/jeps/223
        // We only care about the major version number.
        final Pattern newVersionPattern = Pattern.compile("^(\\d+).*$");
        final Matcher newVersionMatcher = newVersionPattern.matcher(jreVersionStr);
        if (newVersionMatcher.matches()) {
            String v = newVersionMatcher.group(1);
            assert v != null : "@AssumeAssertion(nullness): inspection";
            return Integer.parseInt(v);
        }

        // For Early Access version of the JDK
        final Pattern eaVersionPattern = Pattern.compile("^(\\d+)-ea$");
        final Matcher eaVersionMatcher = eaVersionPattern.matcher(jreVersionStr);
        if (eaVersionMatcher.matches()) {
            String v = eaVersionMatcher.group(1);
            assert v != null : "@AssumeAssertion(nullness): inspection";
            return Integer.parseInt(v);
        }

        throw new RuntimeException(
                "Could not determine version from property java.version=" + jreVersionStr);
    }

    /**
     * Returns the release value passed to the compiler or null if release was not passed.
     *
     * @param env the ProcessingEnvironment
     * @return the release value or null if none was passed
     */
    public static @Nullable String getReleaseValue(ProcessingEnvironment env) {
        Context ctx = ((JavacProcessingEnvironment) env).getContext();
        Options options = Options.instance(ctx);
        return options.get(Option.RELEASE);
    }

    /**
     * Returns the pathname to the tools.jar file, or null if it does not exist. Returns null on
     * Java 9 and later.
     *
     * @return the pathname to the tools.jar file, or null
     */
    public static @Nullable String getToolsJar() {

        if (getJreVersion() > 8) {
            return null;
        }

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            String javaHomeProperty = System.getProperty("java.home");
            if (javaHomeProperty.endsWith(File.separator + "jre")) {
                javaHome = javaHomeProperty.substring(javaHomeProperty.length() - 4);
            } else {
                // Could also determine the location of javac on the path...
                throw new Error("Can't infer Java home; java.home=" + javaHomeProperty);
            }
        }
        String toolsJarFilename = javaHome + File.separator + "lib" + File.separator + "tools.jar";
        if (!new File(toolsJarFilename).exists()) {
            throw new Error(
                    String.format(
                            "File does not exist: %s ; JAVA_HOME=%s ; java.home=%s",
                            toolsJarFilename, javaHome, System.getProperty("java.home")));
        }
        return javaHome + File.separator + "lib" + File.separator + "tools.jar";
    }

    /**
     * Concatenates two arrays. Can be invoked varargs-style.
     *
     * @param <T> the type of the array elements
     * @param array1 the first array
     * @param array2 the second array
     * @return a new array containing the contents of the given arrays, in order
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concatenate(T[] array1, T... array2) {
        @SuppressWarnings("nullness") // elements are not non-null yet, but will be by return stmt
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    /**
     * Concatenates an element, an array, and an element.
     *
     * @param <T> the type of the array elements
     * @param firstElt the first element
     * @param array the array
     * @param lastElt the last elemeent
     * @return a new array containing first element, the array, and the last element, in that order
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concatenate(T firstElt, T[] array, T lastElt) {
        @SuppressWarnings("nullness") // elements are not non-null yet, but will be by return stmt
        T[] result = Arrays.copyOf(array, array.length + 2);
        result[0] = firstElt;
        System.arraycopy(array, 0, result, 1, array.length);
        result[result.length - 1] = lastElt;
        return result;
    }

    /**
     * Like Thread.sleep, but does not throw any exceptions, so it is easier for clients to use.
     * Causes the currently executing thread to sleep (temporarily cease execution) for the
     * specified number of milliseconds.
     *
     * @param millis the length of time to sleep in milliseconds
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
