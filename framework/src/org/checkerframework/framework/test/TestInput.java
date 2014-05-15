package org.checkerframework.framework.test;

import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

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

    private TestInput(String checkerDir, Iterable<? extends JavaFileObject> files,
                     Iterable<String> processors, List<String> options) {

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
        // Always output warnings for unchecked constructs
        this.options.add("-Xlint:unchecked");
        // Use short javac diagnostics
        this.options.add("-XDrawDiagnostics");
        this.options.add("-AprintErrorStack");

        // Use the annotated jdk for the compile bootclasspath
        // This is set by build.xml
        String jdkJarPath = System.getProperty("JDK_JAR");
        if (jdkJarPath != null && jdkJarPath.length() > 0) {
            this.options.add("-Xbootclasspath/p:" + jdkJarPath);
        }

        // Pass the source path to allow test files that depend
        // on each other.
        if (checkerDir != null && !checkerDir.isEmpty()) {
            this.options.add("-sourcepath");
            this.options.add(checkerDir);
        }

        this.options.add("-implicit:class");

        this.options.add("-d");
        this.options.add(OUTDIR);

        this.options.add("-classpath");
        this.options.add("build" + File.pathSeparator +
                "junit.jar" + File.pathSeparator +
                classpath + File.pathSeparator +
                globalclasspath);

        this.options.addAll(options);
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

    public static TestRun compileAndCheck(String checkerDir,
            Iterable<? extends JavaFileObject> files,
            String processor, List<String> options) {

        List<String> opts = new LinkedList<String>();
        if (processor != null) {
            opts.add("-processor");
            opts.add(processor);
        }

        opts.addAll(options);

        TestInput input = new TestInput(checkerDir, files,
                // TODO: why are the processors passed as options
                // and not through this parameter?
                Collections.<String>emptySet(),
                opts);
        return input.run();
    }
}
