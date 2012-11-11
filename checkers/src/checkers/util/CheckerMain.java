package checkers.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * The main class for the Checkers when using the binary distribution, that
 * delegates the compilation invocation to javac.<p>
 *
 * The class has two responsibilities:
 * <ul>
 * <li>it adds the annotated JDK to the bootclasspath
 * <li>if invoked when the compiler classes is in the bootclasspath (e.g. with
 *     Apple JVM), it restarts the JVM and prepends the JSR 308 compiler to
 *     bootclasspath.
 * </ul>
 */
public class CheckerMain {
    private static final String VERSION = "1";

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

    private static File tempJDKPath() {
        String userSupplied = System.getProperty("jsr308.jdk");
        if (userSupplied != null)
            return new File(userSupplied);

        String tmpFolder = System.getProperty("java.io.tmpdir");
        File jdkFile = new File(tmpFolder, "jdk-" + VERSION + ".jar");
        return jdkFile;
    }

    /** returns the path to annotated JDK */
    private static String jdkJar() {
        // case 1: running from binary
        String thisJar = findPathJar(CheckerMain.class);
        File potential = new File(new File(thisJar).getParentFile(), "jdk.jar");
        if (potential.exists()) {
            //System.out.println("from adjacent jdk.jar");
            return potential.getPath();
        }

        // case 2: there was a temporary copy
        File jdkFile = tempJDKPath();
        //System.out.println(jdkFile);
        if (jdkFile.exists()) {
            //System.out.println("From temporary");
            return jdkFile.getPath();
        }

        // case 3: extract zipped jdk.jar
        try {
            extractFile(thisJar, "jdk.jar", jdkFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (jdkFile.exists()) {
            //System.out.println("Extracted jar");
            return jdkFile.getPath();
        }

        throw new AssertionError("Couldn't find annotated JDK");
    }

    private static void extractFile(String jar, String fileName, File output) throws Exception {
        int BUFFER = 2048;

        File jarFile = new File(jar);
        if (! jarFile.exists()) {
            throw new Exception("File does not exist: " + jarFile);
        }
        ZipFile zip;
        try {
            zip = new ZipFile(jarFile);
        } catch (Exception e) {
            throw new RuntimeException("Problem opening zip file " + jarFile);
        }

        ZipEntry entry = zip.getEntry(fileName);
        assert !entry.isDirectory();

        BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
        int currentByte;
        // establish buffer for writing file
        byte data[] = new byte[BUFFER];

        // write the current file to disk
        FileOutputStream fos = new FileOutputStream(output);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

        // read and write until last byte is encountered
        while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, currentByte);
        }
        dest.flush();
        dest.close();
        is.close();
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
            // Keep this method name synchronized!
            clazz.getMethod("getReceiverParameter");
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

    /** Execute the commands, with IO redirection */
    static void execute(Iterable<String> cmdArray) {
        String command = constructCommand(cmdArray);
        CLibrary.INSTANCE.system(command);
    }
}
