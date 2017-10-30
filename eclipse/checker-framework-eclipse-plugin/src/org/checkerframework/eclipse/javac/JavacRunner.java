package org.checkerframework.eclipse.javac;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;

/**
 * This class is used to run the checker from the Sun Compiler API rather than using the
 * commandline.
 *
 * @author asumu
 */
public class JavacRunner implements CheckersRunner {
    public static final String CHECKERS_JAR_LOCATION = "lib/checkers.jar";
    public static final String JAVAC_LOCATION = "lib/javac.jar";
    public static final String JDK_LOCATION = "lib/jdk.jar";
    public static final List<String> IMPLICIT_ARGS =
            Arrays.asList("checkers.nullness.quals.*", "checkers.interning.quals.*");

    private final Iterable<String> fileNames;
    private final Iterable<String> processors;
    private final String classpath;
    private final DiagnosticCollector<JavaFileObject> collector;
    private final boolean hasQuals;

    public JavacRunner(
            String[] fileNames, String[] processors, String classpath, boolean hasQuals) {
        this.collector = new DiagnosticCollector<JavaFileObject>();
        this.fileNames = Arrays.asList(fileNames);
        this.processors = Arrays.asList(processors);
        this.classpath = classpath;
        this.hasQuals = hasQuals;
    }

    /**
     * Runs the compiler on the selected files using the given processor
     *
     * @param fileNames files that need to be type-checked
     * @param processors Type processors to run
     * @param classpath The classpath to reference in compilation
     */
    public void run() {
        Iterable<String> opts;

        opts = getOptions(processors, classpath);

        // The following code uses the compiler's internal APIs, which are
        // volatile. (see warning in JavacTool source)
        JavacTool tool = JavacTool.create();
        JavacFileManager manager = null; // tool.getStandardFileManager(collector, null, null);

        Iterable<? extends JavaFileObject> fileObjs =
                manager.getJavaFileObjectsFromStrings(fileNames);

        CheckerPlugin.getDefault();
        MessageConsole console = CheckerPlugin.findConsole();
        MessageConsoleStream stream = console.newMessageStream();
        Writer writer = new OutputStreamWriter(stream);

        JavacTask task = tool.getTask(writer, manager, collector, opts, null, fileObjs);

        task.call();
        manager.close();
    }

    private Iterable<String> getOptions(Iterable<String> processors, String classpath) {
        List<String> opts = new ArrayList<String>();

        opts.add("-verbose");
        opts.add("-proc:only");

        try {
            opts.add(
                    "-Xbootclasspath/p:"
                            + getLocation(JAVAC_LOCATION)
                            + ":"
                            + getLocation(JDK_LOCATION)
                            + ":");
        } catch (IOException e) {
            CheckerPlugin.logException(e, e.getMessage());
        }

        opts.add("-XprintProcessorInfo");
        opts.add("-Xprefer:source");

        // Build the processor arguments, comma separated
        StringBuilder processorStr = new StringBuilder();
        Iterator<String> itr = processors.iterator();

        while (itr.hasNext()) {
            processorStr.append(itr.next());
            if (itr.hasNext()) {
                processorStr.append(",");
            }
        }

        opts.add("-processor");
        opts.add(processorStr.toString());

        // Processor options
        addProcessorOptions(opts);

        // Classpath
        opts.add("-cp");
        opts.add(classpath);

        return opts;
    }

    /**
     * Add options for type processing from the preferences
     *
     * @param opts
     */
    private void addProcessorOptions(List<String> opts) {
        IPreferenceStore store = CheckerPlugin.getDefault().getPreferenceStore();

        String skipUses = store.getString(CheckerPreferences.PREF_CHECKER_A_SKIP_CLASSES);
        if (!skipUses.isEmpty()) {
            opts.add("-AskipUses=" + skipUses);
        }

        String lintOpts = store.getString(CheckerPreferences.PREF_CHECKER_A_LINT);
        if (!lintOpts.isEmpty()) {
            opts.add("-Alint=" + lintOpts);
        }

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_WARNS)) opts.add("-Awarns");
        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_NO_MSG_TEXT))
            opts.add("-Anomsgtext");
        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_SHOW_CHECKS))
            opts.add("-Ashowchecks");
        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_FILENAMES)) opts.add("-Afilenames");

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS) && hasQuals) {
            StringBuilder builder = new StringBuilder();
            for (String annClass : IMPLICIT_ARGS) {
                builder.append(annClass);
                builder.append(":");
            }

            // chop off the last :
            builder.setLength(builder.length() - 1);
            builder.trimToSize();

            // TODO: this is disabled for now since it doesn't work
            // opts.add("-J-Djsr308_imports=\"" + builder.toString() + "\"");
        }
    }

    private String getLocation(String path) throws IOException {
        Bundle bundle = Platform.getBundle(CheckerPlugin.PLUGIN_ID);

        Path javacJAR = new Path(path);
        URL javacJarURL = FileLocator.toFileURL(FileLocator.find(bundle, javacJAR, null));
        return javacJarURL.getPath();
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return collector.getDiagnostics();
    }

    public List<JavacError> getErrors() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = getDiagnostics();
        List<JavacError> javacErrors = new ArrayList<JavacError>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getSource() != null) {
                javacErrors.add(new JavacError(diagnostic));
            } else { // TODO: TEST PRINTING THIS TO THE CONSOLE
                System.out.println(
                        "No source for diagnostic at: "
                                + diagnostic.getLineNumber()
                                + " Message "
                                + diagnostic.getMessage(null));
            }
        }
        return javacErrors;
    }
}
