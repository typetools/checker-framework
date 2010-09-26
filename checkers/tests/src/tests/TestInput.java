package tests;

import java.io.StringWriter;
import java.util.*;
import javax.tools.*;
import java.io.*;

public class TestInput {

    public boolean debug = false;

    private JavaCompiler compiler;

    private StandardJavaFileManager fileManager;

    private Iterable<? extends JavaFileObject> files;
    private Iterable<String> processors;
    private List<String> options;

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

        // Method
        //   com.sun.tools.javac.main.Main.apMessage(AnnotationProcessingError ex)
        // may print the stack trace of an exception to standard output,
        // even though the exception is later handled.  (Why is that
        // printed to standard out whereas diagnostic messages are not?
        // And why does it happen here but not when javac is run from the
        // command line?)  We need to figure out how to redirect that stack
        // trace elsewhere, to avoid looking like it was thrown during
        // execution of the test.  (There's a javac option -Xstdout, but it
        // isn't recognized by JavacTool.)
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
        opts.add("1.7");
        for (String option : options)
            opts.add(option);

        TestInput input = new TestInput(files,
                Collections.<String>emptySet(),
                opts.toArray(new String[opts.size()]));
        return input.run();
    }
}
