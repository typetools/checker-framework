package checkers.regex;

import java.util.regex.Pattern;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;

/**
 * Adds {@link ValidRegex} to the type of {@code String} literals that are
 * syntactically String literals.
 */
public class RegexAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<RegexChecker> {

    public RegexAnnotatedTypeFactory(RegexChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        if (!type.isAnnotated()
            && tree.getKind() == Tree.Kind.STRING_LITERAL
            && isValidRegex((String)((LiteralTree)tree).getValue())) {
            type.addAnnotation(ValidRegex.class);
        }
        super.annotateImplicit(tree, type);
    }

    /**
     * Returns true iff {@code str} is a valid regular expression
     */
    private boolean isValidRegex(String str) {
        try {
            Pattern.compile(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
