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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

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

    /** Reference types that may be annotated with @Regex. */
    protected TypeMirror[] legalReferenceTypes;

    /**
     * Create a RegexVisitor.
     *
     * @param checker the associated RegexChecker.
     */
    public RegexVisitor(BaseTypeChecker checker) {
        super(checker);
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.matchResultEnd =
                TreeUtils.getMethod(
                        java.util.regex.MatchResult.class.getCanonicalName(), "end", 1, env);
        this.matchResultGroup =
                TreeUtils.getMethod(
                        java.util.regex.MatchResult.class.getCanonicalName(), "group", 1, env);
        this.matchResultStart =
                TreeUtils.getMethod(
                        java.util.regex.MatchResult.class.getCanonicalName(), "start", 1, env);
        this.patternCompile =
                TreeUtils.getMethod(
                        java.util.regex.Pattern.class.getCanonicalName(), "compile", 2, env);
        this.patternLiteral =
                TreeUtils.getField(
                        java.util.regex.Pattern.class.getCanonicalName(), "LITERAL", env);
        Elements elements = atypeFactory.getElementUtils();
        this.legalReferenceTypes =
                new TypeMirror[] {
                    elements.getTypeElement("java.lang.CharSequence").asType(),
                    elements.getTypeElement("java.lang.Character").asType(),
                    elements.getTypeElement("java.util.regex.Pattern").asType(),
                    elements.getTypeElement("java.util.regex.MatchResult").asType()
                };
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
                if (receiver == null) {
                    // When checking implementations of java.util.regex.MatcherResult, calls to
                    // group (and other methods) don't have a receiver tree.  So, just do the
                    // regular checking. Verifying an implemenation of a subclass of MatcherResult
                    // is out of the scope of this checker.
                    return super.visitMethodInvocation(node, p);
                }
                int annoGroups = 0;
                AnnotatedTypeMirror receiverType = atypeFactory.getAnnotatedType(receiver);

                if (receiverType != null && receiverType.hasAnnotation(Regex.class)) {
                    annoGroups =
                            atypeFactory.getGroupCount(receiverType.getAnnotation(Regex.class));
                }
                if (paramGroups > annoGroups) {
                    checker.reportError(
                            group, "group.count.invalid", paramGroups, annoGroups, receiver);
                }
            } else {
                checker.reportWarning(group, "group.count.unknown");
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
    protected TypeValidator createTypeValidator() {
        return new RegexValidator(checker, this, atypeFactory);
    }

    /** Forbid @Regex on types that are not character sequences. */
    private class RegexValidator extends BaseTypeValidator {

        /**
         * Create a RegexValidator.
         *
         * @param checker checker
         * @param visitor visitor
         * @param atypeFactory factory
         */
        public RegexValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        /**
         * Returns true if @Regex may be written on the given type.
         *
         * @param tm a type
         * @return true if @Regex may be written on the given type
         */
        private boolean regexIsApplicable(TypeMirror tm) {
            if (TypesUtils.isDeclaredOfName(tm, "java.lang.Object")) {
                return true;
            }
            Types typeUtils = checker.getProcessingEnvironment().getTypeUtils();
            for (TypeMirror legalType : legalReferenceTypes) {
                if (typeUtils.isSubtype(tm, legalType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
            if (!(type.hasAnnotation(((RegexAnnotatedTypeFactory) atypeFactory).UNKNOWNREGEX)
                    || type.hasAnnotation(
                            ((RegexAnnotatedTypeFactory) atypeFactory).REGEXBOTTOM))) {
                TypeMirror underlying = type.getUnderlyingType();
                if (!regexIsApplicable(underlying)) {
                    TypeMirror unannotated =
                            TypeAnnotationUtils.unannotatedType(
                                    type.getErased().getUnderlyingType());
                    checker.reportError(tree, "type.invalid", type.getAnnotations(), unannotated);
                }
            }

            return super.visitDeclared(type, tree);
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
            if (type.getKind() != TypeKind.CHAR) {
                if (!type.hasAnnotation(((RegexAnnotatedTypeFactory) atypeFactory).UNKNOWNREGEX)) {
                    TypeMirror unannotated =
                            TypeAnnotationUtils.unannotatedType(type.getUnderlyingType());
                    checker.reportError(tree, "type.invalid", type.getAnnotations(), unannotated);
                }
            }
            return super.visitPrimitive(type, tree);
        }
    }
}
