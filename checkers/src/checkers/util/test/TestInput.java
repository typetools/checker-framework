package checkers.util.test;

import java.io.StringWriter;
import java.util.*;
import javax.tools.*;


import java.io.*;

public class TestInput {

    private static final boolean debug = false;

    private final JavaCompiler compiler;

    private final StandardJavaFileManager fileManager;

    private final Iterable<? extends JavaFileObject> files;
    private final Iterable<String> processors;
    private final List<String> options;

    private static final String OUTDIR = System.getProperty("tests.outputDir",
            "tests" + File.separator + "build" + File.separator + "testclasses");
    static { ensureExistance(OUTDIR); }

    public TestInput(Iterable<? extends JavaFileObject> files,
                     Iterable<String> processors, String[] options) {

        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.fileManager = compiler.getStandardFileManager(null, null, null);

        this.files = files;
        this.processors = processors;
        this.options = new LinkedList<String>();

        String classpath = System.getProperty("tests.classpath",
                "tests" + File.separator + "build");
        String globalclasspath = System.getProperty("java.class.path", "");

        this.options.add("-Xmaxerrs");
        this.options.add("9999");
        this.options.add("-g");
        this.options.add("-d");
        this.options.add(OUTDIR);
        this.options.add("-classpath");
        this.options.add("build" + File.pathSeparator + "junit.jar"
                + File.pathSeparator + classpath + File.pathSeparator
                + globalclasspath);
        this.options.addAll(Arrays.asList(options));
    }

    private static void ensureExistance(String path) {
        File file = new File(path);
        if (!file.exists())
            file.mkdirs();
    }

    public TestRun run() {
        StringWriter output = new StringWriter();
        DiagnosticCollector<JavaFileObject> diagnostics = new
            DiagnosticCollector<JavaFileObject>();

        if (debug) {
            System.out.printf("TestInput.run:%n  options: %s%n  processors: %s%n  files: %s%n",
                              this.options, this.processors, this.files);
        }

        JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager,
              diagnostics, this.options, this.processors, this.files);
        
        /* 
         * In Eclipse, std out and std err for multiple tests appear as one
         * long stream. When selecting a specific failed test, one sees the
         * expected/unexpected messages, but not the std out/err messages from
         * that particular test. Can we improve this somehow?
         */
        Boolean result = task.call();

        return new TestRun(result, output.toString(), diagnostics.getDiagnostics());
    }

    public static TestRun compileAndCheck(Iterable<? extends JavaFileObject> files,
                                          String processor, String[] options) {
        List<String> opts = new LinkedList<String>();
        if (processor != null) {
            opts.add("-processor");
            opts.add(processor);
        }
        opts.add("-source");
        opts.add("1.8");
        for (String option : options)
            opts.add(option);

        TestInput input = new TestInput(files,
                Collections.<String>emptySet(),
                opts.toArray(new String[opts.size()]));
        return input.run();
    }
}
