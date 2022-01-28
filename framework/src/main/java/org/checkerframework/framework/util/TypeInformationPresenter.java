package org.checkerframework.framework.util;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

import javax.tools.Diagnostic;

/**
 * Presents formatted type information for various AST trees in a class.
 *
 * <p>The formatted type information is designed to be visualized by editors and IDEs that support
 * Language Server Protocol (LSP).
 */
public class TypeInformationPresenter {

    /** The AnnotatedTypeFactory for the current analysis. */
    private final AnnotatedTypeFactory factory;

    /**
     * The GenericAnnotatedTypeFactory for the current analysis. null if the factory is not an
     * instance of GenericAnnotatedTypeFactory; otherwise, factory and genFactory refer to the same
     * object.
     */
    private final GenericAnnotatedTypeFactory<
                    ? extends CFAbstractValue<?>,
                    ? extends CFAbstractStore<? extends CFAbstractValue<?>, ?>,
                    ? extends CFAbstractTransfer<?, ?, ?>,
                    ? extends CFAbstractAnalysis<?, ?, ?>>
            genFactory;

    /** This formats the ATMs that the presenter is going to present. */
    private final AnnotatedTypeFormatter typeFormatter;

    /**
     * Constructs a presenter for the given factory.
     *
     * @param factory The AnnotatedTypeFactory for the current analysis.
     */
    public TypeInformationPresenter(AnnotatedTypeFactory factory) {
        this.factory = factory;
        if (factory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
            this.genFactory = (GenericAnnotatedTypeFactory<?, ?, ?, ?>) factory;
        } else {
            this.genFactory = null;
        }
        this.typeFormatter = new DefaultAnnotatedTypeFormatter(true, true);
    }

    /**
     * The entry point for presenting type information of trees in the given class.
     *
     * @param tree A ClassTree that has been type-checked by the factory.
     */
    public void process(ClassTree tree) {
        TypeInformationReporter visitor = new TypeInformationReporter(tree);
        visitor.scan(tree, null);
    }

    /**
     * Stores an inclusive range [(startLine, startCol), (endLine, endCol)] in the source code to
     * which a piece of type information refers. All indices are 0-based since LSP uses 0-based
     * positions.
     */
    private static class MessageRange {
        /** 0-based line number of the start position. */
        private final long startLine;

        /** 0-based column number of the start position. */
        private final long startCol;

        /** 0-based line number of the end position. */
        private final long endLine;

        /** 0-based column number of the end position. */
        private final long endCol;

        /**
         * Constructs a new MessageRange with the given position information.
         *
         * @param startLine 0-based line number of the start position
         * @param startCol 0-based column number of the start position
         * @param endLine 0-based line number of the end position
         * @param endCol 0-based column number of the end position
         */
        private MessageRange(long startLine, long startCol, long endLine, long endCol) {
            this.startLine = startLine;
            this.startCol = startCol;
            this.endLine = endLine;
            this.endCol = endCol;
        }

        /**
         * Constructs a new MessageRange with the given position information.
         *
         * @param startLine 0-based line number of the start position
         * @param startCol 0-based column number of the start position
         * @param endLine 0-based line number of the end position
         * @param endCol 0-based column number of the end position
         * @return a new MessageRange with the given position information
         */
        private static MessageRange of(long startLine, long startCol, long endLine, long endCol) {
            return new MessageRange(startLine, startCol, endLine, endCol);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d, %d, %d)", startLine, startCol, endLine, endCol);
        }
    }

    /**
     * It is possible to report multiple type messages for the same message range. This enum
     * provides some explanation for each kind of type message.
     */
    private enum MessageKind {
        /** The type of the tree at its use site. */
        USE_TYPE,
        /**
         * The declared type of the tree. For a method, it should be the method's signature. For a
         * field, it should be the type of the field in its declaration.
         */
        DECLARED_TYPE,
        /** The declared type of the LHS of an assignment or compound assignment tree. */
        ASSIGN_LHS_DECLARED_TYPE,
        /**
         * The type of the RHS of an assignment or compound assignment tree.
         *
         * <p>For a postfix operation, it can be considered as a special assignment tree, in which
         * the LHS is returned and the RHS is the new value of the variable. In this situation, this
         * message kind means the type of the new value of the variable.
         */
        ASSIGN_RHS_TYPE,
    }

    /**
     * A visitor which traverses a class tree and reports type information of various sub-trees.
     *
     * <p>Note: Since nested class trees will be type-checked separately, this visitor does not dive
     * into any nested class trees.
     */
    private class TypeInformationReporter extends TreeScanner<Void, Void> {

        /** The class tree in which it traverses and reports type information. */
        private final ClassTree classTree;

        /**
         * Root of the current class tree. This is a helper for computing positions of a sub-tree.
         */
        private final CompilationUnitTree currentRoot;

        /** This is a helper for computing positions of a sub-tree. */
        private final SourcePositions sourcePositions;

        /** The checker that's currently running. */
        private final BaseTypeChecker checker;

        /**
         * Constructs a new reporter for the given class tree.
         *
         * @param classTree a ClassTree
         */
        public TypeInformationReporter(ClassTree classTree) {
            this.classTree = classTree;
            this.checker = factory.getChecker();
            this.currentRoot = this.checker.getPathToCompilationUnit().getCompilationUnit();
            this.sourcePositions = factory.getTreeUtils().getSourcePositions();
        }

        /**
         * Reports a diagnostic message indicating the range corresponds to the given tree has the
         * given type. Specifically, the message has key "lsp.type.information", and it contains the
         * name of the checker, the given messageKind, the given type, and the computed message
         * range for the tree. If the tree is an artificial tree, don't report anything.
         *
         * @param tree The tree that is used to find the corresponding range to report.
         * @param type The type that we are going to display.
         * @param messageKind The kind of the given type.
         */
        private void reportTreeType(Tree tree, AnnotatedTypeMirror type, MessageKind messageKind) {
            MessageRange messageRange = computeMessageRange(tree);
            if (messageRange == null) {
                // Don't report if the tree can't be found in the source file.
                // Please check the implementation of computeMessageRange for
                // more details.
                return;
            }

            checker.reportError(
                    tree,
                    "lsp.type.information",
                    checker.getClass().getSimpleName(),
                    messageKind,
                    typeFormatter.format(type),
                    messageRange);
        }

        /**
         * A wrapper of the method reportTreeType(Tree, AnnotatedTypeMirror, MessageKind) with
         * {@link MessageKind#USE_TYPE} as the default message kind.
         *
         * @param tree The tree that is used to find the corresponding range to report.
         * @param type The type that we are going to display.
         */
        private void reportTreeType(Tree tree, AnnotatedTypeMirror type) {
            reportTreeType(tree, type, MessageKind.USE_TYPE);
        }

        /**
         * Computes the 0-based inclusive message range for the given tree.
         *
         * <p>Note that the range sometimes don't cover the entire source code of the tree. For
         * example, in "int a = 0", we have a variable tree "int a", but we only want to report the
         * range of the identifier "a". This customizes the positions where we want the type
         * information to show.
         *
         * @param tree The tree for which we want to compute the message range.
         * @return A message range corresponds to the tree.
         */
        private MessageRange computeMessageRange(Tree tree) {
            long startPos = sourcePositions.getStartPosition(currentRoot, tree);
            long endPos = sourcePositions.getEndPosition(currentRoot, tree);
            if (startPos == Diagnostic.NOPOS || endPos == Diagnostic.NOPOS) {
                // The tree doesn't exist in the source file.
                // For example, a class tree may contain a child that represents
                // a default constructor which is not explicitly written out in
                // the source file.
                // For this kind of trees, there's no way to compute their range
                // in the source file.
                return null;
            }

            LineMap lineMap = currentRoot.getLineMap();
            startPos = ((JCTree) tree).getPreferredPosition();
            long startLine = lineMap.getLineNumber(startPos);
            long startCol = lineMap.getColumnNumber(startPos);
            long endLine = startLine;
            long endCol;

            // We are decreasing endCol by 1 because we want it to be inclusive
            switch (tree.getKind()) {
                case UNARY_PLUS:
                case UNARY_MINUS:
                case BITWISE_COMPLEMENT:
                case LOGICAL_COMPLEMENT:
                case MULTIPLY:
                case DIVIDE:
                case REMAINDER:
                case PLUS:
                case MINUS:
                case AND:
                case XOR:
                case OR:
                case ASSIGNMENT:
                case LESS_THAN:
                case GREATER_THAN:
                    // 1-character operators
                    endCol = startCol;
                    break;
                case PREFIX_INCREMENT:
                case PREFIX_DECREMENT:
                case POSTFIX_INCREMENT:
                case POSTFIX_DECREMENT:
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case CONDITIONAL_AND:
                case CONDITIONAL_OR:
                case MULTIPLY_ASSIGNMENT:
                case DIVIDE_ASSIGNMENT:
                case REMAINDER_ASSIGNMENT:
                case PLUS_ASSIGNMENT:
                case MINUS_ASSIGNMENT:
                case AND_ASSIGNMENT:
                case XOR_ASSIGNMENT:
                case OR_ASSIGNMENT:
                case LESS_THAN_EQUAL:
                case GREATER_THAN_EQUAL:
                case EQUAL_TO:
                case NOT_EQUAL_TO:
                    // 2-character operators
                    endCol = startCol + 1;
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                case LEFT_SHIFT_ASSIGNMENT:
                case RIGHT_SHIFT_ASSIGNMENT:
                    // 3-character operators
                    endCol = startCol + 2;
                    break;
                case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                    // 4-character operators
                    endCol = startCol + 3;
                    break;
                case IDENTIFIER:
                    endCol = startCol + ((IdentifierTree) tree).getName().length() - 1;
                    break;
                case VARIABLE:
                    endCol = startCol + ((VariableTree) tree).getName().length() - 1;
                    break;
                case MEMBER_SELECT:
                    // The preferred start column of MemberSelectTree locates the "."
                    // character before the member identifier. So we increase startCol
                    // by 1 to point to the start of the member identifier.
                    startCol += 1;
                    endCol = startCol + ((MemberSelectTree) tree).getIdentifier().length() - 1;
                    break;
                case MEMBER_REFERENCE:
                    MemberReferenceTree memberReferenceTree = (MemberReferenceTree) tree;

                    final int identifierLength;
                    if (memberReferenceTree.getMode() == MemberReferenceTree.ReferenceMode.NEW) {
                        identifierLength = 3;
                    } else {
                        identifierLength = memberReferenceTree.getName().length();
                    }

                    // The preferred position of a MemberReferenceTree is the head of
                    // its expression, which is not ideal. Here we compute the range of
                    // its identifier using the end position and the length of the identifier.
                    endLine = lineMap.getLineNumber(endPos);
                    endCol = lineMap.getColumnNumber(endPos) - 1;
                    startLine = endLine;
                    startCol = endCol - identifierLength + 1;
                    break;
                case TYPE_PARAMETER:
                    endCol = startCol + ((TypeParameterTree) tree).getName().length() - 1;
                    break;
                case METHOD:
                    endCol = startCol + ((MethodTree) tree).getName().length() - 1;
                    break;
                case METHOD_INVOCATION:
                    return computeMessageRange(((MethodInvocationTree) tree).getMethodSelect());
                default:
                    endLine = lineMap.getLineNumber(endPos);
                    endCol = lineMap.getColumnNumber(endPos) - 1;
                    break;
            }

            // convert 1-based positions to 0-based positions
            return MessageRange.of(startLine - 1, startCol - 1, endLine - 1, endCol - 1);
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            @SuppressWarnings("interning:not.interned")
            boolean isNestedClass = tree != classTree;
            if (isNestedClass) {
                // Since nested class trees will be type-checked separately, this visitor does
                // not dive into any nested class trees.
                return null;
            }
            return super.visitClass(tree, unused);
        }

        @Override
        public Void visitTypeParameter(TypeParameterTree tree, Void unused) {
            reportTreeType(
                    tree, factory.getAnnotatedTypeFromTypeTree(tree), MessageKind.DECLARED_TYPE);
            return super.visitTypeParameter(tree, unused);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            // TODO: "int x = 1" is a VariableTree, but there is no AssignmentTree and it
            // TODO: is difficult to locate the "=" symbol.
            AnnotatedTypeMirror varType =
                    genFactory != null
                            ? genFactory.getAnnotatedTypeLhs(tree)
                            : factory.getAnnotatedType(tree);
            reportTreeType(tree, varType, MessageKind.DECLARED_TYPE);
            return super.visitVariable(tree, unused);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            reportTreeType(tree, factory.getAnnotatedType(tree), MessageKind.DECLARED_TYPE);
            return super.visitMethod(tree, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            reportTreeType(tree, factory.methodFromUse(tree).executableType);
            return super.visitMethodInvocation(tree, unused);
        }

        @Override
        public Void visitAssignment(AssignmentTree tree, Void unused) {
            AnnotatedTypeMirror varType =
                    genFactory != null
                            ? genFactory.getAnnotatedTypeLhs(tree.getVariable())
                            : factory.getAnnotatedType(tree.getVariable());
            reportTreeType(tree, varType, MessageKind.ASSIGN_LHS_DECLARED_TYPE);
            reportTreeType(
                    tree,
                    factory.getAnnotatedType(tree.getExpression()),
                    MessageKind.ASSIGN_RHS_TYPE);
            return super.visitAssignment(tree, unused);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
            reportTreeType(tree, factory.getAnnotatedType(tree));
            AnnotatedTypeMirror varType =
                    genFactory != null
                            ? genFactory.getAnnotatedTypeLhs(tree.getVariable())
                            : factory.getAnnotatedType(tree.getVariable());
            reportTreeType(tree, varType, MessageKind.ASSIGN_LHS_DECLARED_TYPE);
            reportTreeType(
                    tree,
                    factory.getAnnotatedType(tree.getExpression()),
                    MessageKind.ASSIGN_RHS_TYPE);
            return super.visitCompoundAssignment(tree, unused);
        }

        @Override
        public Void visitUnary(UnaryTree tree, Void unused) {
            Tree.Kind treeKind = tree.getKind();
            switch (treeKind) {
                case UNARY_PLUS:
                case UNARY_MINUS:
                case BITWISE_COMPLEMENT:
                case LOGICAL_COMPLEMENT:
                case PREFIX_INCREMENT:
                case PREFIX_DECREMENT:
                    reportTreeType(tree, factory.getAnnotatedType(tree));
                    break;
                case POSTFIX_INCREMENT:
                case POSTFIX_DECREMENT:
                    reportTreeType(tree, factory.getAnnotatedType(tree));
                    if (genFactory != null) {
                        reportTreeType(
                                tree,
                                genFactory.getAnnotatedTypeRhsUnaryAssign(tree),
                                MessageKind.ASSIGN_RHS_TYPE);
                    }
                    break;
                default:
                    throw new BugInCF(
                            "Unsupported unary tree type "
                                    + treeKind
                                    + " for "
                                    + TypeInformationPresenter.class.getCanonicalName());
            }
            return super.visitUnary(tree, unused);
        }

        @Override
        public Void visitBinary(BinaryTree tree, Void unused) {
            reportTreeType(tree, factory.getAnnotatedType(tree));
            return super.visitBinary(tree, unused);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
            if (TreeUtils.isFieldAccess(tree)) {
                reportTreeType(tree, factory.getAnnotatedType(tree));
            } else if (TreeUtils.isMethodAccess(tree)) {
                reportTreeType(tree, factory.getAnnotatedType(tree), MessageKind.DECLARED_TYPE);
            }

            return super.visitMemberSelect(tree, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
            // the declared type of the functional interface
            reportTreeType(tree, factory.getAnnotatedType(tree), MessageKind.DECLARED_TYPE);
            // the use type of the functional interface
            reportTreeType(tree, factory.getFnInterfaceFromTree(tree).first);
            return super.visitMemberReference(tree, unused);
        }

        @Override
        public Void visitIdentifier(IdentifierTree tree, Void unused) {
            switch (TreeUtils.elementFromUse(tree).getKind()) {
                case ENUM_CONSTANT:
                case FIELD:
                case PARAMETER:
                case LOCAL_VARIABLE:
                case EXCEPTION_PARAMETER:
                case RESOURCE_VARIABLE:
                case CONSTRUCTOR:
                    reportTreeType(tree, factory.getAnnotatedType(tree));
                    break;
                case METHOD:
                    reportTreeType(tree, factory.getAnnotatedType(tree), MessageKind.DECLARED_TYPE);
                    break;
                default:
                    break;
            }
            return super.visitIdentifier(tree, unused);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, Void unused) {
            reportTreeType(tree, factory.getAnnotatedType(tree));
            return super.visitLiteral(tree, unused);
        }
    }
}
