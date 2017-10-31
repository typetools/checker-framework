package org.checkerframework.eclipse.javac;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.actions.CheckerManager;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.prefs.OptionLine;
import org.checkerframework.eclipse.util.Command;
import org.checkerframework.eclipse.util.PluginUtil;
import org.checkerframework.eclipse.util.Util;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;

/**
 * Runs the Checker Framework (i.e. javac with the appropriate bootclasspath, classpath, and option
 * arguments).
 */
public class CommandlineJavacRunner implements CheckersRunner {

    /** The location of the checkers.jar relative to the plugin directory */
    public static final String CHECKERS_JAR_LOCATION = "lib/checker.jar";

    public boolean verbose = false;

    /** Names of source files to compile */
    protected final List<String> fileNames;

    /** checkers to run */
    protected final String[] processors;

    /** The classpath for this project */
    protected final String classpath;

    /** The bootclasspath for this project */
    protected final String bootClasspath;

    /** The output of running the Checker Framework Compiler */
    protected String checkResult;

    /** The location of checkers.jar */
    protected File checkerJar;

    /** Whether or not the checker in question has qualifiers that are not supplied by Aquals */
    protected boolean hasQuals;

    public CommandlineJavacRunner(
            final String[] fileNames,
            final String[] processors,
            final String classpath,
            final String bootClasspath,
            final boolean hasQuals) {
        this.fileNames = Arrays.asList(fileNames);
        this.processors = processors;

        // TODO: SEEMS THAT WHEN WE ARE USING @ ARGS THE CLASSPATH FROM THE JAR IS OVERRIDDEN - FIX
        // THIS
        this.checkerJar = locatePluginFile(CHECKERS_JAR_LOCATION);
        this.classpath = checkerJar.getAbsolutePath() + File.pathSeparator + classpath;
        this.bootClasspath = bootClasspath;

        final IPreferenceStore prefs = CheckerPlugin.getDefault().getPreferenceStore();
        this.verbose = prefs.getBoolean(CheckerPreferences.PREF_CHECKER_VERBOSE);

        this.hasQuals = hasQuals;
    }

    /**
     * Write the names of all files to be checked into a file-of-file-names(fofn) and then call the
     * Checker Framework in "check-only" mode to check the files listed in the fofn
     */
    public void run() {
        try {
            MessageConsoleStream out = CheckerPlugin.findConsole().newMessageStream();

            final File srcFofn =
                    PluginUtil.writeTmpSrcFofn(
                            "CFPlugin-eclipse", true, PluginUtil.toFiles(fileNames));
            final File classpathFofn =
                    PluginUtil.writeTmpCpFile("CFPlugin-eclipse", true, classpath);

            final List<String> cmd =
                    createCommand(
                            srcFofn,
                            processors,
                            classpathFofn,
                            bootClasspath,
                            new PrintStream(out));

            if (verbose) {
                out.println(PluginUtil.join(" ", cmd));
                out.println();
                out.println("Classpath:\n  " + classpath + "\n");
                out.println("Source Files:\n  " + PluginUtil.join("\n\t", fileNames));
            }

            final String[] cmdArr = cmd.toArray(new String[cmd.size()]);
            checkResult = Command.exec(cmdArr);

            if (verbose) {
                printTrimmedOutput(out, checkResult);
                out.println("\n*******************\n");
            }

            srcFofn.delete();
            classpathFofn.delete();
        } catch (IOException e) {
            CheckerPlugin.logException(e, "Error calling javac");
        }
    }

    /**
     * @return The implicit annotations that should be used when running the Checker Framework
     *     compiler see -Djsr308_imports in the Checker Framework Manual
     */
    private String implicitAnnotations(final String[] processors) {
        return PluginUtil.join(File.pathSeparator, CheckerManager.getSelectedQuals(processors));
    }

    /**
     * Create a list where each item in the list forms a part of the command for calling the Checker
     * Framework compiler e.g. java -jar checker.jar -proc:only -classpath /this/projects/classpath
     * -processor checkers.nullness.NullChecker @srcFofnPath
     *
     * @param srcFofn A file of file names that contains the paths of all files to compile
     * @param processors Checkers to call on the given filenames
     * @param classpathFofn A file of file names for the Eclipse project's classpath
     * @param bootClassPath The Eclipse project's bootclasspath
     * @return A list of strings that (when separated by spaces) will form a call to the Checker
     *     Framework compiler
     */
    protected List<String> createCommand(
            final File srcFofn,
            final String[] processors,
            final File classpathFofn,
            final String bootClassPath,
            PrintStream out) {
        final Map<PluginUtil.CheckerProp, Object> props =
                new HashMap<PluginUtil.CheckerProp, Object>();

        final IPreferenceStore prefs = CheckerPlugin.getDefault().getPreferenceStore();
        if (prefs.getBoolean(CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS) && this.hasQuals) {
            props.put(PluginUtil.CheckerProp.IMPLICIT_IMPORTS, implicitAnnotations(processors));
        }

        final List<String> miscOptions = new ArrayList<String>();
        addPreferenceOptions(miscOptions, prefs);

        if (!miscOptions.isEmpty()) {
            props.put(PluginUtil.CheckerProp.MISC_COMPILER, miscOptions);
        }

        props.put(PluginUtil.CheckerProp.A_DETAILED_MSG, true);

        addProcessorOptions(props, prefs);

        final String procsStr = PluginUtil.join(",", processors);
        final String jdkPath = prefs.getString(CheckerPreferences.PREF_CHECKER_JDK_PATH);

        return PluginUtil.getCmd(
                null,
                null,
                null,
                srcFofn,
                procsStr,
                checkerJar.getAbsolutePath(),
                jdkPath,
                classpathFofn,
                bootClassPath,
                props,
                out,
                true,
                null);
    }

    /**
     * Any options found under the label "Additional Compiler Options" in the Checker Framework
     * Plugin preferences page
     *
     * @param store The preference store for this plugin
     */
    private void addPreferenceOptions(final List<String> opts, IPreferenceStore store) {
        // add options from preferences
        String argStr = store.getString(CheckerPreferences.PREF_CHECKER_ARGS);
        List<OptionLine> optionlines = OptionLine.parseOptions(argStr);
        for (final OptionLine optLine : optionlines) {
            if (optLine.isActive()) {
                opts.add(optLine.getArgument());
            }
        }
    }

    /**
     * Add options for type processing from the preferences
     *
     * @param opts
     */
    private void addProcessorOptions(
            Map<PluginUtil.CheckerProp, Object> opts, IPreferenceStore store) {
        // TODO: some input validation would be nice here. Especially for
        // the additional compiler flags, which could be checked against
        // the compiler.

        String skipUses = store.getString(CheckerPreferences.PREF_CHECKER_A_SKIP_CLASSES);
        if (!skipUses.isEmpty()) {
            opts.put(PluginUtil.CheckerProp.A_SKIP, skipUses);
        }

        String lintOpts = store.getString(CheckerPreferences.PREF_CHECKER_A_LINT);
        if (!lintOpts.isEmpty()) {
            opts.put(PluginUtil.CheckerProp.A_LINT, lintOpts);
        }

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_WARNS)) {
            opts.put(PluginUtil.CheckerProp.A_WARNS, Boolean.TRUE);
        }

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_NO_MSG_TEXT))
            opts.put(PluginUtil.CheckerProp.A_NO_MSG_TXT, Boolean.TRUE);

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_SHOW_CHECKS))
            opts.put(PluginUtil.CheckerProp.A_SHOW_CHECKS, Boolean.TRUE);

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_FILENAMES))
            opts.put(PluginUtil.CheckerProp.A_FILENAMES, Boolean.TRUE);
    }

    /**
     * Find a file within the plugin directory
     *
     * @param path The name of the file to find relative to the plugin directory
     * @return The path to the given file
     */
    public static File locatePluginFile(String path) {
        Bundle bundle = Platform.getBundle(CheckerPlugin.PLUGIN_ID);

        Path checkersJAR = new Path(path);
        URL checkersJarURL;
        try {
            checkersJarURL = FileLocator.toFileURL(FileLocator.find(bundle, checkersJAR, null));
        } catch (IOException e) {
            throw new RuntimeException("Exception locating plugin on path: " + path, e);
        } catch (NullPointerException npe) {
            throw new RuntimeException(
                    "Bundle= "
                            + bundle
                            + " ID="
                            + CheckerPlugin.PLUGIN_ID
                            + " checkerJar="
                            + checkersJAR,
                    npe);
        }

        File checkersJarFile;
        try {
            checkersJarFile = new File(checkersJarURL.toURI());
        } catch (URISyntaxException e) {
            checkersJarFile = new File(checkersJarURL.getPath());
        }

        return checkersJarFile;
    }

    /**
     * Parse the result of calling the Checker Framework compiler
     *
     * @return A list of JavacErrors parsed from the compiler output
     */
    public List<JavacError> getErrors() {
        return JavacError.parse(checkResult);
    }

    public static void printTrimmedOutput(final MessageConsoleStream out, final String output) {

        List<String> lines = Arrays.asList(output.split(Util.NL));
        for (final String line : lines) {
            out.println(JavacError.trimDetails(line));
        }
    }
}
