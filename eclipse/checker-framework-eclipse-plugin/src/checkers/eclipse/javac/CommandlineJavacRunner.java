package checkers.eclipse.javac;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.*;

import checkers.eclipse.util.PluginUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;

import checkers.eclipse.CheckerPlugin;
import checkers.eclipse.actions.CheckerManager;
import checkers.eclipse.prefs.CheckerPreferences;
import checkers.eclipse.util.Command;
import checkers.eclipse.util.JavaUtils;

/**
 * Runs the Checker Framework compiler (i.e. the JSR308 compiler with the
 * appropriate bootclasspath, classpath, and option arguments.
 */
public class CommandlineJavacRunner implements CheckersRunner {

    /**
     * The location of the checkers.jar relative to the plugin directory
     */
    public static final String CHECKERS_LOCATION = "lib/checkers.jar";

    public static boolean VERBOSE = false;

    /**
     * Names of source files to compile
     */
    private final List<String> fileNames;

    /**
     * checkers to run
     */
    private final String processors;

    /**
     * The classpath for this project
     */
    private final String classpath;

    /**
     * The bootclasspath for this project
     */
    private final String bootClasspath;

    /**
     * The output of running the Checker Framework Compiler
     */
    private String checkResult;

    public CommandlineJavacRunner(String[] fileNames, String processors,
            String classpath, String bootClasspath) {
        this.fileNames = Arrays.asList(fileNames);
        this.processors = processors;
        this.classpath = classpath;
        this.bootClasspath = bootClasspath;
    }

    /**
     * Write the names of all files to be checked into a file-of-file-names(fofn) and
     * then call the Checker Framework in "check-only" mode to check the files
     * listed in the fofn
     */
    public void run()
    {
        try {
            MessageConsoleStream out = CheckerPlugin.findConsole().newMessageStream();

        	final File srcFofn = PluginUtil.writeTmpFofn("CFPlugin-eclipse", ".fofn", true, PluginUtil.toFiles(fileNames));
        	final List<String> cmd = createCommand(srcFofn, processors, classpath, bootClasspath, new PrintStream(out));

            if (VERBOSE)
                out.println(JavaUtils.join("\n", cmd));

            final String [] cmdArr = cmd.toArray(new String[cmd.size()]);
            checkResult = Command.exec(cmdArr);

            if (VERBOSE)
                out.println(checkResult);

            srcFofn.delete();
        } catch (IOException e) {
            CheckerPlugin.logException(e, "Error calling javac");
        }
    }

    /**
     * @return The implicit annotations that should be used when running the Checker Framework compiler
     * see -Djsr308_imports in the Checker Framework manual
     */
    private String implicitAnnotations() {
        return JavaUtils.join(File.pathSeparator, CheckerManager.getInstance().getSelectedQuals());
    }

    /**
     * Create a list where each item in the list forms a part of the command for calling the Checker Framework compiler
     * e.g.
     * java -jar checkers.jar -proc:only -classpath /this/projects/classpath -processor checkers.nullness.NullChecker @srcFofnPath
     * @param srcFofn A file-of-filenames that contains the paths of all files to compile
     * @param processors Checkers to call on the given filenames
     * @param classpath  The Eclipse project's classpath
     * @param bootClassPath The Eclipse project's bootclasspath
     * @return A list of strings that (when separated by spaces) will form a call to the Checker Framework compiler
     */
    private List<String> createCommand( final File srcFofn, final String processors,
                                        final String classpath, final String bootClassPath,
                                        PrintStream out)  {
        final Map<PluginUtil.CheckerProp, Object> props = new HashMap<PluginUtil.CheckerProp, Object>();

        final IPreferenceStore prefs = CheckerPlugin.getDefault().getPreferenceStore();
        if (prefs.getBoolean(CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS)) {
            props.put(PluginUtil.CheckerProp.IMPLICIT_IMPORTS, implicitAnnotations());
        }

        final List<String> miscOptions = new ArrayList<String>();
        addPreferenceOptions(miscOptions, prefs);
        props.put(PluginUtil.CheckerProp.MISC_COMPILER, miscOptions);

        addProcessorOptions(props, prefs);

        final String jdkPath = prefs.getString(CheckerPreferences.PREF_CHECKER_JDK_PATH);

        return PluginUtil.getCmd(null, srcFofn, processors,
                                 locatePluginFile(CHECKERS_LOCATION),
                                 jdkPath, classpath, bootClassPath,
                                 props, out);
    }
    
    /**
     * Any options found under the label "Additional Compiler Options" in the Checker Framework Plugin
     * preferences page
     * @param cmd    A list to which the options should be added
     * @param store  The preference store for this plugin
     */
    private void addPreferenceOptions(final List<String> opts, IPreferenceStore store) {

        // add options from preferences
        String argStr = store.getString(CheckerPreferences.PREF_CHECKER_ARGS);

        if (!argStr.isEmpty()) {
            String[] prefOpts = argStr.split("\\s+");

            for (String opt : prefOpts) {
                opts.add(opt);
            }
        }
    }

    /**
     * Add options for type processing from the preferences
     * 
     * @param opts
     */
    private void addProcessorOptions(Map<PluginUtil.CheckerProp, Object> opts, IPreferenceStore store)
    {
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

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_WARNS)){
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
     * @param path The name of the file to find relative to the plugin directory
     * @return The path to the given file
     */
    public static String locatePluginFile(String path) {
        Bundle bundle = Platform.getBundle(CheckerPlugin.PLUGIN_ID);

        Path checkersJAR = new Path(path);
        URL checkersJarURL;
        try {
            checkersJarURL = FileLocator.toFileURL(FileLocator.find(bundle, checkersJAR, null));
        } catch (IOException e) {
            throw new RuntimeException("Exception locating plugin on path: " + path, e);
        } catch (NullPointerException npe) {
        	throw new RuntimeException("Bundle= " + bundle + " ID=" + CheckerPlugin.PLUGIN_ID + " checkersJar=" + checkersJAR, npe);
        }

        return checkersJarURL.getPath();
    }

    /**
     * Parse the result of calling the Checker Framework compiler
     * @return A list of JavacErrors parsed from the compiler output
     */
    public List<JavacError> getErrors() {
        return JavacError.parse(checkResult);
    }
}
