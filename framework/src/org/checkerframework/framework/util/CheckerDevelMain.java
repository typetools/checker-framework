package org.checkerframework.framework.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckerDevelMain extends CheckerMain {

    private static final String PROP_PREFIX  = "CheckerDevelMain";
    private static final String BINARY_PROP      = PROP_PREFIX + ".binary";
    private static final String CP_PROP          = PROP_PREFIX + ".cp";
    private static final String COMPILE_BCP_PROP = PROP_PREFIX + ".compile.bcp";
    private static final String RUNTIME_BCP_PROP = PROP_PREFIX + ".runtime.bcp";
    private static final String VERBOSE_PROP = PROP_PREFIX + ".verbose";


    public static void main(final String[] args)  {

        final String cp          = System.getProperty( CP_PROP );
        final String runtimeBcp  = System.getProperty( RUNTIME_BCP_PROP );
        final String compileBcp  = System.getProperty( COMPILE_BCP_PROP );
        final String binDir  = System.getProperty( BINARY_PROP  );
        final String verbose = System.getProperty( VERBOSE_PROP );


        if (verbose != null && verbose.equalsIgnoreCase("TRUE")) {
            System.out.print("CheckerDevelMain:\n" +
                    "Prepended to classpath:     " + cp         +  "\n" +
                    "Prepended to compile bootclasspath: " + compileBcp +  "\n" +
                    "Prepended to runtime bootclasspath: " + runtimeBcp +  "\n" +
                    "Binary Dir:                 " + binDir     +  "\n"
            );
        }

        assert (binDir != null) :
                BINARY_PROP + " must specify a binary directory in which " +
                "checker.jar, javac.jar, etc... are usually built";

        assert (cp         != null) : CP_PROP  + " must specify a path entry to prepend to the CLASSPATH";
        assert (runtimeBcp != null) : RUNTIME_BCP_PROP + " must specify a path entry to prepend to the Java bootclasspath when running Javac";  //TODO: Fix the assert messages
        assert (compileBcp != null) : COMPILE_BCP_PROP + " must specify a path entry to prepend to the compiler bootclasspath";

        // The location that checker.jar would be in if we have built it
        final File checkersLoc = new File(binDir, "checker.jar");
        final CheckerDevelMain program = new CheckerDevelMain(checkersLoc, args);
        final int exitStatus = program.invokeCompiler();
        System.exit(exitStatus);
    }

    /**
     * Construct all the relevant file locations and java version given the path to this jar and
     * a set of directories in which to search for jars
     */
    public CheckerDevelMain(File searchPath, String[] args) {
        super(searchPath, args);
    }



    @Override
    public void assertValidState() {
    }


    @Override
    protected List<String> createRuntimeBootclasspath(final List<String> argsList) {
        return prependPathOpts( RUNTIME_BCP_PROP, new ArrayList<String>());
    }

    @Override
    public void addMainArgs(final List<String> args) {
        args.add("com.sun.tools.javac.Main");
    }

    @Override
    protected List<String> createCompilationBootclasspath(final List<String> argsList) {
        return prependPathOpts( COMPILE_BCP_PROP, super.createCompilationBootclasspath(argsList));
    }

    @Override
    protected List<String> createCpOpts(final List<String> argsList) {
        return prependPathOpts( CP_PROP, super.createCpOpts(argsList));
    }

    private static List<String> prependPathOpts(final String pathProp, final List<String> pathOpts, final String ... otherPaths) {
        final String cp     = System.getProperty(pathProp);

        final List<String> newPathOpts = new ArrayList<String>();

        if (!cp.trim().isEmpty()) {
            newPathOpts.addAll(Arrays.asList(cp.split(File.pathSeparator)));
        }

        newPathOpts.addAll(Arrays.asList(otherPaths));
        newPathOpts.addAll(pathOpts);

        return newPathOpts;
    }

}
