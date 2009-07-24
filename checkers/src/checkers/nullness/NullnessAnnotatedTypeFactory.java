package checkers.nullness;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.*;
import checkers.nullness.quals.*;
import checkers.quals.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * Adds the {@link NonNull} annotation to a type that is:
 *
 * <ol>
 * <li value="1">(*) all literals (except for {@code null} literal)
 * <li value="2">(*) all primitive types
 * <li value="3">(*) an array-creation expression (with new)
 * <li value="4">(*) an object-creation expression (with new)
 * <li value="5">(*) a package declaration
 * <li value="6">in the scope of a {@link DefaultQualifier} annotation and
 *  matches its location criteria
 * <li value="7">determined to be {@link NonNull} by flow-sensitive inference
 * <li value="8">the class in a static member access (e.g., "System" in "System.out")
 * <li value="9">an exception parameter
 * <li value="10">the receiver type of a non-static (and non-constructor) method
 * </ol>
 *
 * Adds the {@link Raw} annotation to
 * <ol>
 * <li value="11">the receiver type of constructors.
 * </ol>
 *
 * <p>
 *
 * Additionally, the type factory will add the {@link Nullable} annotation to a
 * type if the input is
 * <ol>
 * <li value="12">(*) the null literal
 * <li value="13">of type {@link Void},
 * <li value="14">may resolve the types of some {@link NonNull} fields as
 *  {@link Nullable} depending on the presence of a {@link Raw} annotation on
 *  a constructor or method receiver.  Please review
 *  <a href="http://people.csail.mit.edu/mernst/pubs/pluggable-checkers-issta2008-abstract.html">
 *  the Checker Framework</a> for {@link Raw} semantics.
 * </ol>
 *
 * Implementation detail:  (*) cases are handled by a meta-annotation
 * rather than by code in this class.
 */
public class NullnessAnnotatedTypeFactory extends AnnotatedTypeFactory {

    private final Flow flow;
    private final QualifierDefaults defaults;
    private final TypeAnnotator typeAnnotator;
    private final TreeAnnotator treeAnnotator;
    private final QualifierPolymorphism poly;
    private final DependentTypes dependentTypes;
    /*package*/ final AnnotatedTypeFactory rawnessFactory;

    private final AnnotationCompleter completer = new AnnotationCompleter();

    /** Represents the Nullness Checker qualifiers */
    protected final AnnotationMirror POLYNULL, NONNULL, RAW, NULLABLE, LAZYNONNULL;
    Map<String, AnnotationMirror> aliases;

    private final MapGetHeauristics mapGetHeauristics;
    private final CollectionToArrayHeauristics collectionToArrayHeauristics;

    /** Creates a {@link NullnessAnnotatedTypeFactory}. */
    public NullnessAnnotatedTypeFactory(NullnessSubchecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        typeAnnotator = new NonNullTypeAnnotator(checker);
        treeAnnotator = new NonNullTreeAnnotator(checker);
        mapGetHeauristics = new MapGetHeauristics(env, this);

        POLYNULL = this.annotations.fromClass(PolyNull.class);
        NONNULL = this.annotations.fromClass(NonNull.class);
        RAW = this.annotations.fromClass(Raw.class);
        NULLABLE = this.annotations.fromClass(Nullable.class);
        LAZYNONNULL = this.annotations.fromClass(LazyNonNull.class);

        aliases = new HashMap<String, AnnotationMirror>();

        // aliases for nonnull
        aliases.put(edu.umd.cs.findbugs.annotations.NonNull.class.getCanonicalName(), NONNULL);
        aliases.put(javax.annotation.Nonnull.class.getCanonicalName(), NONNULL);
        aliases.put(org.jetbrains.annotations.NotNull.class.getCanonicalName(), NONNULL);

        // aliases for nullable
        aliases.put(edu.umd.cs.findbugs.annotations.CheckForNull.class.getCanonicalName(), NULLABLE);
        aliases.put(edu.umd.cs.findbugs.annotations.Nullable.class.getCanonicalName(), NULLABLE);
        aliases.put(edu.umd.cs.findbugs.annotations.UnknownNullness.class.getCanonicalName(), NULLABLE);
        aliases.put(javax.annotation.CheckForNull.class.getCanonicalName(), NULLABLE);
        aliases.put(javax.annotation.Nullable.class.getCanonicalName(), NULLABLE);
        aliases.put(org.jetbrains.annotations.Nullable.class.getCanonicalName(), NULLABLE);

        collectionToArrayHeauristics = new CollectionToArrayHeauristics(env, this);

        defaults = new QualifierDefaults(this, this.annotations);
        defaults.setAbsoluteDefaults(NONNULL, Collections.singleton(DefaultLocation.ALL_EXCEPT_LOCALS));

        this.poly = new QualifierPolymorphism(checker, this);
        this.dependentTypes = new DependentTypes(checker.getProcessingEnvironment(), root);

        RawnessSubchecker rawness = new RawnessSubchecker();
        rawness.currentPath = checker.currentPath;
        rawness.init(checker.getProcessingEnvironment());
        rawnessFactory = rawness.createFactory(root);

        flow = new NullnessFlow(checker, root, this);
        flow.scan(root, null);
    }

    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        if (elt instanceof VariableElement)
            annotateIfStatic(elt, type);

        typeAnnotator.visit(type);
        // case 6: apply default
        defaults.annotate(elt, type);
        if (elt instanceof TypeElement)
            type.clearAnnotations();
        completer.visit(type);
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        treeAnnotator.visit(tree, type);
        typeAnnotator.visit(type);
        // case 6: apply default
        defaults.annotate(tree, type);

        substituteRaw(tree, type);

        dependentTypes.handle(tree, type);
        final AnnotationMirror inferred = flow.test(tree);
        if (inferred != null) {
            // case 7: flow analysis
            type.clearAnnotations();
            type.addAnnotation(inferred);
        }
        completer.visit(type);
    }

    @Override
    protected AnnotatedDeclaredType getImplicitReceiverType(Tree tree) {
        AnnotatedDeclaredType type = super.getImplicitReceiverType(tree);
//        // 'this' should always be nonnull, unless it's raw
//        if (type != null && !type.hasAnnotation(RAW)) {
//            type.clearAnnotations();
//            type.addAnnotation(NONNULL);
//        }
        return type;
    }

    @Override
    public final AnnotatedDeclaredType getEnclosingType(TypeElement element, Tree tree) {
        AnnotatedDeclaredType dt = super.getEnclosingType(element, tree);
        if (dt != null && dt.hasAnnotation(NULLABLE)) {
            dt.removeAnnotation(NULLABLE);
            dt.addAnnotation(NONNULL);
        }
        return dt;
    }

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        for (AnnotatedTypeMirror supertype : supertypes) {
            typeAnnotator.visit(supertype);
            if (supertype.getKind() == TypeKind.DECLARED)
                defaults.annotateTypeElement((TypeElement)((AnnotatedDeclaredType)supertype).getUnderlyingType().asElement(), supertype);
            completer.visit(supertype);
        }
    }

    @Override
    public AnnotatedExecutableType methodFromUse(MethodInvocationTree tree) {
        AnnotatedExecutableType method = super.methodFromUse(tree);
        poly.annotate(tree, method);
//        poly.annotate(method.getElement(), method);

        mapGetHeauristics.handle(tree, method);
        collectionToArrayHeauristics.handle(tree, method);
        return method;
    }

    @Override
    public AnnotatedExecutableType constructorFromUse(NewClassTree tree) {
        AnnotatedExecutableType constructor = super.constructorFromUse(tree);
        dependentTypes.handleConstructor(tree, constructor);
        return constructor;
    }

    /**
     * Substitutes {@link Raw} annotations on a type. If the the receiver of
     * the member select expression tree (which might implicitly be "this") is
     * {@link Raw}, this replaces the annotations on {@code type} with
     * {@link Raw}.
     *
     * @param tree a select expression, which might be implicit
     * @param type the type for which to substitute annotations based on the
     *        placement of {@link Raw}
     * @return true iff we reduced the annotated type from {@link NonNull} to
     *         {@link Raw}
     */
    private boolean substituteRaw(Tree tree, AnnotatedTypeMirror type) {

        // If it's not an expression tree, it's definitely not a select.
        if (tree.getKind() != Tree.Kind.MEMBER_SELECT
                && tree.getKind() != Tree.Kind.IDENTIFIER)
            return false;

        // Identifiers might implicitly select from "this", but not if they're
        // class or interface identifiers.
        if (tree instanceof IdentifierTree) {

            Element elt = TreeUtils.elementFromUse((IdentifierTree) tree);
            if (elt == null || !elt.getKind().isField())
                return false;

            Tree decl = declarationFromElement(elt);
            if (decl != null
                && decl instanceof VariableTree
                && ((VariableTree) decl).getInitializer() != null
                && getAnnotatedType(((VariableTree) decl).getInitializer()).hasAnnotation(NONNULL))
            return false;
        }

        // case 13
        final AnnotatedTypeMirror select = rawnessFactory.getReceiver((ExpressionTree) tree);
        if (select != null && select.hasAnnotation(RAW)
                && !type.hasAnnotation(NULLABLE) && !type.getKind().isPrimitive()) {
            boolean wasNN = type.hasAnnotation(NONNULL);
            type.clearAnnotations();
            type.addAnnotation(LAZYNONNULL);
            return wasNN;
        }
        return false;
    }

    /**
     * If the element is {@link NonNull} when used in a static member access,
     *  modifies the element's type (by adding {@link NonNull}).
     *
     * @param elt the element being accessed
     * @param type the type of the element {@code elt}
     */
    private void annotateIfStatic(Element elt, AnnotatedTypeMirror type) {

        if (elt == null)
            return;

        if (elt.getKind().isClass() || elt.getKind().isInterface()
                // Workaround for System.{out,in,err} issue: assume all static
                // fields in java.lang.System are nonnull.
                || isSystemField(elt)) {
            type.clearAnnotations();
            type.addAnnotation(NONNULL);
        }
    }

    private boolean isSystemField(Element elt) {
        if (!elt.getKind().isField())
            return false;

        if (!ElementUtils.isStatic(elt) || !ElementUtils.isFinal(elt))
            return false;

        VariableElement var = (VariableElement)elt;

        // Heuristic: if we have a static final field in a system package,
        // treat it as NonNull (many like Boolean.TYPE and System.out
        // have constant value null but are set by the VM).
        boolean inJavaPackage = ElementUtils.getQualifiedClassName(var).toString().startsWith("java.");

        return (var.getConstantValue() != null
                || var.getSimpleName().contentEquals("class")
                || inJavaPackage);
    }

    /**
     * Ensures that every type is fully annotated, no type is unqualified,
     * and no type has multiple qualifiers.
     */
    private class AnnotationCompleter extends AnnotatedTypeScanner<Void, Void> {

        @Override
        protected Void scan(AnnotatedTypeMirror type, Void p) {

            if (type == null)
                return super.scan(type, p);

            if (type.getKind() == TypeKind.TYPEVAR
                    && type.getAnnotations().isEmpty())
                return super.scan(type, p);

            if (type.getAnnotations().isEmpty())
                type.addAnnotation(NULLABLE);
            else if (type.hasAnnotation(RAW))
                type.removeAnnotation(NONNULL);
            else if (type.hasAnnotation(NONNULL))
                type.removeAnnotation(NULLABLE);

            // case 13: type of Void is nullable
            if (TypesUtils.isDeclaredOfName(type.getUnderlyingType(), "java.lang.Void")
                    // Hack: Special case Void.class
                    && (type.getElement() == null || !type.getElement().getKind().isClass())) {
                type.clearAnnotations();
                type.addAnnotation(NULLABLE);
            }

            assert !type.getAnnotations().isEmpty() : type;

            return super.scan(type, p);
        }
    }

    /**
     * Adds {@link NonNull} and {@link Raw} annotations to the type based on
     * the underlying (unannotated) type.
     */
    private class NonNullTypeAnnotator extends TypeAnnotator {

        /** Creates a {@link NonNullTypeAnnotator} for the given checker. */
        NonNullTypeAnnotator(BaseTypeChecker checker) {
            super(checker);
        }

        /**
         * cases 10, 11
         * Adds {@link NonNull} and {@link Raw} annotations to the unannotated
         * method receiver
         */
        @Override
        public Void visitExecutable(AnnotatedExecutableType type, ElementKind p) {

            // Don't add implicit receiver annotations if some are already there.
            if (!type.getReceiverType().getAnnotations().isEmpty())
                return super.visitExecutable(type, p);

            ExecutableElement elt = type.getElement();
            assert elt != null;

            if (elt.getKind() == ElementKind.CONSTRUCTOR)
                // case 11. Add @Raw to constructors.
                type.getReceiverType().addAnnotation(RAW);
            else if (!ElementUtils.isStatic(elt))
                // case 10 Add @NonNull to non-static non-constructors.
                type.getReceiverType().addAnnotation(NONNULL);

            return super.visitExecutable(type, p);
        }
    }

    /**
     * Adds {@link NonNull} annotations to a type based on the AST from which
     * the type was obtained.
     */
    private class NonNullTreeAnnotator extends TreeAnnotator {

        /** Creates a {@link NonNullTreeAnnotator} for the given checker. */
        NonNullTreeAnnotator(BaseTypeChecker checker) {
            super(checker);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;
            // case 8: class in static member access
            annotateIfStatic(elt, type);
            return super.visitMemberSelect(node, type);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;

            // case 8. static method access
            annotateIfStatic(elt, type);

            // Workaround: exception parameters should be implicitly
            // NonNull, but due to a compiler bug they have
            // kind PARAMETER instead of EXCEPTION_PARAMETER. Until it's
            // fixed we manually inspect enclosing catch blocks.
            // case 9. exception parameter
            if (isExceptionParameter(node))
                type.addAnnotation(NONNULL);

            return super.visitIdentifier(node, type);
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            if (type.getAnnotations().isEmpty()) {
                AnnotatedTypeMirror exprType = getAnnotatedType(node.getExpression());
                type.addAnnotations(exprType.getAnnotations());
            }
            return super.visitTypeCast(node, type);
        }

        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror type) {
            // A constructor that invokes another constructor in the first
            // statement is nonnull by default, as all the fields
            // should be initialized by the first one
            AnnotatedExecutableType execType = (AnnotatedExecutableType)type;
            if (execType.getReceiverType().getAnnotations().isEmpty()
                    && TreeUtils.containsThisConstructorInvocation(node))
            execType.getReceiverType().addAnnotation(NONNULL);

            return super.visitMethod(node, type);
        }

        private boolean isExceptionParameter(IdentifierTree node) {
            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;
            if (elt.getKind() != ElementKind.PARAMETER
                    || !TypesUtils.isThrowable(elt.asType()))
                return false;
            final TreePath path = getPath(node);
            CatchTree ct = (CatchTree)TreeUtils.enclosingOfKind(path, Tree.Kind.CATCH);
            if (ct == null)
                return false;

            final VariableTree catchParamTree = ct.getParameter();
            final VariableElement catchParamElt = TreeUtils.elementFromDeclaration(catchParamTree);
            return elt.equals(catchParamElt);
        }
    }

    /**
     * Aliased annotations.
     *
     */
    protected AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        TypeElement elem = (TypeElement)a.getAnnotationType().asElement();

        String qualName = elem.getQualifiedName().toString();
        return aliases.get(qualName);
    }

}
