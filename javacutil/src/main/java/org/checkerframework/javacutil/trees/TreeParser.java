package org.checkerframework.javacutil.trees;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import org.checkerframework.javacutil.Pair;

import java.util.StringTokenizer;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * A utility class for parsing Java expression snippets, and converting them to proper Javac AST
 * nodes.
 *
 * <p>This is useful for parsing {@code EnsuresNonNull*}, and {@code KeyFor} values.
 *
 * <p>Currently, it handles four tree types only:
 *
 * <ul>
 *   <li>Identifier tree (e.g. {@code id})
 *   <li>Literal tree (e.g. 2, 3)
 *   <li>Method invocation tree (e.g. {@code method(2, 3)})
 *   <li>Member select tree (e.g. {@code Class.field}, {@code instance.method()})
 *   <li>Array access tree (e.g. {@code array[id]})
 * </ul>
 *
 * Notable limitation: Doesn't handle spaces, or non-method-argument parenthesis.
 *
 * <p>It's implemented via a Recursive-Descend parser.
 */
public class TreeParser {
    /** Valid delimiters. */
    private static final String DELIMS = ".[](),";
    /** A sentinel value. */
    private static final String SENTINEL = "";

    /** The TreeMaker instance. */
    private final TreeMaker maker;
    /** The names instance. */
    private final Names names;

    /** Create a TreeParser. */
    public TreeParser(ProcessingEnvironment env) {
        Context context = ((JavacProcessingEnvironment) env).getContext();
        maker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    /**
     * Parses the snippet in the string as an internal Javac AST expression node.
     *
     * @param s the java snippet
     * @return the AST corresponding to the snippet
     */
    public ExpressionTree parseTree(String s) {
        StringTokenizer tokenizer = new StringTokenizer(s, DELIMS, true);
        String token = tokenizer.nextToken();

        try {
            return parseExpression(tokenizer, token).first;
        } catch (Exception e) {
            throw new ParseError(e);
        } finally {
            tokenizer = null;
            token = null;
        }
    }

    /** The next token from the tokenizer, or the {@code SENTINEL} if none is available. */
    private String nextToken(StringTokenizer tokenizer) {
        return tokenizer.hasMoreTokens() ? tokenizer.nextToken() : SENTINEL;
    }

    /** The parsed expression tree for the given token. */
    private JCExpression fromToken(String token) {
        // Optimization
        if ("true".equals(token)) {
            return maker.Literal(true);
        } else if ("false".equals(token)) {
            return maker.Literal(false);
        }

        if (Character.isLetter(token.charAt(0))) {
            return maker.Ident(names.fromString(token));
        }

        Object value;
        try {
            value = Integer.valueOf(token);
        } catch (Exception e2) {
            try {
                value = Double.valueOf(token);
            } catch (Exception ef) {
                throw new Error("Can't parse as integer or double: " + token);
            }
        }
        return maker.Literal(value);
    }

    /**
     * Parse an expression.
     *
     * @param tokenizer the tokenizer
     * @param token the first token
     * @return a pair of a parsed expression and the next token
     */
    private Pair<JCExpression, String> parseExpression(StringTokenizer tokenizer, String token) {
        JCExpression tree = fromToken(token);

        while (tokenizer.hasMoreTokens()) {
            String delim = nextToken(tokenizer);
            token = delim;
            if (".".equals(delim)) {
                token = nextToken(tokenizer);
                tree = maker.Select(tree, names.fromString(token));
            } else if ("(".equals(delim)) {
                token = nextToken(tokenizer);
                ListBuffer<JCExpression> args = new ListBuffer<>();
                while (!")".equals(token)) {
                    Pair<JCExpression, String> p = parseExpression(tokenizer, token);
                    JCExpression arg = p.first;
                    token = p.second;
                    args.append(arg);
                    if (",".equals(token)) {
                        token = nextToken(tokenizer);
                    }
                }
                // For now, handle empty args only
                assert ")".equals(token) : "Unexpected token: " + token;
                tree = maker.Apply(List.nil(), tree, args.toList());
            } else if ("[".equals(token)) {
                token = nextToken(tokenizer);
                Pair<JCExpression, String> p = parseExpression(tokenizer, token);
                JCExpression index = p.first;
                token = p.second;
                assert "]".equals(token) : "Unexpected token: " + token;
                tree = maker.Indexed(tree, index);
            } else {
                return Pair.of(tree, token);
            }
            assert tokenizer != null : "@AssumeAssertion(nullness): side effects";
        }

        return Pair.of(tree, token);
    }

    /** An internal error. */
    private static class ParseError extends RuntimeException {
        /** The serial version UID. */
        private static final long serialVersionUID = 1887754619522101929L;

        /** Create a ParseError. */
        ParseError(Throwable cause) {
            super(cause);
        }
    }
}
