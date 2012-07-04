package checkers.nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.CommitmentAnnotatedTypeFactory;
import checkers.nonnull.quals.AssertNonNullAfter;
import checkers.nonnull.quals.AssertNonNullIfFalse;
import checkers.nonnull.quals.AssertNonNullIfNonNull;
import checkers.nonnull.quals.AssertNonNullIfTrue;
import checkers.nonnull.quals.AssertParametersNonNull;
import checkers.nonnull.quals.LazyNonNull;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.NonNullOnEntry;
import checkers.nonnull.quals.Nullable;
import checkers.nonnull.quals.Pure;
import checkers.nullness.quals.Primitive;
import checkers.quals.DefaultLocation;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypes;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

public class NonNullAnnotatedTypeFactory extends
		CommitmentAnnotatedTypeFactory<NonNullChecker> {

	/** Annotation constants */
	protected final AnnotationMirror NONNULL, NULLABLE, PRIMITIVE;

	public NonNullAnnotatedTypeFactory(NonNullChecker checker,
			CompilationUnitTree root) {
		super(checker, root);

		NONNULL = this.annotations.fromClass(NonNull.class);
		NULLABLE = this.annotations.fromClass(Nullable.class);
		PRIMITIVE = this.annotations.fromClass(Primitive.class);

		// aliases with checkers.nullness.quals qualifiers
		addAliasedAnnotation(checkers.nullness.quals.NonNull.class, NONNULL);
		addAliasedAnnotation(checkers.nullness.quals.Nullable.class, NULLABLE);

		addOurAlias(checkers.nullness.quals.AssertNonNullAfter.class,
				AssertNonNullAfter.class);
		addOurAlias(checkers.nullness.quals.AssertNonNullIfFalse.class,
				AssertNonNullIfFalse.class);
		addOurAlias(checkers.nullness.quals.AssertNonNullIfTrue.class,
				AssertNonNullIfTrue.class);
		addOurAlias(checkers.nullness.quals.AssertNonNullIfNonNull.class,
				AssertNonNullIfNonNull.class);
		addOurAlias(checkers.nullness.quals.AssertParametersNonNull.class,
				AssertParametersNonNull.class);
		addOurAlias(checkers.nullness.quals.Pure.class, Pure.class);
		addOurAlias(checkers.nullness.quals.NonNullOnEntry.class,
				NonNullOnEntry.class);
		addOurAlias(checkers.nullness.quals.LazyNonNull.class,
				LazyNonNull.class);

		// aliases borrowed from NullnessAnnotatedTypeFactory
		addAliasedAnnotation(com.sun.istack.NotNull.class, NONNULL);
		addAliasedAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class,
				NONNULL);
		addAliasedAnnotation(javax.annotation.Nonnull.class, NONNULL);
		addAliasedAnnotation(javax.validation.constraints.NotNull.class,
				NONNULL);
		addAliasedAnnotation(org.jetbrains.annotations.NotNull.class, NONNULL);
		addAliasedAnnotation(org.netbeans.api.annotations.common.NonNull.class,
				NONNULL);
		addAliasedAnnotation(org.jmlspecs.annotation.NonNull.class, NONNULL);
		addAliasedAnnotation(com.sun.istack.Nullable.class, NULLABLE);
		addAliasedAnnotation(
				edu.umd.cs.findbugs.annotations.CheckForNull.class, NULLABLE);
		addAliasedAnnotation(edu.umd.cs.findbugs.annotations.Nullable.class,
				NULLABLE);
		addAliasedAnnotation(
				edu.umd.cs.findbugs.annotations.UnknownNullness.class, NULLABLE);
		addAliasedAnnotation(javax.annotation.CheckForNull.class, NULLABLE);
		addAliasedAnnotation(javax.annotation.Nullable.class, NULLABLE);
		addAliasedAnnotation(org.jetbrains.annotations.Nullable.class, NULLABLE);
		addAliasedAnnotation(
				org.netbeans.api.annotations.common.CheckForNull.class,
				NULLABLE);
		addAliasedAnnotation(
				org.netbeans.api.annotations.common.NullAllowed.class, NULLABLE);
		addAliasedAnnotation(
				org.netbeans.api.annotations.common.NullUnknown.class, NULLABLE);
		addAliasedAnnotation(org.jmlspecs.annotation.Nullable.class, NULLABLE);

		defaults.addAbsoluteDefault(NONNULL,
				Collections.singleton(DefaultLocation.ALL_EXCEPT_LOCALS));
		// defaults.addAbsoluteDefault(COMMITTED,
		// Collections.singleton(DefaultLocation.ALL_EXCEPT_LOCALS));
		Set<AnnotationMirror> localdef = new HashSet<AnnotationMirror>();
		localdef.add(NULLABLE);
		localdef.add(COMMITTED);
		defaults.setLocalVariableDefault(localdef);

		Set<AnnotationMirror> flowQuals = new HashSet<AnnotationMirror>();
		flowQuals.add(NONNULL);
		flowQuals.add(PRIMITIVE);
		nnFlow = new NonNullFlow(checker, root, flowQuals, this);

		realPostInit();
	}

	@Override
	protected void postInit() {
		// hack: do nothing on call from the super constructor to ensure that
		// the real postinit is only called once, at the end of the subclass
		// constructor.
	}

	protected void realPostInit() {
		super.postInit();
		getFlow().scan(root);
	}
	
	protected AnnotatedTypeMirror getDeclaredAndDefaultedAnnotatedType(Tree tree) {
		HACK_DONT_CALL_POST_AS_MEMBER = true;
		SHOULD_CACHE = false;
		
		AnnotatedTypeMirror type = getAnnotatedType(tree);
		
		SHOULD_CACHE = true;
		HACK_DONT_CALL_POST_AS_MEMBER = false;

		return type;
	}

	@Override
	protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {

		// treat LazyNonNull as Nullable, except for:
		// 1. flow will preserve nonnull information about LazyNonNull fields
		// over method calls (see NonNullFlow).
		// 2. only nonnull can be assigned to lazynonnull fields
		// (see NonNullVisitor.commonAssignmentCheck).
		if (TreeUtils.isFieldAccess(tree)) {
			Element el;
			if (tree.getKind().equals(Tree.Kind.IDENTIFIER)) {
				el = TreeUtils.elementFromUse((IdentifierTree) tree);
			} else {
				// cast is safe: isFieldAccess is only true for identifiers or
				// memberselects
				el = TreeUtils.elementFromUse((MemberSelectTree) tree);
			}
			if (getAliasedDeclAnnotation(el, LazyNonNull.class) != null) {
				changeAnnotationInOneHierarchy(type, NULLABLE);
			}
		}

		// determines commitment type (and nullability based on commitment type)
		super.annotateImplicit(tree, type);
		// refine nullability with flow
		AnnotationMirror inferred = getFlow().test(tree);
		if (inferred != null) {
			assert AnnotationUtils.areSame(inferred, NONNULL);
			changeAnnotationInOneHierarchy(type, NONNULL);
		}
	}

	/**
	 * Replace the currently present annotation from the type hierarchy of a
	 * from type and add a instead.
	 * 
	 * @param type
	 *            The type to modify.
	 * @param a
	 *            The annotation that should be present afterwards.
	 */
	private void changeAnnotationInOneHierarchy(AnnotatedTypeMirror type,
			AnnotationMirror a) {
		for (AnnotationMirror other : checker.getNonNullAnnotations()) {
			type.removeAnnotation(other);
		}
		type.addAnnotation(a);
	}

	@Override
	protected TypeAnnotator createTypeAnnotator(NonNullChecker checker) {
		return new NonNullTypeAnnotator(checker);
	}

	@Override
	protected TreeAnnotator createTreeAnnotator(NonNullChecker checker) {
		return new NonNullTreeAnnotator(checker);
	}

	/**
	 * If the element is {@link NonNull} when used in a static member access,
	 * modifies the element's type (by adding {@link NonNull}).
	 * 
	 * @param elt
	 *            the element being accessed
	 * @param type
	 *            the type of the element {@code elt}
	 */
	private void annotateIfStatic(Element elt, AnnotatedTypeMirror type) {

		if (elt == null)
			return;

		if (elt.getKind().isClass() || elt.getKind().isInterface()
		// Workaround for System.{out,in,err} issue: assume all static
		// fields in java.lang.System are nonnull.
				|| isSystemField(elt)) {

			changeAnnotationInOneHierarchy(type, NONNULL);
		}
	}

	private boolean isSystemField(Element elt) {
		if (!elt.getKind().isField())
			return false;

		if (!ElementUtils.isStatic(elt) || !ElementUtils.isFinal(elt))
			return false;

		VariableElement var = (VariableElement) elt;

		// Heuristic: if we have a static final field in a system package,
		// treat it as NonNull (many like Boolean.TYPE and System.out
		// have constant value null but are set by the VM).
		boolean inJavaPackage = ElementUtils.getQualifiedClassName(var)
				.toString().startsWith("java.");

		return (var.getConstantValue() != null
				|| var.getSimpleName().contentEquals("class") || inJavaPackage);
	}

	private boolean isExceptionParameter(IdentifierTree node) {
		Element elt = TreeUtils.elementFromUse(node);
		assert elt != null;
		return elt.getKind() == ElementKind.EXCEPTION_PARAMETER;
	}

	protected class NonNullTreeAnnotator
			extends
			CommitmentAnnotatedTypeFactory<NonNullChecker>.CommitmentTreeAnnotator {
		public NonNullTreeAnnotator(BaseTypeChecker checker) {
			super(checker);
		}

		@Override
		public Void visitMemberSelect(MemberSelectTree node,
				AnnotatedTypeMirror type) {

			Element elt = TreeUtils.elementFromUse(node);
			assert elt != null;
			// case 8: class in static member access
			annotateIfStatic(elt, type);
			return super.visitMemberSelect(node, type);
		}

		@Override
		public Void visitIdentifier(IdentifierTree node,
				AnnotatedTypeMirror type) {

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

	}

	protected class NonNullTypeAnnotator
			extends
			CommitmentAnnotatedTypeFactory<NonNullChecker>.CommitmentTypeAnnotator {
		public NonNullTypeAnnotator(BaseTypeChecker checker) {
			super(checker);
		}

	}
}
