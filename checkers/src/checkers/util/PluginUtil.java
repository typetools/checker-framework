package checkers.util;
/**
 * This file contains basic utility functions that should be reused to create
 * a command line call to CheckerMain.
 *
 * NOTE: There is multiple copies of this file in the following projects/locations:
 * maven-plugin/
 *     org.checkersplugin.PluginUtil
 *
 * checker-framework-eclipse-plugin/
 *     checkers.eclipse.util.PluginUtil
 *
 * checker-framework/
 *     checkers.util.PluginUtil
 *
 * These files MUST be IDENTICAL after the package descriptor.
 *
 * If you change this file be sure to copy the exact file (including this comment
 * and excluding the package line) to the other projects.  During release this file
 * and all its copies will be diffed (excluding any line starting with "package ")
 *
 * There is a script at checker-framework/release/syncPluginUtil.sh
 * that syncs these files programatically.
 */

import java.io.*;
import java.util.*;

public class PluginUtil {

    /**
     * Option name for specifying an alternative javac.jar location.  The accompanying value
     * MUST be the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String JAVAC_PATH_OPT = "-javacJar";

    /**
     * Option name for specifying an alternative jdk.jar location.  The accompanying value
     * MUST be the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String JDK_PATH_OPT   = "-jdkJar";


    public static List<File> toFiles(final List<String> fileNames) {
        final List<File> files = new ArrayList<File>(fileNames.size());
        for(final String fn : fileNames) {
            files.add(new File(fn));
        }

        return files;
    }

    /**
     * Takes a list of files and writes it as a "File of file names" (i.e. a file with one filepath on each line)
     * to the destination file, overwriting the destination file if it exists.  Note the filepath used is the
     * absolute filepath
     * @param destination The fofn file we are writing.  This file will contain newline separated list of absolute file paths
     * @param files The files to write to the destination file
     * @throws IOException
     */
    public static void writeFofn(final File destination, final List<File> files) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(destination));
        try {
            for(final File file : files) {
                bw.write(wrapArg(file.getAbsolutePath()));
                bw.newLine();
            }

            bw.flush();
        } finally {
            bw.close();
        }
    }

    /**
     * Takes a list of files and writes it as a "File of file names" (i.e. a file with one filepath on each line)
     * to the destination file, overwriting the destination file if it exists.  Note the filepath used is the
     * absolute filepath
     * @param destination The fofn file we are writing.  This file will contain newline separated list of absolute file paths
     * @param files The files to write to the destination file
     * @throws IOException
     */
    public static void writeFofn(final File destination, final File ... files) throws IOException {
        writeFofn(destination, Arrays.asList(files));
    }

    public static File writeTmpFofn(final String prefix, final String suffix, final boolean deleteOnExit,
                                    final List<File> files) throws IOException {
        final File tmpFile = File.createTempFile(prefix, suffix);
        if( deleteOnExit ) {
            tmpFile.deleteOnExit();
        }
        writeFofn(tmpFile, files);
        return tmpFile;
    }

    public static File writeTmpArgFile(final String prefix, final String suffix, final boolean deleteOnExit,
                                       final List<String> args) throws IOException {
        final File tmpFile = File.createTempFile(prefix, suffix);
        if( deleteOnExit ) {
            tmpFile.deleteOnExit();
        }
        writeArgFile(tmpFile, args);
        return tmpFile;
    }

    public static void writeArgFile(final File destination, final List<String> args) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(destination));
        try {
            bw.write(join(" ", args));
            bw.flush();
        } finally {
            bw.close();
        }
    }


    public static List<String> readArgFile(final File argFile) throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(argFile));
        String line;

        List<String> lines = new ArrayList<String>();
        while((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
        return lines;
    }

    /**
     * TODO: Either create/use a util class
     */
    public static <T> String join(final String delimiter, final Collection<T> objs) {

        boolean notFirst = false;
        final StringBuffer sb = new StringBuffer();

        for(final Object obj : objs) {
            if(notFirst) {
                sb.append(delimiter);
            }
            sb.append(obj.toString());
            notFirst = true;
        }

        return sb.toString();
    }


    public static List<String> getStringProp(final Map<CheckerProp, Object> props,
                                             final CheckerProp prop, final String tag,
                                             final String ... extras) {
        final List<String> out = new ArrayList<String>();
        final String strProp = (String) props.get(prop);
        if(strProp != null && !strProp.isEmpty()) {
            out.add(tag + strProp);
            for(final String extra : extras) {
                out.add(extra);
            }
        }

        return out;
    }

    public static List<String> getBooleanProp(final Map<CheckerProp, Object> props,
                                              final CheckerProp prop, final String tag) {
        Boolean aSkip = (Boolean) props.get(prop);
        if(aSkip != null && aSkip) {
            return Arrays.asList(tag);
        }
        return new ArrayList<String>();
    }

    public enum CheckerProp {
        IMPLICIT_IMPORTS() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                return getStringProp(props, this, "-J-Djsr308_imports=", "-implicit:class");
            }
        },

        MISC_COMPILER() {
            @Override
            public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
                @SuppressWarnings("unchecked")
                List<String> miscOpts = (List<String>) props.get(this);

                if (miscOpts != null && !miscOpts.isEmpty()) {
                    return new ArrayList<String>(miscOpts);
                }
                return new ArrayList<String>();
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
     * Any options found in props to the cmd list
     * @param cmd    A list to which the options should be added
     * @param props  The map of checker properties too search for options in
     */
    private static void addOptions(final List<String> cmd, Map<CheckerProp,Object> props) {
        for(CheckerProp cp : CheckerProp.values()) {
            cmd.addAll(cp.getCmdLine(props));
        }
    }

    public static File writeTmpSrcFofn(final String prefix, final boolean deleteOnExit,
                                       final List<File> files) throws IOException {
        return writeTmpFofn(prefix, ".src_files", deleteOnExit, files);
    }

    public static File writeTmpCpFile(final String prefix, final boolean deleteOnExit,
                                      final String classpath) throws IOException {
        return writeTmpArgFile(prefix, ".classpath", deleteOnExit, Arrays.asList("-classpath", wrapArg(classpath)));
    }

    public static boolean isWindows() {
        final String os = System.getProperty("os.name");
        return os.toLowerCase().contains("win");
    }

    public static String wrapArg(final String classpath) {
        if(classpath.contains(" ")) {
            return '"' + escapeQuotesAndSlashes(classpath) + '"';
        }
        return classpath;
    }

    public static String escapeQuotesAndSlashes(final String toEscape) {
        final Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("\\\\", "\\\\\\\\");
        replacements.put("\"", "\\\\\"");

        String replacement = toEscape;
        for(final Map.Entry<String, String> entry : replacements.entrySet()) {
            replacement = replacement.replaceAll(entry.getKey(), entry.getValue());
        }

        return replacement;
    }

    public static String getJavaCommand(final String javaHome, final PrintStream out) {
        if( javaHome == null || javaHome.equals("") ) {
            return "java";
        }

        final File java    = new File(javaHome, "bin" + File.separator + "java");
        final File javaExe = new File(javaHome, "bin" + File.separator + "java.exe");
        if(java.exists()) {
            return java.getAbsolutePath();
        } else if(javaExe.exists()) {
            return javaExe.getAbsolutePath();
        } else {
            if( out != null ) {
                out.println("Could not find java executable at: ( " + java.getAbsolutePath()    + "," +
                        javaExe.getAbsolutePath() + ")" +
                        "\n  Using \"java\" command.\n");
            }
            return "java";
        }
    }

    public static String fileArgToStr(final File fileArg) {
        return "@" + fileArg.getAbsolutePath();
    }


    //TODO: Perhaps unify this with CheckerMain as it violates DRY
    public static List<String> getCmd(final String executable,  final File javacPath, final File jdkPath,
                                      final File srcFofn, final String processors,
                                      final String checkerHome, final String javaHome,
                                      final File classPathFofn, final String bootClassPath,
                                      final Map<CheckerProp, Object> props, PrintStream out,
                                      final boolean procOnly, final String outputDirectory) {

        final List<String> cmd = new ArrayList<String>();

        final String java    = ( executable != null ) ? executable
                : getJavaCommand(javaHome, out);

        cmd.add(java);
        cmd.add("-jar");
        cmd.add(checkerHome);

        if(procOnly) {
            cmd.add("-proc:only");
        } else {
            cmd.add("-d");
            cmd.add(outputDirectory);
        }

        if(bootClassPath != null && !bootClassPath.trim().isEmpty()) {
            cmd.add("-Xbootclasspath/p:" +  bootClassPath);
        }

        if(javacPath != null) {
            cmd.add(JAVAC_PATH_OPT);
            cmd.add(javacPath.getAbsolutePath());
        }

        if(jdkPath != null) {
            cmd.add(JDK_PATH_OPT);
            cmd.add(jdkPath.getAbsolutePath());
        }

        if(classPathFofn != null ) {
            cmd.add(fileArgToStr(classPathFofn));
        }

        if(processors != null) {
            cmd.add("-processor");
            cmd.add(processors);
        }

        addOptions(cmd, props);
        cmd.add(fileArgToStr(srcFofn));

        return cmd;

    }

    public static List<String> toJavaOpts(final List<String> opts) {
        final List<String> outOpts = new ArrayList<String>(opts.size());
        for(final String opt : opts) {
            outOpts.add("-J" + opt);
        }

        return outOpts;
    }

    public static List<String> getCmdArgsOnly(final File srcFofn, final String processors,
                                              final String checkerHome, final String javaHome,
                                              final File classpathFofn,   final String bootClassPath,
                                              final Map<CheckerProp, Object> props, PrintStream out,
                                              final boolean procOnly, final String outputDirectory) {

        final List<String> cmd = getCmd(null, null, null, srcFofn, processors,
                checkerHome, javaHome, classpathFofn, bootClassPath, props, out,
                procOnly, outputDirectory);
        cmd.remove(0);
        return cmd;
    }



    public static List<String> getCmdArgsOnly(final File javacPath, final File jdkPath,
                                              final File srcFofn, final String processors,
                                              final String checkerHome, final String javaHome,
                                              final File classpathFofn,   final String bootClassPath,
                                              final Map<CheckerProp, Object> props, PrintStream out,
                                              final boolean procOnly, final String outputDirectory) {

        final List<String> cmd = getCmd(null, javacPath, jdkPath, srcFofn, processors,
                checkerHome, javaHome, classpathFofn, bootClassPath, props, out,
                procOnly, outputDirectory);
        cmd.remove(0);
        return cmd;
    }
}
