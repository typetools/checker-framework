package org.checkersplugin;

import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.codehaus.plexus.compiler.CompilerError;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * A hack to access protected static methods in {@link org.codehaus.plexus.compiler.javac.JavacCompiler}.
 * @author Adam Warski (adam at warski dot org)
 */
public class JavacCompilerUtil extends JavacCompiler {
	@SuppressWarnings({"unchecked"})
	public static List<CompilerError> parseModernStream(BufferedReader input)
        throws IOException {
		return JavacCompiler.parseModernStream(input);
	}
}
