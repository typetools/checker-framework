package org.checkerframework.framework.ajava;

// TODO: Fix imports.
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import java.util.List;

public abstract class AnnotationInsertVisitor implements VoidVisitor<Node> {
    private void transferAnnotations(Node src, Node dest) {
        if (!(src instanceof NodeWithAnnotations<?>) || !(dest instanceof NodeWithAnnotations<?>)) {
            return;
        }

        NodeWithAnnotations<?> srcAnnos = (NodeWithAnnotations<?>) src;
        NodeWithAnnotations<?> destAnnos = (NodeWithAnnotations<?>) dest;
        for (AnnotationExpr annotation : srcAnnos.getAnnotations()) {
            destAnnos.addAnnotation(annotation);
        }
    }

    private void visitLists(List<? extends Node> list1, List<? extends Node> list2) {
        assert list1.size() == list2.size();
        for (int i = 0; i < list1.size(); i++) {
            list1.get(i).accept(this, list2.get(i));
        }
    }

    @Override
    public void visit(final AnnotationDeclaration n, final Node other) {
        AnnotationDeclaration node = (AnnotationDeclaration) other;
        visitLists(n.getMembers(), node.getMembers());
        visitLists(n.getModifiers(), node.getModifiers());
        n.getName().accept(this, node.getName());
        n.getComment().ifPresent(l -> l.accept(this, node.getComment().get()));
    }

    @Override
    public void visit(final AnnotationMemberDeclaration n, final Node other) {
        n.getDefaultValue().ifPresent(l -> l.accept(this, arg));
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getType().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayAccessExpr n, final Node other) {
        n.getIndex().accept(this, arg);
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayCreationExpr n, final Node other) {
        n.getElementType().accept(this, arg);
        n.getInitializer().ifPresent(l -> l.accept(this, arg));
        n.getLevels().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayInitializerExpr n, final Node other) {
        n.getValues().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final AssertStmt n, final Node other) {
        n.getCheck().accept(this, arg);
        n.getMessage().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final AssignExpr n, final Node other) {
        n.getTarget().accept(this, arg);
        n.getValue().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BinaryExpr n, final Node other) {
        n.getLeft().accept(this, arg);
        n.getRight().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BlockComment n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BlockStmt n, final Node other) {
        n.getStatements().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BooleanLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BreakStmt n, final Node other) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CastExpr n, final Node other) {
        n.getExpression().accept(this, arg);
        n.getType().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CatchClause n, final Node other) {
        n.getBody().accept(this, arg);
        n.getParameter().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CharLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ClassExpr n, final Node other) {
        n.getType().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final Node other) {
        n.getExtendedTypes().forEach(p -> p.accept(this, arg));
        n.getImplementedTypes().forEach(p -> p.accept(this, arg));
        n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getMembers().forEach(p -> p.accept(this, arg));
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ClassOrInterfaceType n, final Node other) {
        n.getName().accept(this, arg);
        n.getScope().ifPresent(l -> l.accept(this, arg));
        n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CompilationUnit n, final Node other) {
        n.getImports().forEach(p -> p.accept(this, arg));
        n.getModule().ifPresent(l -> l.accept(this, arg));
        n.getPackageDeclaration().ifPresent(l -> l.accept(this, arg));
        n.getTypes().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ConditionalExpr n, final Node other) {
        n.getCondition().accept(this, arg);
        n.getElseExpr().accept(this, arg);
        n.getThenExpr().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ConstructorDeclaration n, final Node other) {
        n.getBody().accept(this, arg);
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getParameters().forEach(p -> p.accept(this, arg));
        n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
        n.getThrownExceptions().forEach(p -> p.accept(this, arg));
        n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ContinueStmt n, final Node other) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final DoStmt n, final Node other) {
        n.getBody().accept(this, arg);
        n.getCondition().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final DoubleLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EmptyStmt n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EnclosedExpr n, final Node other) {
        n.getInner().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EnumConstantDeclaration n, final Node other) {
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getClassBody().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EnumDeclaration n, final Node other) {
        n.getEntries().forEach(p -> p.accept(this, arg));
        n.getImplementedTypes().forEach(p -> p.accept(this, arg));
        n.getMembers().forEach(p -> p.accept(this, arg));
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ExplicitConstructorInvocationStmt n, final Node other) {
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getExpression().ifPresent(l -> l.accept(this, arg));
        n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ExpressionStmt n, final Node other) {
        n.getExpression().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final FieldAccessExpr n, final Node other) {
        n.getName().accept(this, arg);
        n.getScope().accept(this, arg);
        n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final FieldDeclaration n, final Node other) {
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getVariables().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ForEachStmt n, final Node other) {
        n.getBody().accept(this, arg);
        n.getIterable().accept(this, arg);
        n.getVariable().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ForStmt n, final Node other) {
        n.getBody().accept(this, arg);
        n.getCompare().ifPresent(l -> l.accept(this, arg));
        n.getInitialization().forEach(p -> p.accept(this, arg));
        n.getUpdate().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final IfStmt n, final Node other) {
        n.getCondition().accept(this, arg);
        n.getElseStmt().ifPresent(l -> l.accept(this, arg));
        n.getThenStmt().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final InitializerDeclaration n, final Node other) {
        n.getBody().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final InstanceOfExpr n, final Node other) {
        n.getExpression().accept(this, arg);
        n.getType().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final IntegerLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final JavadocComment n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LabeledStmt n, final Node other) {
        n.getLabel().accept(this, arg);
        n.getStatement().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LineComment n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LongLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MarkerAnnotationExpr n, final Node other) {
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MemberValuePair n, final Node other) {
        n.getName().accept(this, arg);
        n.getValue().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MethodCallExpr n, final Node other) {
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getScope().ifPresent(l -> l.accept(this, arg));
        n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MethodDeclaration n, final Node other) {
        n.getBody().ifPresent(l -> l.accept(this, arg));
        n.getType().accept(this, arg);
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getParameters().forEach(p -> p.accept(this, arg));
        n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
        n.getThrownExceptions().forEach(p -> p.accept(this, arg));
        n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final NameExpr n, final Node other) {
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final NormalAnnotationExpr n, final Node other) {
        n.getPairs().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final NullLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ObjectCreationExpr n, final Node other) {
        n.getAnonymousClassBody().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getScope().ifPresent(l -> l.accept(this, arg));
        n.getType().accept(this, arg);
        n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final PackageDeclaration n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final Parameter n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getType().accept(this, arg);
        n.getVarArgsAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final PrimitiveType n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final Name n, final Node other) {
        n.getQualifier().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SimpleName n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayType n, final Node other) {
        n.getComponentType().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayCreationLevel n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getDimension().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final IntersectionType n, final Node other) {
        n.getElements().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnionType n, final Node other) {
        n.getElements().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ReturnStmt n, final Node other) {
        n.getExpression().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SingleMemberAnnotationExpr n, final Node other) {
        n.getMemberValue().accept(this, arg);
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final StringLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SuperExpr n, final Node other) {
        n.getTypeName().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SwitchEntry n, final Node other) {
        n.getLabels().forEach(p -> p.accept(this, arg));
        n.getStatements().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SwitchStmt n, final Node other) {
        n.getEntries().forEach(p -> p.accept(this, arg));
        n.getSelector().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SynchronizedStmt n, final Node other) {
        n.getBody().accept(this, arg);
        n.getExpression().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ThisExpr n, final Node other) {
        n.getTypeName().ifPresent(l -> l.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ThrowStmt n, final Node other) {
        n.getExpression().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TryStmt n, final Node other) {
        n.getCatchClauses().forEach(p -> p.accept(this, arg));
        n.getFinallyBlock().ifPresent(l -> l.accept(this, arg));
        n.getResources().forEach(p -> p.accept(this, arg));
        n.getTryBlock().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LocalClassDeclarationStmt n, final Node other) {
        n.getClassDeclaration().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TypeParameter n, final Node other) {
        n.getName().accept(this, arg);
        n.getTypeBound().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnaryExpr n, final Node other) {
        n.getExpression().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnknownType n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VariableDeclarationExpr n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getVariables().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VariableDeclarator n, final Node other) {
        n.getInitializer().ifPresent(l -> l.accept(this, arg));
        n.getName().accept(this, arg);
        n.getType().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VoidType n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final WhileStmt n, final Node other) {
        n.getBody().accept(this, arg);
        n.getCondition().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final WildcardType n, final Node other) {
        n.getExtendedType().ifPresent(l -> l.accept(this, arg));
        n.getSuperType().ifPresent(l -> l.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LambdaExpr n, final Node other) {
        n.getBody().accept(this, arg);
        n.getParameters().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MethodReferenceExpr n, final Node other) {
        n.getScope().accept(this, arg);
        n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TypeExpr n, final Node other) {
        n.getType().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(NodeList n, Node other) {
        for (Object node : n) {
            ((Node) node).accept(this, arg);
        }
    }

    @Override
    public void visit(final ImportDeclaration n, final Node other) {
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    public void visit(final ModuleDeclaration n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getDirectives().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    public void visit(final ModuleRequiresDirective n, final Node other) {
        n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleExportsDirective n, final Node other) {
        n.getModuleNames().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleProvidesDirective n, final Node other) {
        n.getName().accept(this, arg);
        n.getWith().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleUsesDirective n, final Node other) {
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleOpensDirective n, final Node other) {
        n.getModuleNames().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnparsableStmt n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ReceiverParameter n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getName().accept(this, arg);
        n.getType().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VarType n, final Node other) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final Modifier n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SwitchExpr n, final Node other) {
        n.getEntries().forEach(p -> p.accept(this, arg));
        n.getSelector().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TextBlockLiteralExpr n, final Node other) {
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final YieldStmt n, final Node other) {
        n.getExpression().accept(this, arg);
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }
}
