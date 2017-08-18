package org.checkerframework.checker.regex;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A type-checking visitor for the Regex type system.
 *
 * <p>This visitor does the following:
 *
 * <ol>
 *   <li value="1">Allows any String to be passed to Pattern.compile if the Pattern.LITERAL flag is
 *       passed.
 *   <li value="2">Checks compound String concatenation to ensure correct usage of Regex Strings.
 *   <li value="3">Checks calls to {@code MatchResult.start}, {@code MatchResult.end} and {@code
 *       MatchResult.group} to ensure that a valid group number is passed.
 * </ol>
 *
 * @see RegexChecker
 */
public class RegexVisitor extends BaseTypeVisitor<RegexAnnotatedTypeFactory> {

    private final ExecutableElement matchResultEnd;
    private final ExecutableElement matchResultGroup;
    private final ExecutableElement matchResultStart;
    private final ExecutableElement patternCompile;
    private final VariableElement patternLiteral;

    public RegexVisitor(BaseTypeChecker checker) {
        super(checker);
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.matchResultEnd =
                TreeUtils.getMethod(java.util.regex.MatchResult.class.getName(), "end", 1, env);
        this.matchResultGroup =
                TreeUtils.getMethod(java.util.regex.MatchResult.class.getName(), "group", 1, env);
        this.matchResultStart =
                TreeUtils.getMethod(java.util.regex.MatchResult.class.getName(), "start", 1, env);
        this.patternCompile =
                TreeUtils.getMethod(java.util.regex.Pattern.class.getName(), "compile", 2, env);
        this.patternLiteral =
                TreeUtils.getField(java.util.regex.Pattern.class.getName(), "LITERAL", env);
    }

    /**
     * Case 1: Don't require a Regex annotation on the String argument to Pattern.compile if the
     * Pattern.LITERAL flag is passed.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        if (TreeUtils.isMethodInvocation(node, patternCompile, env)) {
            ExpressionTree flagParam = node.getArguments().get(1);
            if (flagParam.getKind() == Kind.MEMBER_SELECT) {
                MemberSelectTree memSelect = (MemberSelectTree) flagParam;
                if (TreeUtils.isSpecificFieldAccess(memSelect, patternLiteral)) {
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
        } else if (TreeUtils.isMethodInvocation(node, matchResultEnd, env)
                || TreeUtils.isMethodInvocation(node, matchResultGroup, env)
                || TreeUtils.isMethodInvocation(node, matchResultStart, env)) {
            /**
             * Case 3: Checks calls to {@code MatchResult.start}, {@code MatchResult.end} and {@code
             * MatchResult.group} to ensure that a valid group number is passed.
             */
            ExpressionTree group = node.getArguments().get(0);
            if (group.getKind() == Kind.INT_LITERAL) {
                LiteralTree literal = (LiteralTree) group;
                int paramGroups = (Integer) literal.getValue();
                ExpressionTree receiver = TreeUtils.getReceiverTree(node);
                int annoGroups = 0;
                AnnotatedTypeMirror receiverType = atypeFactory.getAnnotatedType(receiver);
                if (receiverType.hasAnnotation(Regex.class)) {
                    annoGroups =
                            atypeFactory.getGroupCount(receiverType.getAnnotation(Regex.class));
                }
                if (paramGroups > annoGroups) {
                    checker.report(
                            Result.failure(
                                    "group.count.invalid", paramGroups, annoGroups, receiver),
                            group);
                }
            } else {
                checker.report(Result.warning("group.count.unknown"), group);
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    /** Case 2: Check String compound concatenation for valid Regex use. */
    // TODO: Remove this. This should be handled by flow.
    /*
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        // Default behavior from superclass
    }
    */

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        // TODO: only allow Regex and PolyRegex annotations on types in legalReferenceTypes.
        // This is pending an implementation of AnnotatedTypeMirror.getExplicitAnnotations
        // that supports local variables, array types and parameterized types.
        /*// Only allow annotations on subtypes of the types in legalReferenceTypes.
        if (!useType.getExplicitAnnotations().isEmpty()) {
            Types typeUtils = env.getTypeUtils();
            for (TypeMirror type : legalReferenceTypes) {
                if (typeUtils.isSubtype(declarationType.getUnderlyingType(), type)) {
                    return true;
                }
            }
            return false;
        }*/
        return super.isValidUse(declarationType, useType, tree);
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        // TODO: only allow Regex and PolyRegex annotations on chars.
        // This is pending an implementation of AnnotatedTypeMirror.getExplicitAnnotations
        // that supports local variables and array types.
        /*// Only allow annotations on char.
        if (!type.getExplicitAnnotations().isEmpty()) {
            return type.getKind() == TypeKind.CHAR;
        }*/
        return super.isValidUse(type, tree);
    }
}
