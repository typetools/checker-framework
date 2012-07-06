package daikon.tools.runtimechecker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jtb.JavaParser;
import jtb.syntaxtree.*;
import jtb.visitor.TreeDumper;
import jtb.visitor.TreeFormatter;
import utilMDE.Assert;
import utilMDE.UtilMDE;
import daikon.Daikon;
import daikon.FileIO;
import daikon.Global;
import daikon.LogHelper;
import daikon.PptMap;
import daikon.tools.jtb.*;
import daikon.tools.runtimechecker.InstrumentVisitor;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 * Instruments a file to check invariant violations at
 * runtime. Violated invariants are stored in a list in
 * daikon.tools.runtimechecker.Runtime. The control flow of the class
 * remains unchanged from the original.
 *
 * @author Carlos Pacheco
 */
public class InstrumentHandler extends CommandHandler {

    public boolean handles(String command) {
        if (command.equals("instrument")) {
            return true;
        }
        return false;
    }

    // If true, the instrumenter also outputs checker classes (classes that define
    // invariant-checking methods outside the instrumented class).
    private static boolean createCheckerClasses = false;

    private static final Logger debug = Logger.getLogger("daikon.tools.runtimechecker.InstrumentHandler");

    // If the --max_invariants_pp option is given, this variable is set
    // to the maximum number of invariants out annotate per program point.
    protected static int maxInvariantsPP = -1;

    private static final String make_all_fields_public_SWITCH = "make_all_fields_public";
    private static final String output_only_high_conf_invariants_SWITCH = "output_only_high_conf_invariants";
    private static final String create_checker_classes_SWITCH = "create_checker_classes";
    private static final String directory_SWITCH = "directory";
    private static final String checkers_directory_SWITCH = "checker_classes_directory";

    private String instrumented_directory = "instrumented-classes";
    private String checkersOutputDirName = "checker-classes";

    // Whether should print debugging information as it is executing.
    private static String debug_SWITCH = "debug";

    public boolean handle(String[] args) {

        Daikon.output_format = daikon.inv.OutputFormat.JAVA;
        daikon.PrintInvariants.wrap_xml = true;

        if (!args[0].equals("instrument")) {
            System.err.println("Command (first argument) to instrumenter was not recognized.");
            return false;
        }

        String[] realArgs = new String[args.length - 1];
        for (int i = 0; i < realArgs.length; i++) {
            realArgs[i] = args[i + 1];
        }
        Arguments arguments = readArguments(realArgs);
        if (arguments == errorWhileReadingArguments) {
            return false;
        }

        // Create instrumented-classes dir.
        File outputDir = new File(instrumented_directory);

        System.out.println("Reading invariant file: " + arguments.invFile);
        PptMap ppts = null;
        try {
            ppts = FileIO.read_serialized_pptmap(new File(arguments.invFile),
                    true /* use saved config */);
        } catch (IOException e) {
            System.err.println("Exception while reading invariant file " + arguments.invFile);
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        // Compile original sources (because daikon.tools.jtb.Ast accesses
        // them via reflection).
        //compile(arguments.javaFileNames, "");

        // Create filenames including temp directory and pakage directories.
        List<ParseResults> parseResults = ParseResults.parse(arguments.javaFileNames,
                                                             true /* discard comments */);

        for (ParseResults oneFile : parseResults) {

            System.out.println("Instrumenting " + oneFile.fileName);

            List<CheckerClass> checkerClasses = new ArrayList<CheckerClass>();

            for (TypeDeclaration decl : oneFile.roots) {

                InstrumentVisitor v = new InstrumentVisitor(ppts, decl);

                decl.accept(v);

                if (createCheckerClasses) {
                    v.add_checkers_for_nondeclared_members();
                    checkerClasses.addAll(v.checkerClasses.getCheckerClasses());
                }
            }

            try {

                File instrumentedFileDir = new File(outputDir.getPath()
                                                    + File.separator
                                                    + oneFile.packageName.replace(".", File.separator));

                if (!instrumentedFileDir.exists()) {
                    instrumentedFileDir.mkdirs();
                }

                File checkerClassesDir = new File(checkersOutputDirName
                                                  + File.separator
                                                  + oneFile.packageName.replace(".", File.separator));


                if (!checkerClassesDir.exists()) {
                    checkerClassesDir.mkdirs();
                }

                // Output instrumented class
                String instrumentedFileName = oneFile.fileName;
                File instrumentedFile = new File(instrumentedFileDir, instrumentedFileName);
                debug.fine("instrumented file name: " + instrumentedFile.getPath());
                System.out.println("Writing " + instrumentedFile);
                Writer output = new FileWriter(instrumentedFile);
                oneFile.compilationUnit.accept(new TreeFormatter());
                TreeDumper dumper = new TreeDumper(output);
                dumper.printSpecials(false);
                oneFile.compilationUnit.accept(dumper);

                output.close();

                // Output checker classes
                for (CheckerClass cls : checkerClasses) {
                    String checkerClassFileName = cls.getCheckerClassName() + ".java";
                    File checkerClassFile = new File(checkerClassesDir, checkerClassFileName);
                    System.out.println("Writing " + checkerClassFile);
                    output = new FileWriter(checkerClassFile);
                    CompilationUnit cu = cls.getCompilationUnit();
                    cu.accept(new TreeFormatter());
                    cu.accept(new TreeDumper(output));
                }

            } catch (IOException e) {
                System.err.println("Exception while instrumenting " + oneFile.fileName);
                System.err.println(e.getMessage());
                e.printStackTrace();
                return false;
            }

        }

        return true;
    }

    private static class Arguments {
        public String invFile;

        public List<String> javaFileNames;
    }

    private static Arguments errorWhileReadingArguments = new Arguments();

    private Arguments readArguments(String[] args) {

        LongOpt[] longopts = new LongOpt[] {
                new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null,
                        0),
                new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT,
                        null, 0),
                new LongOpt(output_only_high_conf_invariants_SWITCH,
                        LongOpt.NO_ARGUMENT, null, 0),
                new LongOpt(make_all_fields_public_SWITCH,
                        LongOpt.NO_ARGUMENT, null, 0),
                new LongOpt(create_checker_classes_SWITCH,
                        LongOpt.NO_ARGUMENT, null, 0),
                new LongOpt(directory_SWITCH,
                        LongOpt.REQUIRED_ARGUMENT, null, 0),
                new LongOpt(checkers_directory_SWITCH,
                        LongOpt.REQUIRED_ARGUMENT, null, 0),
                new LongOpt(debug_SWITCH, LongOpt.NO_ARGUMENT, null, 0) };
        Getopt g = new Getopt("daikon.tools.runtimechecker.InstrumentHandler", args, "hs", longopts);
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
            case 0:
                // got a long option
                String option_name = longopts[g.getLongind()].getName();

                if (debug_SWITCH.equals(option_name)) {
                    debug.setLevel(Level.FINE);
                } else if (create_checker_classes_SWITCH.equals(option_name)) {
                    createCheckerClasses = true;
                } else if (output_only_high_conf_invariants_SWITCH.equals(option_name)) {
                    InstrumentVisitor.outputOnlyHighConfInvariants = true;
                } else if (make_all_fields_public_SWITCH.equals(option_name)) {
                    InstrumentVisitor.makeAllFieldsPublic = true;
                } else if (directory_SWITCH.equals(option_name)) {
                    instrumented_directory = g.getOptarg();
                } else if (checkers_directory_SWITCH.equals(option_name)) {
                    checkersOutputDirName = g.getOptarg();
                } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
                    Global.debugAll = true;
                } else if (Daikon.debug_SWITCH.equals(option_name)) {
                    LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
                } else {
                    System.err.println("Unknown long option received: "
                                       + option_name);
                }
                break;
            default:
                System.out.println("unrecognized option" + c);
                return errorWhileReadingArguments;
            }
        }
        // The index of the first non-option argument -- the name of the
        // invariant file.
        int argindex = g.getOptind();
        if (argindex >= args.length) {
            System.out
                    .println("Error: No .inv file or .java file arguments supplied.");
            return errorWhileReadingArguments;
        }
        String invfile = args[argindex];
        argindex++;
        if (!(invfile.endsWith(".inv") || invfile.endsWith(".inv.gz"))) {
            System.out.println("Error: first argument must be a"
                    + "file ending in .inv or .inv.gz.");
            return errorWhileReadingArguments;
        }
        if (argindex >= args.length) {
            System.out.println("Error: No .java file arguments supplied.");
            return errorWhileReadingArguments;
        }
        List<String> javaFileNames = new ArrayList<String>();
        for (; argindex < args.length; argindex++) {
            String javafile = args[argindex];
            if (!javafile.endsWith(".java")) {
                System.out.println("File does not end in .java: " + javafile);
                return errorWhileReadingArguments;
            }
            javaFileNames.add(javafile);
        }

        Arguments ret = new Arguments();
        ret.invFile = invfile;
        ret.javaFileNames = javaFileNames;
        return ret;
    }
}
