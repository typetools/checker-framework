package checkers.util.trees;

import java.util.StringTokenizer;

import javax.annotation.processing.ProcessingEnvironment;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class TreeParser {
    private static final String DELIMS = ".[](),";
    private static final String SENTINAL = "";

    private final TreeMaker maker;
    private final Names names;

    public TreeParser(ProcessingEnvironment env) {
        Context context = ((JavacProcessingEnvironment)env).getContext();
        maker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    public ExpressionTree parseTree(String s) {
        tokenizer = new StringTokenizer(s, DELIMS, true);
        token = tokenizer.nextToken();

        try {
            return parseExpression();
        } catch (Exception e) {
            throw new ParseError(e);
        } finally {
            tokenizer = null;
            token = null;
        }
    }

    StringTokenizer tokenizer = null;
    String token = null;

    private String nextToken() {
        token = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : SENTINAL;
        return token;
    }

    JCExpression fromToken(String token) {
        // Optimization
        if ("true".equals(token)) {
            return maker.Literal(true);
        } else if ("false".equals(token)) {
            return maker.Literal(false);
        }

        if (Character.isLetter(token.charAt(0)))
            return maker.Ident(names.fromString(token));

        Object value = null;
        try {
            value = Integer.valueOf(token);
        } catch (Exception e2) { try {
            value = Double.valueOf(token);
        } catch (Exception ef) {}}
        assert value != null;
        return maker.Literal(value);
    }

    public JCExpression parseExpression() {
        JCExpression tree = fromToken(token);

        while (tokenizer.hasMoreTokens()) {
            String delim = nextToken();
            if (".".equals(delim)) {
                nextToken();
                tree = maker.Select(tree,
                        names.fromString(token));
            } else if ("(".equals(delim)) {
                nextToken();
                // For now, handle empty args only
                assert ")".equals(token);
                tree = maker.Apply(List.<JCExpression>nil(),
                        tree, List.<JCExpression>nil());
            } else if ("[".equals(token)) {
                nextToken();
                JCExpression index = parseExpression();
                assert "]".equals(token);
                tree = maker.Indexed(tree, index);
            } else if ("]".equals(token)) {
                return tree;
            } else if (SENTINAL.equals(token)) {
                return tree;
            }
        }

        return tree;
    }

    class ParseError extends RuntimeException {
        private static final long serialVersionUID = 1887754619522101929L;

        ParseError(Throwable cause) {
            super(cause);
        }
    }
}
