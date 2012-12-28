package checkers.util;
/**
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
 * This file contains basic utility functions that should be reused to create
 * a command line call to CheckerMain.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PluginUtil {


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
                bw.write(file.getAbsolutePath());
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

        A_SKIP() { //TODO: NEED TO ADD
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


    public static String getJavaCommand(final String javaHome, final PrintStream out) {
        if( javaHome == null || javaHome.equals("") ) {
            return "java";
        }

        final File java = new File(javaHome, "bin" + File.separator + "java");
        if(java.exists()) {
            return java.getAbsolutePath();
        } else {
            //TODO: IS THERE A BETTER WAY OF SAYING WE ARE LETTING THE OS RESOLVE THE REFERENCE TO JAVA?

            if( out != null ) {
                out.println("Could not find java executable at " + java.getAbsolutePath() +
                        ".  Using \"java\" command.");
            }
            return "java";
        }
    }

    public static List<String> getCmd(final String executable,  final File srcFofn, final String processors,
                                      final String checkerHome, final String javaHome,
                                      final String classpath,   final String bootClassPath,
                                      final Map<CheckerProp, Object> props, PrintStream out) {

        final List<String> cmd = new ArrayList<String>();

        final String java    = ( executable != null ) ? executable
                                                      : getJavaCommand(javaHome, out);

        cmd.add(java);
        cmd.add("-jar");
        cmd.add(checkerHome);

        cmd.add("-proc:only");
        cmd.add("-Xbootclasspath/p:" + bootClassPath);

        cmd.add("-classpath");
        cmd.add(classpath);

        cmd.add("-processor");
        cmd.add(processors);

        addOptions(cmd, props);

        cmd.add("@" + srcFofn.getAbsolutePath());
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
                                              final String classpath,   final String bootClassPath,
                                              final Map<CheckerProp, Object> props, PrintStream out) {

        final List<String> cmd = getCmd(null, srcFofn, processors,
                checkerHome, javaHome, classpath,
                bootClassPath, props, out);
        cmd.remove(0);
        return cmd;
    }
}
