package org.checkerframework.framework.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.javacutil.PluginUtil;

public class CheckerDevelMain extends CheckerMain {

    private static final String PROP_PREFIX = "CheckerDevelMain";
    private static final String BINARY_PROP = PROP_PREFIX + ".binary";
    private static final String CP_PROP = PROP_PREFIX + ".cp";
    private static final String PP_PROP = PROP_PREFIX + ".pp";
    private static final String COMPILE_BCP_PROP = PROP_PREFIX + ".compile.bcp";
    private static final String RUNTIME_CP_PROP = PROP_PREFIX + ".runtime.cp";
    private static final String VERBOSE_PROP = PROP_PREFIX + ".verbose";

    public static void main(final String[] args) {

        final String cp = System.getProperty(CP_PROP);
        final String pp = System.getProperty(PP_PROP);
        final String runtimeCp = System.getProperty(RUNTIME_CP_PROP);
        final String compileBcp = System.getProperty(COMPILE_BCP_PROP);
        final String binDir = System.getProperty(BINARY_PROP);
        final boolean verbose = PluginUtil.getBooleanSystemProperty(VERBOSE_PROP);

        if (verbose) {
            System.out.println("CheckerDevelMain:");
            System.out.println("Prepended to classpath:     " + cp);
            System.out.println("Prepended to processor classpath:   " + pp);
            System.out.println("Prepended to compile bootclasspath: " + compileBcp);
            System.out.println("Prepended to runtime classpath:     " + runtimeCp);
            System.out.println("Binary Dir:                 " + binDir);
        }

        assert (binDir != null)
                : BINARY_PROP
                        + " must specify a binary directory in which "
                        + "checker.jar, etc... are usually built";

        assert (cp != null) : CP_PROP + " must specify a path entry to prepend to the CLASSPATH";
        assert (pp != null)
                : PP_PROP + " must specify a path entry to prepend to the processor path";

        assert (runtimeCp != null)
                : RUNTIME_CP_PROP
                        + " must specify a path entry to prepend to the Java classpath when running javac"; // TODO: Fix the assert messages
        assert (compileBcp != null)
                : COMPILE_BCP_PROP
                        + " must specify a path entry to prepend to the compiler bootclasspath";

        // The location that checker.jar would be in if we have built it
        final File checkersLoc = new File(binDir, "checker.jar");
        ArrayList<String> argsPlusEa = new ArrayList<>(args.length + 1);
        argsPlusEa.addAll(Arrays.asList(args));
        argsPlusEa.add("-J-ea");
        final CheckerDevelMain program = new CheckerDevelMain(checkersLoc, argsPlusEa);
        final int exitStatus = program.invokeCompiler();
        System.exit(exitStatus);
    }

    /**
     * Construct all the relevant file locations and java version given the path to this jar and a
     * set of directories in which to search for jars.
     */
    public CheckerDevelMain(File searchPath, List<String> args) {
        super(searchPath, args);
    }

    @Override
    public void assertValidState() {}

    @Override
    protected List<String> createRuntimeClasspath(final List<String> argsList) {
        return prependPathOpts(RUNTIME_CP_PROP, new ArrayList<>());
    }

    @Override
    protected List<String> createCompilationBootclasspath(final List<String> argsList) {
        return prependPathOpts(COMPILE_BCP_PROP, super.createCompilationBootclasspath(argsList));
    }

    @Override
    protected List<String> createCpOpts(final List<String> argsList) {
        return prependPathOpts(CP_PROP, super.createCpOpts(argsList));
    }

    @Override
    protected List<String> createPpOpts(final List<String> argsList) {
        return prependPathOpts(PP_PROP, super.createPpOpts(argsList));
    }

    private static List<String> prependPathOpts(
            final String pathProp, final List<String> pathOpts, final String... otherPaths) {
        final String cp = System.getProperty(pathProp);

        final List<String> newPathOpts = new ArrayList<>();

        if (!cp.trim().isEmpty()) {
            newPathOpts.addAll(Arrays.asList(cp.split(File.pathSeparator)));
        }

        newPathOpts.addAll(Arrays.asList(otherPaths));
        newPathOpts.addAll(pathOpts);

        return newPathOpts;
    }
}
