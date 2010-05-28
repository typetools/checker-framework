package checkers.eclipse.javac;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.ui.console.*;
import org.osgi.framework.*;

import checkers.eclipse.*;
import checkers.eclipse.util.*;

/**
 * Runs the compiler and parses the output.
 */
public class CommandlineJavacRunner {
    public static final String CHECKERS_LOCATION = "lib/checkers.jar";
    public static final String JAVAC_LOCATION = "lib/javac.jar";
    public static final List<String> IMPLICIT_ARGS = Arrays.asList(
            "checkers.nullness.quals.*", "checkers.igj.quals.*",
            "checkers.javari.quals.*", "checkers.interning.quals.*");

    public static boolean VERBOSE = true;

    public List<JavacError> callJavac(List<String> fileNames, String processor,
            String classpath) {
        try {
            String[] cmd = options(fileNames, processor, classpath);
            if (VERBOSE)
                System.out.println(JavaUtils.join("\n", cmd));

            MessageConsoleStream out = Activator.findConsole()
                    .newMessageStream();

            String result = Command.exec(cmd);

            if (VERBOSE)
                out.println(result);

            return JavacError.parse(result);
        } catch (IOException e) {
            Activator.logException(e, "Error calling javac");
            return null;
        }
    }

    @SuppressWarnings("unused")
    private String implicitAnnotations() {
        return JavaUtils.join(File.pathSeparator, IMPLICIT_ARGS);
    }

    private String[] options(List<String> fileNames, String processor,
            String classpath) throws IOException {
        List<String> opts = new ArrayList<String>();
        opts.add(javaVM());
        opts.add("-ea:com.sun.tools");
        opts.add("-Xbootclasspath/p:" + javacJARlocation());

        // opts.add("-Djsr308_imports=\"" + implicitAnnotations() + "\"");

        opts.add("-jar");
        opts.add(javacJARlocation());
        // if (VERBOSE)
        // opts.add("-verbose");
        opts.add("-proc:only");
        opts.add("-classpath");
        opts.add(classpath(classpath));
        opts.add("-processor");
        opts.add(processor);
        // opts.add("-J-Xms256M");
        // opts.add("-J-Xmx515M");
        opts.addAll(fileNames);
        return opts.toArray(new String[opts.size()]);
    }

    private String javaVM() {
        String sep = System.getProperty("file.separator");
        return System.getProperty("java.home") + sep + "bin" + sep + "java";
    }

    private String classpath(String classpath) {
        return classpath;
    }

    private String javacJARlocation() throws IOException {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);

        Path javacJAR = new Path(JAVAC_LOCATION);
        URL javacJarURL = FileLocator.toFileURL(FileLocator.find(bundle,
                javacJAR, null));
        return javacJarURL.getPath();
    }

    // This used to be used. Now we just scan the classpath. The checkers.jar
    // must be on the classpath anyway.
    // XXX The problem is what to do if the checkers.jar on the classpath is
    // different from the one in the plugin.
    public static String checkersJARlocation() {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);

        Path checkersJAR = new Path(CHECKERS_LOCATION);
        URL checkersJarURL;
        try {
            checkersJarURL = FileLocator.toFileURL(FileLocator.find(bundle,
                    checkersJAR, null));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }

        return checkersJarURL.getPath();
    }
}
