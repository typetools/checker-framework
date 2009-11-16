package jsr308.javac;

import java.io.*;
import java.net.*;
import java.util.*;

import jsr308.*;
import jsr308.util.*;

import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

/**
 * Runs the compiler and parses the output.
 */
public class CommandlineJavacRunner{
    public static final String CHECKERS_LOCATION = "lib/checkers/checkers.jar";
    public static final String JAVAC_LOCATION = "lib/langtools/binary/javac.jar";
    public static boolean VERBOSE = true;

    public List<JavacError> callJavac(List<String> fileNames, String processor, String classpath){
        try{
            String[] cmd = options(fileNames, processor, classpath);
            if (VERBOSE)
                System.out.println(toStringLinLines(cmd));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            if (VERBOSE)
                System.out.println(toStringNoCommas(cmd));
            Command.exec(cmd, ps);
            return JavacError.parse(baos.toString());
        }catch (IOException e){
            Activator.getDefault().logException(e, "Error calling javac");
            return null;
        }
    }

    private String toStringLinLines(String[] cmd){
        StringBuilder b = new StringBuilder();
        for (String c : cmd){
            b.append(c + "\n");
        }
        return b.toString();
    }

    // None of the Arrays.toString decorations like [, ]
    private String toStringNoCommas(String[] strings){
        StringBuilder b = new StringBuilder();
        for (String string : strings){
            b.append(string).append(" ");
        }
        return b.toString();
    }

    private String[] options(List<String> fileNames, String processor, String classpath) throws IOException{
        List<String> opts = new ArrayList<String>();
        opts.add(javaVM());
        opts.add("-ea:com.sun.tools");
        opts.add("-Xbootclasspath/p:" + javacJARlocation());
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
        // opts.add("/afs/csail.mit.edu/u/a/akiezun/eclipseworkspaces/runtime-New_configuration/daikon/java/daikon/VarInfo.java");
        return opts.toArray(new String[opts.size()]);
    }

    private String javaVM(){
        String sep = System.getProperty("file.separator");
        return System.getProperty("java.home") + sep + "bin" + sep + "java";
    }

    private String classpath(String classpath){
        return classpath;
    }

    private String javacJARlocation() throws IOException{
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);

        Path javacJAR = new Path(JAVAC_LOCATION);
        URL javacJarURL = FileLocator.toFileURL(FileLocator.find(bundle, javacJAR, null));
        return javacJarURL.getPath();
    }

    // This used to be used. Now we just scan the classpath. The checkers.jar must be on the classpath anyway.
    // XXX The problem is what to do if the checkers.jar on the classpath is different from the one in the plugin.
    public static String checkersJARlocation(){
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);

        Path checkersJAR = new Path(CHECKERS_LOCATION);
        URL checkersJarURL;
        try{
            checkersJarURL = FileLocator.toFileURL(FileLocator.find(bundle, checkersJAR, null));
        }catch (IOException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }

        return checkersJarURL.getPath();
    }
}
