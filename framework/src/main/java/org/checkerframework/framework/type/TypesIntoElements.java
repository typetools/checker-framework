package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;

/**
 * A helper class that puts the annotations from an AnnotatedTypeMirrors back into the corresponding
 * Elements, so that they get stored in the bytecode by the compiler.
 *
 * <p>This has kind-of the symmetric function to {@code TypeFromElement}.
 *
 * <p>This class deals with javac internals and liberally imports such classes.
 */
public class TypesIntoElements {

    /**
     * The entry point.
     *
     * @param processingEnv the environment
     * @param atypeFactory the type factory
     * @param tree the ClassTree to process
     */
    public static void store(
            ProcessingEnvironment processingEnv,
            AnnotatedTypeFactory atypeFactory,
            ClassTree tree) {
        Symbol.ClassSymbol csym = (Symbol.ClassSymbol) TreeUtils.elementFromDeclaration(tree);
        Types types = processingEnv.getTypeUtils();

        storeTypeParameters(processingEnv, types, atypeFactory, tree.getTypeParameters(), csym);

        /* TODO: storing extends/implements types results in
         * a strange error e.g. from the Nullness Checker.
         * I think somewhere we take the annotations on extends/implements as
         * the receiver annotation on a constructor, breaking logic there.
         * I assume that the problem is the default that we use for these locations.
         * Once we've decided the defaulting, enable this.
         * See example of code that fails when this is enabled in
         * checker/jtreg/nullness/annotationsOnExtends. Also, see https://github
         * .com/typetools/checker-framework/pull/876 for a better implementation (though it also
         * causes the error).
        storeClassExtends(processingEnv, types, atypeFactory, tree.getExtendsClause(), csym, -1);
        {
            int implidx = 0;
            for (Tree imp : tree.getImplementsClause()) {
                storeClassExtends(processingEnv, types, atypeFactory, imp, csym, implidx);
                ++implidx;
            }
        }
        */

        for (Tree mem : tree.getMembers()) {
            if (mem.getKind() == Tree.Kind.METHOD) {
                storeMethod(processingEnv, types, atypeFactory, (MethodTree) mem);
            } else if (mem.getKind() == Tree.Kind.VARIABLE) {
                storeVariable(processingEnv, types, atypeFactory, (VariableTree) mem);
            } else {
                // System.out.println("Unhandled member tree: " + mem);
            }
        }
    }

    private static void storeMethod(
            ProcessingEnvironment processingEnv,
            Types types,
            AnnotatedTypeFactory atypeFactory,
            MethodTree meth) {
        AnnotatedExecutableType mtype = atypeFactory.getAnnotatedType(meth);
        MethodSymbol sym = (MethodSymbol) TreeUtils.elementFromDeclaration(meth);
        TypeAnnotationPosition tapos;
        List<Attribute.TypeCompound> tcs = List.nil();

        storeTypeParameters(processingEnv, types, atypeFactory, meth.getTypeParameters(), sym);

        {
            // return type
            JCTree ret = ((JCTree.JCMethodDecl) meth).getReturnType();
            if (ret != null) {
                tapos = TypeAnnotationUtils.methodReturnTAPosition(ret.pos);
                tcs =
                        tcs.appendList(
                                generateTypeCompounds(processingEnv, mtype.getReturnType(), tapos));
            }
        }
        {
            // receiver
            JCTree recv = ((JCTree.JCMethodDecl) meth).getReceiverParameter();
            if (recv != null) {
                tapos = TypeAnnotationUtils.methodReceiverTAPosition(recv.pos);
                tcs =
                        tcs.appendList(
                                generateTypeCompounds(
                                        processingEnv, mtype.getReceiverType(), tapos));
            }
        }
        {
            // parameters
            int pidx = 0;
            java.util.List<AnnotatedTypeMirror> ptypes = mtype.getParameterTypes();
            for (JCTree param : ((JCTree.JCMethodDecl) meth).getParameters()) {
                tapos = TypeAnnotationUtils.methodParameterTAPosition(pidx, param.pos);
                tcs = tcs.appendList(generateTypeCompounds(processingEnv, ptypes.get(pidx), tapos));
                ++pidx;
            }
        }
        {
            // throws clauses
            int tidx = 0;
            java.util.List<AnnotatedTypeMirror> ttypes = mtype.getThrownTypes();
            for (JCTree thr : ((JCTree.JCMethodDecl) meth).getThrows()) {
                tapos = TypeAnnotationUtils.methodThrowsTAPosition(tidx, thr.pos);
                tcs = tcs.appendList(generateTypeCompounds(processingEnv, ttypes.get(tidx), tapos));
                ++tidx;
            }
        }

        addUniqueTypeCompounds(types, sym, tcs);
    }

    private static void storeVariable(
            ProcessingEnvironment processingEnv,
            Types types,
            AnnotatedTypeFactory atypeFactory,
            VariableTree var) {
        VarSymbol sym = (VarSymbol) TreeUtils.elementFromDeclaration(var);
        AnnotatedTypeMirror type;
        if (atypeFactory instanceof GenericAnnotatedTypeFactory) {
            // TODO: this is rather ugly: we do not want refinement from the
            // initializer of the field. We need a general way to get
            // the "defaulted" type of a variable.
            type =
                    ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                            .getAnnotatedTypeLhs(var);
        } else {
            type = atypeFactory.getAnnotatedType(var);
        }

        TypeAnnotationPosition tapos = TypeAnnotationUtils.fieldTAPosition(((JCTree) var).pos);

        List<Attribute.TypeCompound> tcs;
        tcs = generateTypeCompounds(processingEnv, type, tapos);
        addUniqueTypeCompounds(types, sym, tcs);
    }

    @SuppressWarnings("unused") // TODO: see usage in comments above
    private static void storeClassExtends(
            ProcessingEnvironment processingEnv,
            Types types,
            AnnotatedTypeFactory atypeFactory,
            Tree ext,
            Symbol.ClassSymbol csym,
            int implidx) {

        AnnotatedTypeMirror type;
        int pos;
        if (ext == null) {
            // The implicit superclass is always java.lang.Object.
            // TODO: is this a good way to get the type?
            type = atypeFactory.fromElement(csym.getSuperclass().asElement());
            pos = -1;
        } else {
            type = atypeFactory.getAnnotatedTypeFromTypeTree(ext);
            pos = ((JCTree) ext).pos;
        }

        TypeAnnotationPosition tapos = TypeAnnotationUtils.classExtendsTAPosition(implidx, pos);

        List<Attribute.TypeCompound> tcs;
        tcs = generateTypeCompounds(processingEnv, type, tapos);
        addUniqueTypeCompounds(types, csym, tcs);
    }

    private static void storeTypeParameters(
            ProcessingEnvironment processingEnv,
            Types types,
            AnnotatedTypeFactory atypeFactory,
            java.util.List<? extends TypeParameterTree> tps,
            Symbol sym) {
        boolean isClassOrInterface = sym.getKind().isClass() || sym.getKind().isInterface();
        List<Attribute.TypeCompound> tcs = List.nil();

        int tpidx = 0;
        for (TypeParameterTree tp : tps) {
            AnnotatedTypeVariable typeVar =
                    (AnnotatedTypeVariable) atypeFactory.getAnnotatedTypeFromTypeTree(tp);
            // System.out.println("The Type for type parameter " + tp + " is " + type);

            TypeAnnotationPosition tapos;
            // Note: we use the type parameter pos also for the bounds;
            // the bounds may not be explicit and we couldn't look up separate pos.
            if (isClassOrInterface) {
                tapos = TypeAnnotationUtils.typeParameterTAPosition(tpidx, ((JCTree) tp).pos);
            } else {
                tapos = TypeAnnotationUtils.methodTypeParameterTAPosition(tpidx, ((JCTree) tp).pos);
            }

            { // This block is essentially direct annotations, perhaps we should refactor that
                // method out
                List<Attribute.TypeCompound> res = List.nil();
                for (AnnotationMirror am : typeVar.getLowerBound().getAnnotations()) {
                    Attribute.TypeCompound tc =
                            TypeAnnotationUtils.createTypeCompoundFromAnnotationMirror(
                                    am, tapos, processingEnv);
                    res = res.prepend(tc);
                }
                tcs = tcs.appendList(res);
            }

            AnnotatedTypeMirror tpbound = typeVar.getUpperBound();
            java.util.List<? extends AnnotatedTypeMirror> bounds;
            if (tpbound.getKind() == TypeKind.INTERSECTION) {
                bounds =
                        ((AnnotatedTypeMirror.AnnotatedIntersectionType) tpbound)
                                .directSuperTypes();
            } else {
                bounds = List.of(tpbound);
            }

            int bndidx = 0;
            for (AnnotatedTypeMirror bound : bounds) {
                if (bndidx == 0 && ((Type) bound.getUnderlyingType()).isInterface()) {
                    // If the first bound is an interface, there is an implicit java.lang.Object
                    ++bndidx;
                }

                if (isClassOrInterface) {
                    tapos =
                            TypeAnnotationUtils.typeParameterBoundTAPosition(
                                    tpidx, bndidx, ((JCTree) tp).pos);
                } else {
                    tapos =
                            TypeAnnotationUtils.methodTypeParameterBoundTAPosition(
                                    tpidx, bndidx, ((JCTree) tp).pos);
                }

                tcs = tcs.appendList(generateTypeCompounds(processingEnv, bound, tapos));
                ++bndidx;
            }
            ++tpidx;
        }

        // System.out.println("Adding " + tcs + " to " + sym);
        addUniqueTypeCompounds(types, sym, tcs);
    }

    private static void addUniqueTypeCompounds(Types types, Symbol sym, List<TypeCompound> tcs) {
        List<TypeCompound> raw = sym.getRawTypeAttributes();
        List<Attribute.TypeCompound> res = List.nil();

        for (Attribute.TypeCompound tc : tcs) {
            if (!TypeAnnotationUtils.isTypeCompoundContained(raw, tc, types)) {
                res = res.append(tc);
            }
        }
        // That method only uses reference equality. isTypeCompoundContained does a deep comparison.
        sym.appendUniqueTypeAttributes(res);
    }

    // Do not return null.  Return List.nil() if there are no TypeCompounds to return.
    private static List<Attribute.TypeCompound> generateTypeCompounds(
            ProcessingEnvironment processingEnv,
            AnnotatedTypeMirror type,
            TypeAnnotationPosition tapos) {
        return new TCConvert(processingEnv).scan(type, tapos);
    }

    /**
     * Convert an AnnotatedTypeMirror and a TypeAnnotationPosition into the corresponding
     * TypeCompounds.
     */
    private static class TCConvert
            extends AnnotatedTypeScanner<List<Attribute.TypeCompound>, TypeAnnotationPosition> {

        private final ProcessingEnvironment processingEnv;

        TCConvert(ProcessingEnvironment processingEnv) {
            this.processingEnv = processingEnv;
        }

        @Override
        public List<TypeCompound> scan(AnnotatedTypeMirror type, TypeAnnotationPosition pos) {
            if (pos == null) {
                throw new BugInCF("TypesIntoElements: invalid usage, null pos with type: " + type);
            }
            List<TypeCompound> res = super.scan(type, pos);
            return res;
        }

        @Override
        protected List<TypeCompound> scan(
                Iterable<? extends AnnotatedTypeMirror> types, TypeAnnotationPosition pos) {
            if (types == null) {
                return List.nil();
            }
            return super.scan(types, pos);
        }

        @Override
        public List<TypeCompound> reduce(List<TypeCompound> r1, List<TypeCompound> r2) {
            if (r1 == null) {
                return r2;
            }
            if (r2 == null) {
                return r1;
            }
            return r1.appendList(r2);
        }

        private List<TypeCompound> directAnnotations(
                AnnotatedTypeMirror type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res = List.nil();

            for (AnnotationMirror am : type.getAnnotations()) {
                // TODO: I BELIEVE THIS ISN'T TRUE BECAUSE PARAMETERS MAY HAVE ANNOTATIONS THAT CAME
                // FROM THE ELEMENT OF THE CLASS WHICH PREVIOUSLY WAS WRITTEN OUT BY
                // TYPESINTOELEMENT.
                //                if (am instanceof Attribute.TypeCompound) {
                //                    // If it is a TypeCompound it was already present in source
                // (right?),
                //                    // so there is nothing to do.
                //                    // System.out.println("  found TypeComound: " + am + " pos: "
                // + ((Attribute.TypeCompound)am).position);
                //                } else {
                // TODO: DOES THIS LEAD TO DOUBLING UP ON THE SAME ANNOTATION IN THE ELEMENT?
                Attribute.TypeCompound tc =
                        TypeAnnotationUtils.createTypeCompoundFromAnnotationMirror(
                                am, tapos, processingEnv);
                res = res.prepend(tc);
                //                }
            }
            return res;
        }

        @Override
        public List<TypeCompound> visitDeclared(
                AnnotatedDeclaredType type, TypeAnnotationPosition tapos) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            // Hack for termination
            visitedNodes.put(type, List.nil());
            List<Attribute.TypeCompound> res;

            TypeAnnotationPosition oldpos = TypeAnnotationUtils.copyTAPosition(tapos);
            locateNestedTypes(type, tapos);

            res = directAnnotations(type, tapos);

            // we sometimes fix-up raw types with wildcards, do not write these into the bytecode as
            // there are no corresponding type arguments and therefore no location to actually add
            // them to
            if (!type.wasRaw()) {
                int arg = 0;
                for (AnnotatedTypeMirror ta : type.getTypeArguments()) {
                    TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTAPosition(tapos);
                    newpos.location =
                            tapos.location.append(
                                    new TypePathEntry(TypePathEntryKind.TYPE_ARGUMENT, arg));
                    res = scanAndReduce(ta, newpos, res);
                    ++arg;
                }
            }

            AnnotatedTypeMirror encl = type.getEnclosingType();
            if (encl != null
                    && encl.getKind() != TypeKind.NONE
                    && encl.getKind() != TypeKind.ERROR) {
                // use original tapos
                res = scanAndReduce(encl, oldpos, res);
            }
            visitedNodes.put(type, res);
            return res;
        }

        /* Modeled after
         * {@link com.sun.tools.javac.code.TypeAnnotations.TypeAnnotationPositions#locateNestedTypes(Type, TypeAnnotationPosition)}
         */
        private void locateNestedTypes(AnnotatedDeclaredType type, TypeAnnotationPosition p) {
            // The number of "steps" to get from the full type to the
            // left-most outer type.
            ListBuffer<TypePathEntry> depth = new ListBuffer<>();

            Type encl = (Type) type.getUnderlyingType().getEnclosingType();
            while (encl != null
                    && encl.getKind() != TypeKind.NONE
                    && encl.getKind() != TypeKind.ERROR) {
                depth = depth.append(TypePathEntry.INNER_TYPE);
                encl = encl.getEnclosingType();
            }

            if (depth.nonEmpty()) {
                p.location = p.location.appendList(depth.toList());
            }
        }

        @Override
        public List<TypeCompound> visitIntersection(
                AnnotatedIntersectionType type, TypeAnnotationPosition tapos) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, List.nil());
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);

            int arg = 0;
            for (AnnotatedTypeMirror ta : type.directSuperTypes()) {
                TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTAPosition(tapos);
                newpos.location =
                        tapos.location.append(
                                new TypePathEntry(TypePathEntryKind.TYPE_ARGUMENT, arg));
                res = scanAndReduce(ta, newpos, res);
                ++arg;
            }
            visitedNodes.put(type, res);
            return res;
        }

        @Override
        public List<TypeCompound> visitUnion(
                AnnotatedUnionType type, TypeAnnotationPosition tapos) {
            // We should never need to write a union type, so raise an error.
            throw new BugInCF(
                    "TypesIntoElement: encountered union type: " + type + " at position: " + tapos);
        }

        @Override
        public List<TypeCompound> visitArray(
                AnnotatedArrayType type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);

            TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTAPosition(tapos);
            newpos.location = tapos.location.append(TypePathEntry.ARRAY);

            return reduce(super.visitArray(type, newpos), res);
        }

        @Override
        public List<TypeCompound> visitPrimitive(
                AnnotatedPrimitiveType type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);
            return res;
        }

        @Override
        public List<TypeCompound> visitTypeVariable(
                AnnotatedTypeVariable type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);
            // Do not call super. The bound will be visited separately.
            return res;
        }

        @Override
        public List<TypeCompound> visitWildcard(
                AnnotatedWildcardType type, TypeAnnotationPosition tapos) {
            if (this.visitedNodes.containsKey(type)) {
                return List.nil();
            }
            // Hack for termination, otherwise we'll visit one type too far (the same recursive
            // wildcard twice and generate extra type annos)
            visitedNodes.put(type, List.nil());
            List<Attribute.TypeCompound> res;

            // Note: By default, an Unbound wildcard will return true for both isExtendsBound and
            // isSuperBound
            if (((Type.WildcardType) type.getUnderlyingType()).isExtendsBound()) {
                res = directAnnotations(type.getSuperBound(), tapos);

                AnnotatedTypeMirror ext = type.getExtendsBound();
                if (ext != null) {
                    TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTAPosition(tapos);
                    newpos.location = tapos.location.append(TypePathEntry.WILDCARD);
                    res = scanAndReduce(ext, newpos, res);
                }

            } else {
                res = directAnnotations(type.getExtendsBound(), tapos);
                AnnotatedTypeMirror sup = type.getSuperBoundField();
                if (sup != null) {
                    TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTAPosition(tapos);
                    newpos.location = tapos.location.append(TypePathEntry.WILDCARD);
                    res = scanAndReduce(sup, newpos, res);
                }
            }
            visitedNodes.put(type, res);
            return res;
        }

        @Override
        public List<TypeCompound> visitNoType(AnnotatedNoType type, TypeAnnotationPosition tapos) {
            return List.nil();
        }

        @Override
        public List<TypeCompound> visitNull(AnnotatedNullType type, TypeAnnotationPosition tapos) {
            return List.nil();
        }
    }
}
