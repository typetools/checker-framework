package checkers.util;

import com.sun.jna.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * The main class for the Checkers when using the binary distribution, that
 * delegates the compilation invocation to javac.
 *
 * The class has two responsibilities:
 *   - it adds the annotated JDK to the bootclasspath
 *   - if invoked when the compiler classes in the bootclasspath (e.g. with
 *     Apple JVM), it restarts the JVM and prepend JSR 308 compiler to
 *     bootclasspath.
 */
public class CheckerMain {

    public static void main(String[] args) throws Exception {
        if (isUsingJSR308Compiler()) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "-Xbootclasspath/p:" + jdkJar();
            System.arraycopy(args, 0, newArgs, 1, args.length);
            com.sun.tools.javac.Main.main(newArgs);
        } else {
            System.out.println("Manipulating bootclasspath");
            List<String> cmdArgs = newCommandArgs(args);
            execute(cmdArgs);
        }
    }

    /**
     * The new command to restart java, with compiler jar prepended to the
     * bootclasspath
     */
    static List<String> newCommandArgs(String[] currArgs) {
        List<String> args = new ArrayList<String>(currArgs.length + 5);
        args.add("java");

        // Java's Arguments
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        args.addAll(mxBean.getInputArguments());

        String jarPath = findPathJar(CheckerMain.class);
        args.add("-Xbootclasspath/p:" + jarPath);
        args.add("-jar");
        args.add(jarPath);
        args.add("-Xbootclasspath/p:" + jdkJar());
        args.addAll(Arrays.asList(currArgs));
        return args;
    }

    /** returns the path to annotated JDK */
    private static String jdkJar() {
        String thisJar = findPathJar(CheckerMain.class);
        String parent = new File(thisJar).getParentFile().getPath();
        return parent + File.separator + "jdk.jar";
    }

    /**
     * Find the jar file containing the annotated JDK (i.e. jar containing
     * this file
     */
    public static String findPathJar(Class<?> context) throws IllegalStateException {
        if (context == null) context = CheckerMain.class;
        String rawName = context.getName();
        String classFileName;
        /* rawName is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */ {
            int idx = rawName.lastIndexOf('.');
            classFileName = (idx == -1 ? rawName : rawName.substring(idx+1)) + ".class";
        }

        String uri = context.getResource(classFileName).toString();
        if (uri.startsWith("file:")) throw new IllegalStateException("This class has been loaded from a directory and not from a jar file.");
        if (!uri.startsWith("jar:file:")) {
            int idx = uri.indexOf(':');
            String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
            throw new IllegalStateException("This class has been loaded remotely via the " + protocol +
                    " protocol. Only loading from a jar on the local file system is supported.");
        }

        int idx = uri.indexOf('!');
        //As far as I know, the if statement below can't ever trigger, so it's more of a sanity check thing.
        if (idx == -1) throw new IllegalStateException("You appear to have loaded this class from a local jar file, but I can't make sense of the URL!");

        try {
            String fileName = URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset().name());
            return new File(fileName).getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("default charset doesn't exist. Your VM is borked.");
        }
    }

    /** Returns true if the JSR308 classes are being used */
    static boolean isUsingJSR308Compiler() {
        try {
            Class<?> clazz = com.sun.source.tree.MethodTree.class;
            clazz.getMethod("getReceiverAnnotations");
            return true;
        } catch (Throwable e) {
            // Error either due to MethodTree not loadable, or that the JSR308
            // specific getReceiverAnnotations() method isn't present
            return false;
        }
    }

    /**
     * Helper class to invoke the libc system() native call
     *
     * Using the system() native call, rather than Runtime.exec(), to handle
     * IO "redirection"
     **/
    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)Native.loadLibrary("c", CLibrary.class);
        int system(String command);
     }

    /**
     * Helper method to do the proper escaping of arguments to pass to
     * system()
     */
    static String constructCommand(Iterable<String> args) {
        StringBuilder sb = new StringBuilder();

        for (String arg: args) {
            sb.append('"');
            sb.append(arg.replace("\"", "\\\""));
            sb.append("\" ");
        }

        return sb.toString();
    }

    /** Execute the cmmands, with IO redirection */
    static void execute(Iterable<String> cmdArray) {
        String command = constructCommand(cmdArray);
        CLibrary.INSTANCE.system(command);
    }
}
