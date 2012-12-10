package checkers.eclipse.javac;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
 * Runs the compiler and parses the output.
 */
public class CommandlineJavacRunner implements CheckersRunner
{
    // TODO: some constants are here, some in JavacRunner
    // TODO: what is the relation between those classes?
    
    public static final String CHECKERS_LOCATION = "lib/checkers.jar";
    public static final String JAVAC_LOCATION = "lib/javac.jar";
    public static final String JSR308ALL_LOCATION = "lib/jsr308-all.jar";
    public static final String JDKJAR_LOCATION = "lib/jdk.jar";

    public static boolean VERBOSE = true;

    private final List<String> fileNames;
    private final String processors;
    private final String classpath;
    private final String bootclasspath;

    private String checkResult;

    public CommandlineJavacRunner(String[] fileNames, String processors,
            String classpath, String bootclasspath)
    {
        this.fileNames = Arrays.asList(fileNames);
        this.processors = processors;
        this.classpath = classpath;
        this.bootclasspath = bootclasspath;
    }

    public void run()
    {
        try
        {
            String[] cmd = options(fileNames, processors, classpath,
                    bootclasspath);
            if (VERBOSE)
                System.out.println(JavaUtils.join("\n", cmd));

            MessageConsoleStream out = CheckerPlugin.findConsole()
                    .newMessageStream();

            checkResult = Command.exec(cmd);

            if (VERBOSE)
                out.println(checkResult);

        }catch (IOException e)
        {
            CheckerPlugin.logException(e, "Error calling javac");
        }
    }

    private String implicitAnnotations()
    {
        return JavaUtils.join(File.pathSeparator, CheckerManager.getInstance()
                .getSelectedQuals());
    }

    private String[] options(List<String> fileNames, String processors,
            String classpath, String bootclasspath) throws IOException
    {
        boolean usingImplicitAnnotations = usingImplicitAnnotations();
        List<String> opts = new ArrayList<String>();
        opts.add(javaVM());

        opts.add("-ea:com.sun.tools");
        opts.add("-Xbootclasspath/p:" + javacJARlocation());

        if (usingImplicitAnnotations)
        {
            opts.add("-Djsr308_imports=" + implicitAnnotations());
        }

        opts.add("-jar");
        opts.add(javacJARlocation());
        // if (VERBOSE)
        // opts.add("-verbose");
        opts.add("-proc:only");
        opts.add("-bootclasspath");
        opts.add(annojdkJARlocation() + File.pathSeparator + bootclasspath + File.pathSeparator + javacJARlocation());

        opts.add("-classpath");
        if (checkersOnClassspath())
        {
            opts.add(classpath);
        }
        else
        {
        	final String checkerFile = locatePluginFile(CHECKERS_LOCATION);
        	if( checkerFile != null ) {
        		opts.add(classpath + File.pathSeparator + checkerFile);
        	} else {
        		throw new RuntimeException("Could not locate checkers.jar at " + CHECKERS_LOCATION);
        	}
        }

        opts.add("-processor");
        opts.add(processors);

        if (usingImplicitAnnotations)
        {
            opts.add("-implicit:class");
        }

        // add options from preferences
        String argStr = CheckerPlugin.getDefault().getPreferenceStore()
                .getString(CheckerPreferences.PREF_CHECKER_ARGS);

        if (!argStr.isEmpty())
        {
            String[] prefOpts = argStr.split("\\s+");

            for (String opt : prefOpts)
            {
                opts.add(opt);
            }
        }

        addProcessorOptions(opts);

        // opts.add("-J-Xms256M");
        // opts.add("-J-Xmx515M");
        opts.addAll(fileNames);

        return opts.toArray(new String[opts.size()]);
    }

    private boolean checkersOnClassspath()
    {
        return Pattern.matches(".*checkers.jar(:|$).*", classpath);
    }

    private boolean usingImplicitAnnotations()
    {
        return CheckerPlugin.getDefault().getPreferenceStore()
                .getBoolean(CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS);
    }

    /**
     * Add options for type processing from the preferences
     * 
     * @param opts
     */
    private void addProcessorOptions(List<String> opts)
    {
        // TODO: some input validation would be nice here. Especially for
        // the additional compiler flags, which could be checked against
        // the compiler.

        IPreferenceStore store = CheckerPlugin.getDefault()
                .getPreferenceStore();

        String skipUses = store
                .getString(CheckerPreferences.PREF_CHECKER_A_SKIP_CLASSES);
        if (!skipUses.isEmpty())
        {
            opts.add("-AskipUses=" + skipUses);
        }

        String lintOpts = store
                .getString(CheckerPreferences.PREF_CHECKER_A_LINT);
        if (!lintOpts.isEmpty())
        {
            opts.add("-Alint=" + lintOpts);
        }

        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_WARNS))
            opts.add("-Awarns");
        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_NO_MSG_TEXT))
            opts.add("-Anomsgtext");
        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_SHOW_CHECKS))
            opts.add("-Ashowchecks");
        if (store.getBoolean(CheckerPreferences.PREF_CHECKER_A_FILENAMES))
            opts.add("-Afilenames");
    }

    private String javaVM()
    {
        IPreferenceStore store = CheckerPlugin.getDefault()
                .getPreferenceStore();
        String jdkPath = store
                .getString(CheckerPreferences.PREF_CHECKER_JDK_PATH);
        String sep = System.getProperty("file.separator");
        String javahome;

        if (jdkPath.isEmpty() || !(new File(jdkPath).exists()))
            javahome = System.getProperty("java.home");
        else
            javahome = jdkPath;

        return javahome + sep + "bin" + sep + "java";
    }

    private String javacJARlocation() throws IOException
    {
        return findJARlocation(JSR308ALL_LOCATION);
    }

    private String annojdkJARlocation() throws IOException
    {
        return findJARlocation(JDKJAR_LOCATION);
    }
    
    private String findJARlocation(String name) throws IOException {
        Bundle bundle = CheckerPlugin.getDefault().getBundle();
        URL javacJarURL;

        javacJarURL = bundle.getEntry(name);
        javacJarURL = FileLocator.toFileURL(javacJarURL);

        // hack to get around broken URI encoding in FileLocator
        // see bug #140596 in Eclipse
        URI javacJarURI;
        try
        {
            javacJarURI = new URI(javacJarURL.toString().replaceAll(" ", "%20"));
        }catch (URISyntaxException e)
        {
            CheckerPlugin.logException(e, e.getMessage());
            return "";
        }

        File javacFile = new File(javacJarURI);
        return javacFile.getAbsolutePath();
    }

    public static String locatePluginFile(String path)
    {
        Bundle bundle = Platform.getBundle(CheckerPlugin.PLUGIN_ID);

        Path checkersJAR = new Path(path);
        URL checkersJarURL;
        try
        {
            checkersJarURL = FileLocator.toFileURL(FileLocator.find(bundle,
                    checkersJAR, null));
        } catch (IOException e)
        {
            throw new RuntimeException("Exception locating plugin on path: " + path, e);
        } catch (NullPointerException npe) {
        	throw new RuntimeException("Bundle= " + bundle + " ID=" + CheckerPlugin.PLUGIN_ID + " checkersJar=" + checkersJAR, npe);
        }

        return checkersJarURL.getPath();
    }

    public List<JavacError> getErrors()
    {
        return JavacError.parse(checkResult);
    }
}
