package checkers.regex;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeVisitor;
import checkers.util.TreeUtils;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/**
 * A type-checking visitor for the Regex type system.
 * 
 * This visitor does the following:
 * 
 * <ol>
 * <li value="1">Allows any String to be passed to Pattern.compile if the
 *    Pattern.LITERAL flag is passed.</li>
 * </ol>
 * 
 * @see RegexChecker
 */
public class RegexVisitor extends BaseTypeVisitor<RegexChecker> {

    private final ExecutableElement patternCompile;
    private final VariableElement patternLiteral;

    public RegexVisitor(RegexChecker checker, CompilationUnitTree root) {
        super(checker, root);

        this.patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 2, checker.getProcessingEnvironment());
        this.patternLiteral = TreeUtils.getField("java.util.regex.Pattern", "LITERAL", checker.getProcessingEnvironment());
    }

    /**
     * Case 1: Don't require a Regex annotation on the String argument to
     * Pattern.compile if the Pattern.LITERAL flag is passed.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (TreeUtils.isMethodInvocation(node, patternCompile, checker.getProcessingEnvironment())) {
            ExpressionTree flagParam = node.getArguments().get(1);
            if (flagParam.getKind() == Kind.MEMBER_SELECT) {
                MemberSelectTree memSelect = (MemberSelectTree) flagParam;
                if (TreeUtils.isFieldAccess(memSelect, patternLiteral, checker.getProcessingEnvironment())) {
                    // This is a call to Pattern.compile with the Pattern.LITERAL
                    // flag so the first parameter doesn't need to be a
                    // @Regex String. Don't call the super method to skip checking
                    // if the first parameter is a @Regex String, but make sure to
                    // still recurse on all of the different parts of the method call.
                    Void r = scan(node.getTypeArguments(), p);
                    r = reduce(scan(node.getMethodSelect(), p), r);
                    r = reduce(scan(node.getArguments(), p), r);
                    return r;
                }
            }
        }
        return super.visitMethodInvocation(node, p);
    }
}
