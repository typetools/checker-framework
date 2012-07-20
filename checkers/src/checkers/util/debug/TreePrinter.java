package checkers.util.debug;

import com.sun.source.util.AbstractTypeProcessor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.Pretty;

import java.io.IOException;
import java.io.StringWriter;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A utility class for pretty-printing the AST of a program.
 *
 * <p>
 * The class is actually an annotation processor; in order to use it, invoke the
 * compiler on the source file(s) for which you wish to view the AST of the program.
 * You may also wish to use the {@code -proc:only} javac option to
 * stop compilation after annotation processing.  (But, in general
 * {@code -proc:only} causes type annotation processors not to be run.)
 *
 * <p>
 * A simple main method is also provided. Put jsr308-all.jar on the bootclasspath
 * and you can invoke this tool as:
 *
 *   java checkers.util.debug.TreePrinter *.java
 *
 * TODO: is there an environment variable for the bootclasspath?
 *
 * <p>
 * The visitor simply uses the javac Pretty visitor to output a nicely formatted
 * version of the AST.
 *
 * TODO: I couldn't find a way to display the result of Pretty, therefore I wrote
 * this simple class. If there already was a way, please let me know.
 *
 * TODO: what I really want is something like SignaturePrinter, but for the whole
 * source of the program, that is, for each type in the program use the factory
 * to determine the defaulted annotations on the type.
 *
 * @see checkers.util.debug.TreeDebug
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TreePrinter extends AbstractTypeProcessor {
    @Override
    public void typeProcess(TypeElement element, TreePath tree) {
        final StringWriter out = new StringWriter();
        final Pretty pretty = new Pretty(out, true);

        try {
            pretty.printUnit((JCCompilationUnit) tree.getCompilationUnit(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(out.toString());
    }

    public static void main(String[] args) throws Exception {
        String[] newArgs = new String[args.length + 3];
        newArgs[0] = "-processor";
        newArgs[1] = "checkers.util.debug.TreePrinter";
        newArgs[2] = "-proc:only";
        System.arraycopy(args, 0, newArgs, 3, args.length);
        com.sun.tools.javac.Main.compile(newArgs);
    }
}
