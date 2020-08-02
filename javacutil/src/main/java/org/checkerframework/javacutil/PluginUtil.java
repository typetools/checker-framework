package org.checkerframework.javacutil;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.UtilPlume;

/**
 * This file contains basic utility functions that should be reused to create a command-line call to
 * {@code CheckerMain}.
 *
 * @deprecated Renamed to {@link SystemUtil} and deleted some unused methods.
 */
@Deprecated
public class PluginUtil {

    /**
     * Option name for specifying an alternative checker-qual.jar location. The accompanying value
     * MUST be the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String CHECKER_QUAL_PATH_OPT = "-checkerQualJar";

    /**
     * Option name for specifying an alternative javac.jar location. The accompanying value MUST be
     * the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String JAVAC_PATH_OPT = "-javacJar";

    /**
     * Option name for specifying an alternative jdk.jar location. The accompanying value MUST be
     * the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String JDK_PATH_OPT = "-jdkJar";

    /** The system-specific line separator. */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Convert a list of strings (file names) to a list of files.
     *
     * @param fileNames a list of file names
     * @return a list of File objects
     */
    public static List<File> toFiles(final List<String> fileNames) {
        final List<File> files = new ArrayList<>(fileNames.size());
        for (final String fn : fileNames) {
            files.add(new File(fn));
        }

        return files;
    }

    /**
     * Takes a list of files and writes it as a "File of file names" (i.e. a file with one filepath
     * on each line) to the destination file, overwriting the destination file if it exists. Note
     * the filepath used is the absolute filepath
     *
     * @param destination the fofn file we are writing. This file will contain newline separated
     *     list of absolute file paths.
     * @param files the files to write to the destination file
     */
    public static void writeFofn(final File destination, final List<File> files)
            throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(destination));
        try {
            for (final File file : files) {
                bw.write(wrapArg(file.getAbsolutePath()));
                bw.newLine();
            }

            bw.flush();
        } finally {
            bw.close();
        }
    }

    /**
     * Takes a list of files and writes it as a "File of file names" (i.e. a file with one filepath
     * on each line) to the destination file, overwriting the destination file if it exists. Note
     * the filepath used is the absolute filepath
     *
     * @param destination the fofn file we are writing. This file will contain newline separated
     *     list of absolute file paths.
     * @param files the files to write to the destination file
     */
    public static void writeFofn(final File destination, final File... files) throws IOException {
        writeFofn(destination, Arrays.asList(files));
    }

    public static File writeTmpFofn(
            final String prefix,
            final String suffix,
            final boolean deleteOnExit,
            final List<File> files)
            throws IOException {
        final File tmpFile = File.createTempFile(prefix, suffix);
        if (deleteOnExit) {
            tmpFile.deleteOnExit();
        }
        writeFofn(tmpFile, files);
        return tmpFile;
    }

    /**
     * Write the strings to a temporary file.
     *
     * @param deleteOnExit if true, delete the file on program exit
     */
    public static File writeTmpFile(
            final String prefix,
            final String suffix,
            final boolean deleteOnExit,
            final List<String> args)
            throws IOException {
        final File tmpFile = File.createTempFile(prefix, suffix);
        if (deleteOnExit) {
            tmpFile.deleteOnExit();
        }
        writeFile(tmpFile, args);
        return tmpFile;
    }

    /** Write the strings to the file, one per line. */
    public static void writeFile(final File destination, final List<String> contents)
            throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(destination));
        try {
            for (String line : contents) {
                bw.write(line);
                bw.newLine();
            }
            bw.flush();
        } finally {
            bw.close();
        }
    }

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
     */
    public static <T> String join(CharSequence delimiter, T @Nullable [] objs) {
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
     */
    public static String join(CharSequence delimiter, @Nullable Iterable<?> values) {
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
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> String joinLines(T @Nullable ... a) {
        return join(LINE_SEPARATOR, a);
    }

    /**
     * Concatenate the string representations of the objects, placing the system-specific line
     * separator between them.
     *
     * @param v list of values to concatenate
     * @return the concatenation of the string representations of the values, each on its own line
     */
    public static String joinLines(@Nullable Iterable<? extends Object> v) {
        return join(LINE_SEPARATOR, v);
    }

    /**
     * Returns a list of command-line arguments: one that sets the given property, plus everything
     * in extras. Returns the empty list if prop is not found in props (or is empty there), even if
     * extras is not empty.
     *
     * @param prop the property to look up in props
     * @param cmdLineArgStart the command-line argument that introduces prop
     */
    public static List<String> getStringProp(
            final Map<CheckerProp, Object> props,
            final CheckerProp prop,
            final String cmdLineArgStart,
            final String... extras) {
        final List<String> out = new ArrayList<>();
        final String strProp = (String) props.get(prop);
        if (strProp != null && !strProp.isEmpty()) {
            out.add(cmdLineArgStart + strProp);
            for (final String extra : extras) {
                out.add(extra);
            }
        }

        return out;
    }

    /**
     * If prop is in props, return a 1-element list containing {@code cmdLineArg}. Otherwise, return
     * a 0-element list.
     */
    public static List<String> getBooleanProp(
            final Map<CheckerProp, Object> props, final CheckerProp prop, final String cmdLineArg) {
        Boolean aSkip = (Boolean) props.get(prop);
        if (aSkip != null && aSkip) {
            return Arrays.asList(cmdLineArg);
        }
        return new ArrayList<>();
    }

    public enum CheckerProp {
        MISC_COMPILER() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                @SuppressWarnings("unchecked")
                List<String> miscOpts = (List<String>) props.get(this);

                if (miscOpts != null && !miscOpts.isEmpty()) {
                    return new ArrayList<>(miscOpts);
                }
                return new ArrayList<>();
            }
        },

        A_SKIP() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getStringProp(props, this, "-AskipUses=");
            }
        },

        A_LINT() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getStringProp(props, this, "-Alint=");
            }
        },

        A_WARNS() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getBooleanProp(props, this, "-Awarns");
            }
        },
        A_NO_MSG_TXT() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getBooleanProp(props, this, "-Anomsgtext");
            }
        },
        A_SHOW_CHECKS() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getBooleanProp(props, this, "-Ashowchecks");
            }
        },
        A_FILENAMES() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getBooleanProp(props, this, "-Afilenames");
            }
        },
        A_DETAILED_MSG() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getBooleanProp(props, this, "-Adetailedmsgtext");
            }
        };

        public abstract List<String> getCmdLine(final Map<CheckerProp, Object> props);
    }

    /**
     * Any options found in props to the cmd list.
     *
     * @param cmd a list to which the options should be added
     * @param props the map of checker properties too search for options in
     */
    private static void addOptions(final List<String> cmd, Map<CheckerProp, Object> props) {
        for (CheckerProp cp : CheckerProp.values()) {
            cmd.addAll(cp.getCmdLine(props));
        }
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
        return Boolean.valueOf(System.getProperty(key, "false"));
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

    public static File writeTmpSrcFofn(
            final String prefix, final boolean deleteOnExit, final List<File> files)
            throws IOException {
        return writeTmpFofn(prefix, ".src_files", deleteOnExit, files);
    }

    public static File writeTmpCpFile(
            final String prefix, final boolean deleteOnExit, final String classpath)
            throws IOException {
        return writeTmpFile(
                prefix,
                ".classpath",
                deleteOnExit,
                Arrays.asList("-classpath", wrapArg(classpath)));
    }

    public static boolean isWindows() {
        final String os = System.getProperty("os.name");
        return os.toLowerCase().contains("win");
    }

    public static String wrapArg(final String classpath) {
        if (classpath.contains(" ")) {
            return '"' + escapeQuotesAndSlashes(classpath) + '"';
        }
        return classpath;
    }

    public static String escapeQuotesAndSlashes(final String toEscape) {
        final Map<String, String> replacements = new HashMap<>();
        replacements.put("\\\\", "\\\\\\\\");
        replacements.put("\"", "\\\\\"");

        String replacement = toEscape;
        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
            replacement = replacement.replaceAll(entry.getKey(), entry.getValue());
        }

        return replacement;
    }

    public static String getJavaCommand(final String javaHome, final PrintStream out) {
        if (javaHome == null || javaHome.equals("")) {
            return "java";
        }

        final File java = new File(javaHome, "bin" + File.separator + "java");
        final File javaExe = new File(javaHome, "bin" + File.separator + "java.exe");
        if (java.exists()) {
            return java.getAbsolutePath();
        } else if (javaExe.exists()) {
            return javaExe.getAbsolutePath();
        } else {
            if (out != null) {
                out.printf(
                        "Could not find java executable at: (%s,%s)%n  Using \"java\" command.%n",
                        java.getAbsolutePath(), javaExe.getAbsolutePath());
            }
            return "java";
        }
    }

    public static String fileArgToStr(final File fileArg) {
        return "@" + fileArg.getAbsolutePath();
    }

    /** Build a javac command. */
    // TODO: Perhaps unify this with CheckerMain as it violates DRY
    public static List<String> getCmd(
            final @Nullable String executable,
            final @Nullable File javacPath,
            final @Nullable File jdkPath,
            final File srcFofn,
            final String processors,
            final String checkerHome,
            final String javaHome,
            final File classPathFofn,
            final String bootClassPath,
            final Map<CheckerProp, Object> props,
            PrintStream out,
            final boolean procOnly,
            final String outputDirectory) {

        final List<String> cmd = new ArrayList<>();

        final String java = (executable != null) ? executable : getJavaCommand(javaHome, out);

        cmd.add(java);
        cmd.add("-jar");
        cmd.add(checkerHome);

        if (procOnly) {
            cmd.add("-proc:only");
        } else if (outputDirectory != null) {
            cmd.add("-d");
            cmd.add(outputDirectory);
        }

        if (bootClassPath != null && !bootClassPath.trim().isEmpty()) {
            cmd.add("-Xbootclasspath/p:" + bootClassPath);
        }

        if (javacPath != null) {
            cmd.add(JAVAC_PATH_OPT);
            cmd.add(javacPath.getAbsolutePath());
        }

        if (jdkPath != null) {
            cmd.add(JDK_PATH_OPT);
            cmd.add(jdkPath.getAbsolutePath());
        }

        if (classPathFofn != null) {
            cmd.add(fileArgToStr(classPathFofn));
        }

        if (processors != null) {
            cmd.add("-processor");
            cmd.add(processors);
        }

        addOptions(cmd, props);
        cmd.add(fileArgToStr(srcFofn));

        return cmd;
    }

    public static List<String> toJavaOpts(final List<String> opts) {
        final List<String> outOpts = new ArrayList<>(opts.size());
        for (final String opt : opts) {
            outOpts.add("-J" + opt);
        }

        return outOpts;
    }

    public static List<String> getCmdArgsOnly(
            final File srcFofn,
            final String processors,
            final String checkerHome,
            final String javaHome,
            final File classpathFofn,
            final String bootClassPath,
            final Map<CheckerProp, Object> props,
            PrintStream out,
            final boolean procOnly,
            final String outputDirectory) {

        final List<String> cmd =
                getCmd(
                        null,
                        null,
                        null,
                        srcFofn,
                        processors,
                        checkerHome,
                        javaHome,
                        classpathFofn,
                        bootClassPath,
                        props,
                        out,
                        procOnly,
                        outputDirectory);
        cmd.remove(0);
        return cmd;
    }

    public static List<String> getCmdArgsOnly(
            final File javacPath,
            final File jdkPath,
            final File srcFofn,
            final String processors,
            final String checkerHome,
            final String javaHome,
            final File classpathFofn,
            final String bootClassPath,
            final Map<CheckerProp, Object> props,
            PrintStream out,
            final boolean procOnly,
            final String outputDirectory) {

        final List<String> cmd =
                getCmd(
                        null,
                        javacPath,
                        jdkPath,
                        srcFofn,
                        processors,
                        checkerHome,
                        javaHome,
                        classpathFofn,
                        bootClassPath,
                        props,
                        out,
                        procOnly,
                        outputDirectory);
        cmd.remove(0);
        return cmd;
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
     * Determine the version of the JRE that we are currently running and select a jdkX where X is
     * the version of Java that is being run (e.g. 8, 9, ...)
     *
     * @return "jdk<em>X</em>" where X is the version of Java that is being run (e.g. 8, 9, ...)
     */
    public static String getJdkJarPrefix() {
        final int jreVersion = getJreVersion();
        final String prefix;

        if (jreVersion < 8) {
            throw new AssertionError("Unsupported JRE version: " + jreVersion);
        } else {
            prefix = "jdk" + jreVersion;
        }

        return prefix;
    }

    /**
     * Determine the version of the JRE that we are currently running and select a jdkX.jar where X
     * is the version of Java that is being run (e.g. 8, 9, ...)
     *
     * @return the jdkX.jar where X is the version of Java that is being run (e.g. 8, 9, ...)
     */
    public static String getJdkJarName() {
        final String fileName = getJdkJarPrefix() + ".jar";
        return fileName;
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
}
