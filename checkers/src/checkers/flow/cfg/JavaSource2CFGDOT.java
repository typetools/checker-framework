package checkers.flow.cfg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.lang.model.element.ExecutableElement;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.xml.ws.Holder;

import checkers.flow.cfg.block.Block;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.util.TreeUtils;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

/**
 * Class to generate the DOT representation of the control flow graph of a given
 * method.
 * 
 * @author Stefan Heule
 */
public class JavaSource2CFGDOT {

	/** Main method. */
	public static void main(String[] args) {
		if (args.length < 2) {
			printUsage();
			System.exit(1);
		}
		String input = args[0];
		String output = args[1];
		File file = new File(input);
		if (!file.canRead()) {
			printError("Cannot read input file: " + file.getAbsolutePath());
			printUsage();
			System.exit(1);
		}

		String method = "test";
		String clas = "Test";
		boolean pdf = false;
		boolean error = false;

		for (int i = 2; i < args.length; i++) {
			if (args[i].equals("-pdf")) {
				pdf = true;
			} else if (args[i].equals("-method")) {
				if (i >= args.length - 1) {
					printError("Did not find <name> after -method.");
					continue;
				}
				i++;
				method = args[i];
			} else if (args[i].equals("-class")) {
				if (i >= args.length - 1) {
					printError("Did not find <name> after -class.");
					continue;
				}
				i++;
				clas = args[i];
			} else {
				printError("Unknown command line argument: " + args[i]);
				error = true;
			}
		}

		if (error) {
			System.exit(1);
		}

		generateDOTofCFG(input, output, method, clas, pdf);
	}

	/** Print an error message. */
	protected static void printError(String string) {
		System.err.println("ERROR: " + string);
	}

	/** Print usage information */
	protected static void printUsage() {
		System.out
				.println("Generate the control flow graph of a Java method, represented as a DOT graph.");
		System.out
				.println("Parameters: <inputfile> <outputfile> [-method <name>] [-class <name>] [-pdf]");
		System.out
				.println("    -pdf:    Also generate the PDF by invoking 'dot'.");
		System.out
				.println("    -method: The method to generate the CFG for (defaults to 'test').");
		System.out
				.println("    -class:  The class in which to find the method (defaults to 'Test').");
	}

	/**
	 * Generate the DOT representation of the CFG for a method.
	 * 
	 * @param inputFile
	 *            Java source input file.
	 * @param outputFile
	 *            Source output file (without file extension)
	 * @param method
	 *            Method name to generate the CFG for.
	 * @param pdf
	 *            Also generate a PDF?
	 */
	public static void generateDOTofCFG(String inputFile, String outputFile,
			String method, String clas, boolean pdf) {
		String fileName = (new File(inputFile)).getName();
		System.out.println("Working on " + fileName + "...");
		MethodTree m = getMethodTree(inputFile, method, clas);

		if (m == null) {
			printError("Method not found.");
			System.exit(1);
		}

		Block b = CFGBuilder.build(m);
		String s = CFGDOTVisualizer.visualize(b);

		try {
			FileWriter fstream = new FileWriter(outputFile + ".txt");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(s);
			System.out.println("Finished " + fileName + ".");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (pdf) {
			producePDF(outputFile);
		}
	}

	/**
	 * Invoke DOT to generate a PDF.
	 */
	protected static void producePDF(String file) {
		try {
			String command = "dot -Tpdf \"" + file + ".txt\" -o \"" + file
					+ ".pdf\"";
			Process child = Runtime.getRuntime().exec(command);
			child.waitFor();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Get the AST of a specific method in a specific class in a specific file
	 * (or null if no such method exists.
	 */
	public static/* @Nullable */MethodTree getMethodTree(String file,
			final String method, String clas) {
		final Holder<MethodTree> m = new Holder<MethodTree>();
		SourceChecker sourceChecker = new SourceChecker() {
			@Override
			protected SourceVisitor<?, ?> createSourceVisitor(
					CompilationUnitTree root) {
				return new SourceVisitor<Void, Void>(this, root) {
					@Override
					public Void visitMethod(MethodTree node, Void p) {
						ExecutableElement el = TreeUtils
								.elementFromDeclaration(node);
						if (el.getSimpleName().contentEquals(method)) {
							m.value = node;
						}
						return null;
					}
				};
			}
		};

		Context context = new Context();
		JavaCompiler javac = new JavaCompiler(context);
		javac.attrParseOnly = true;
		JavacFileManager fileManager = (JavacFileManager) context
				.get(JavaFileManager.class);

		JavaFileObject l = fileManager
				.getJavaFileObjectsFromStrings(List.of(file)).iterator().next();

		try {
			javac.compile(List.of(l), List.of(clas), List.of(sourceChecker));
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}

		return m.value;
	}

}
