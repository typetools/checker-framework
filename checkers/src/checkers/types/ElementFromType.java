package checkers.types;

import javacutils.ErrorReporter;
import javacutils.TreeUtils;
import javacutils.TypeAnnotationUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNoType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNullType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.visitors.AnnotatedTypeScanner;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;


/**
 * A helper class that puts the annotations from an AnnotatedTypeMirror
 * back into an Element, so that they get stored in the bytecode by the compiler.
 *
 * This has kind-of the symmetric function to {@code TypeFromElement}.
 * It doesn't produce a new Element, it modifies an existing Element.
 *
 * This class deals with javac internals and liberally imports such classes.
 */
public class ElementFromType {

    public static void store(ProcessingEnvironment processingEnv, AnnotatedTypeFactory atypeFactory, ClassTree tree) {
        Symbol.ClassSymbol csym = (Symbol.ClassSymbol) TreeUtils.elementFromDeclaration(tree);
        Types types = processingEnv.getTypeUtils();

        storeTypeParameters(processingEnv, types, atypeFactory, tree.getTypeParameters(), csym);

        /* TODO: storing extends/implements types results in
         * a strange error e.g. from the Nullness Checker.
         * I think somewhere we take the annotations on extends/implements as
         * the receiver annotation on a constructor, breaking logic there.
         * I assume that the problem is the default that we use for these locations.
         * Once we've decided the defaulting, enable this.
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

    private static void storeMethod(ProcessingEnvironment processingEnv, Types types,
            AnnotatedTypeFactory atypeFactory, MethodTree meth){
        AnnotatedExecutableType mtype = atypeFactory.getAnnotatedType(meth);
        MethodSymbol sym = (MethodSymbol) TreeUtils.elementFromDeclaration(meth);
        TypeAnnotationPosition tapos = new TypeAnnotationPosition();
        List<Attribute.TypeCompound> tcs = List.nil();

        storeTypeParameters(processingEnv, types, atypeFactory, meth.getTypeParameters(), sym);

        {
            // return type
            JCTree ret = ((JCTree.JCMethodDecl) meth).getReturnType();
            if (ret != null) {
                tapos.type = TargetType.METHOD_RETURN;
                tapos.pos = ret.pos;
                tcs = tcs.appendList(generateTypeCompounds(processingEnv, mtype.getReturnType(), tapos));
            }
        }
        {
            // receiver
            JCTree recv = ((JCTree.JCMethodDecl) meth).getReceiverParameter();
            if (recv != null) {
                tapos.type = TargetType.METHOD_RECEIVER;
                tapos.pos = recv.pos;
                tcs = tcs.appendList(generateTypeCompounds(processingEnv, mtype.getReceiverType(), tapos));
            }
        }
        {
            // parameters
            int pidx = 0;
            java.util.List<AnnotatedTypeMirror> ptypes = mtype.getParameterTypes();
            for (JCTree param : ((JCTree.JCMethodDecl) meth).getParameters()) {
                tapos.type = TargetType.METHOD_FORMAL_PARAMETER;
                tapos.pos = param.pos;
                tapos.parameter_index = pidx;
                tcs = tcs.appendList(generateTypeCompounds(processingEnv, ptypes.get(pidx), tapos));
                ++pidx;
            }
        }
        {
            // throws clauses
            int tidx = 0;
            java.util.List<AnnotatedTypeMirror> ttypes = mtype.getThrownTypes();
            for (JCTree thr : ((JCTree.JCMethodDecl) meth).getThrows()) {
                tapos.type = TargetType.THROWS;
                tapos.pos = thr.pos;
                tapos.type_index = tidx;
                tcs = tcs.appendList(generateTypeCompounds(processingEnv, ttypes.get(tidx), tapos));
                ++tidx;
            }
        }

        addUniqueTypeCompounds(types, sym, tcs);
    }

    private static void storeVariable(ProcessingEnvironment processingEnv, Types types,
            AnnotatedTypeFactory atypeFactory, VariableTree var){
        VarSymbol sym = (VarSymbol) TreeUtils.elementFromDeclaration(var);
        AnnotatedTypeMirror type;
        if (atypeFactory instanceof GenericAnnotatedTypeFactory) {
            // TODO: this is rather ugly: we do not want refinement from the
            // initializer of the field. We need a general way to get
            // the "defaulted" type of a variable.
            type = ((GenericAnnotatedTypeFactory<?, ?, ?, ?>)atypeFactory).getDefaultedAnnotatedType(var, var.getInitializer());
        } else {
            type = atypeFactory.getAnnotatedType(var);
        }

        TypeAnnotationPosition tapos = new TypeAnnotationPosition();
        tapos.type = TargetType.FIELD;
        tapos.pos = ((JCTree)var).pos;

        List<Attribute.TypeCompound> tcs;
        tcs = generateTypeCompounds(processingEnv, type, tapos);
        addUniqueTypeCompounds(types, sym, tcs);
    }

    @SuppressWarnings("unused") // TODO: use from store().
    private static void storeClassExtends(ProcessingEnvironment processingEnv, Types types,
            AnnotatedTypeFactory atypeFactory, Tree ext, Symbol.ClassSymbol csym,
            int implidx){

        AnnotatedTypeMirror type;
        if (ext == null) {
            // The implicit superclass is always java.lang.Object.
            // TODO: is this a good way to get the type?
            type = atypeFactory.fromElement(csym.getSuperclass().asElement());
        } else {
            type = atypeFactory.getAnnotatedType(ext);
        }

        TypeAnnotationPosition tapos = new TypeAnnotationPosition();
        tapos.type = TargetType.CLASS_EXTENDS;
        tapos.type_index = implidx;
        if (ext != null) {
            tapos.pos = ((JCTree)ext).pos;
        }

        List<Attribute.TypeCompound> tcs;
        tcs = generateTypeCompounds(processingEnv, type, tapos);
        addUniqueTypeCompounds(types, csym, tcs);
    }

    private static void storeTypeParameters(ProcessingEnvironment processingEnv, Types types,
            AnnotatedTypeFactory atypeFactory,
            java.util.List<? extends TypeParameterTree> tps, Symbol sym) {
        boolean isClass = sym.getKind().isClass();
        List<Attribute.TypeCompound> tcs = List.nil();

        int tpidx = 0;
        for (TypeParameterTree tp : tps) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedTypeFromTypeTree(tp);
            // System.out.println("The Type for type parameter " + tp + " is " + type);

            TypeAnnotationPosition tapos = new TypeAnnotationPosition();
            tapos.type = isClass ? TargetType.CLASS_TYPE_PARAMETER : TargetType.METHOD_TYPE_PARAMETER;
            tapos.parameter_index = tpidx;
            // We use the type parameter pos also for the bounds;
            // the bounds may not be explicit and we couldn't look up separate pos.
            tapos.pos = ((JCTree)tp).pos;

            tcs = tcs.appendList(generateTypeCompounds(processingEnv, type, tapos));

            tapos.type = isClass ? TargetType.CLASS_TYPE_PARAMETER_BOUND : TargetType.METHOD_TYPE_PARAMETER_BOUND;
            AnnotatedTypeMirror tpbound = ((AnnotatedTypeMirror.AnnotatedTypeVariable) type).getUpperBound();
            java.util.List<? extends AnnotatedTypeMirror> bounds;
            if (tpbound.getKind() == TypeKind.INTERSECTION) {
                bounds = ((AnnotatedTypeMirror.AnnotatedIntersectionType) tpbound).directSuperTypes();
            } else {
                bounds = List.of(tpbound);
            }
            int bndidx = 0;
            for (AnnotatedTypeMirror bound : bounds) {
                if (bndidx == 0 && ((Type)bound.getUnderlyingType()).isInterface()) {
                    // If the first bound is an interface, there is an implicit java.lang.Object
                    ++bndidx;
                }
                tapos.bound_index = bndidx;
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
            if (!TypeAnnotationUtils.isTypeCompoundContained(types, raw, tc)) {
                res = res.append(tc);
            }
        }
        // That method only uses reference equality. isTypeCompoundContained does a deep comparison.
        sym.appendUniqueTypeAttributes(res);
    }

    // return List.nil() not null if there are no TypeCompounds to return.
    public static List<Attribute.TypeCompound> generateTypeCompounds(ProcessingEnvironment processingEnv,
            AnnotatedTypeMirror type, TypeAnnotationPosition tapos) {
        // Generate a local copy, and whenever you need to modify stuff, e.g. type arguments.
        tapos = TypeAnnotationUtils.copyTypeAnnotationPosition(tapos);
        return new TCConvert(processingEnv).scan(type, tapos);
    }

    private static class TCConvert extends AnnotatedTypeScanner<List<Attribute.TypeCompound>, TypeAnnotationPosition> {

        private final ProcessingEnvironment processingEnv;

        TCConvert(ProcessingEnvironment processingEnv) {
            this.processingEnv = processingEnv;
        }

        @Override
        public List<TypeCompound> scan(AnnotatedTypeMirror type,
                TypeAnnotationPosition pos) {
            if (pos == null) {
                ErrorReporter.errorAbort("ElementFromType: invalid usage, null pos with type: " + type);
            }
            if (type == null) {
                return List.nil();
            }
            List<TypeCompound> res = super.scan(type, pos);
            return res;
        }

        @Override
        protected List<TypeCompound> scan(Iterable<? extends AnnotatedTypeMirror> types, TypeAnnotationPosition pos) {
            if (types == null)
                return List.nil();
            return super.scan(types, pos);
        }

        @Override
        public List<TypeCompound> reduce(List<TypeCompound> r1,
                List<TypeCompound> r2) {
            if (r1 == null)
                return r2;
            if (r2 == null)
                return r1;
            return r1.appendList(r2);
        }

        List<TypeCompound> directAnnotations(AnnotatedTypeMirror type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res = List.nil();

            for (AnnotationMirror am : type.getAnnotations()) {
                if (am instanceof Attribute.TypeCompound) {
                    // If it is a TypeCompound it was already present in source (right?),
                    // so there is nothing to do.
                    // System.out.println("  found TypeComound: " + am + " pos: " + ((Attribute.TypeCompound)am).position);
                } else {
                    Attribute.TypeCompound tc = TypeAnnotationUtils.createTypeCompoundFromAnnotationMirror(processingEnv, am, tapos);
                    res = res.prepend(tc);
                }
            }
            return res;
        }

        @Override
        public List<TypeCompound> visitDeclared(AnnotatedDeclaredType type,
                TypeAnnotationPosition tapos) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            // Hack for termination
            visitedNodes.put(type, List.<TypeCompound>nil());
            List<Attribute.TypeCompound> res;

            res = directAnnotations(type, tapos);

            int arg = 0;
            for (AnnotatedTypeMirror ta : type.getTypeArguments()) {
                TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTypeAnnotationPosition(tapos);
                newpos.location = tapos.location.append(new TypePathEntry(TypePathEntryKind.TYPE_ARGUMENT, arg));
                res = scanAndReduce(ta, newpos, res);
                ++ arg;
            }

            AnnotatedTypeMirror encl = type.getEnclosingType();
            if (encl != null && encl.getKind() != TypeKind.NONE) {
                TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTypeAnnotationPosition(tapos);
                newpos.location = tapos.location.append(TypePathEntry.INNER_TYPE);
                res = scanAndReduce(encl, newpos, res);
            }
            visitedNodes.put(type, res);
            return res;
        }

        @Override
        public List<TypeCompound> visitArray(AnnotatedArrayType type,
                TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);

            TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTypeAnnotationPosition(tapos);
            newpos.location = tapos.location.append(TypePathEntry.ARRAY);

            return reduce(super.visitArray(type, newpos), res);
        }

        @Override
        public List<TypeCompound> visitPrimitive(AnnotatedPrimitiveType type,
                TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);
            return res;
        }

        @Override
        public List<TypeCompound> visitTypeVariable(AnnotatedTypeVariable type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);
            // Do not call super. The bound will be visited separately.
            return res;
        }

        @Override
        public List<TypeCompound> visitWildcard(AnnotatedWildcardType type, TypeAnnotationPosition tapos) {
            List<Attribute.TypeCompound> res;
            res = directAnnotations(type, tapos);
            if (((Type.WildcardType)type.getUnderlyingType()).isExtendsBound()) {
                AnnotatedTypeMirror ext = type.getExtendsBoundField();
                if (ext != null) {
                    TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTypeAnnotationPosition(tapos);
                    newpos.location = tapos.location.append(TypePathEntry.WILDCARD);
                    res = scanAndReduce(ext, newpos, res);
                }
            } else {
                AnnotatedTypeMirror sup = type.getSuperBoundField();
                if (sup != null) {
                    TypeAnnotationPosition newpos = TypeAnnotationUtils.copyTypeAnnotationPosition(tapos);
                    newpos.location = tapos.location.append(TypePathEntry.WILDCARD);
                    res = scanAndReduce(sup, newpos, res);
                }
            }
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