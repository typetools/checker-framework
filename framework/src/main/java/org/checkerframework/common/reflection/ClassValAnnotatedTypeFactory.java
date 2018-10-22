package org.checkerframework.common.reflection;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.UnionClassType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.common.reflection.qual.ClassValBottom;
import org.checkerframework.common.reflection.qual.ForName;
import org.checkerframework.common.reflection.qual.GetClass;
import org.checkerframework.common.reflection.qual.UnknownClass;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ClassValAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror CLASSVAL_TOP =
            AnnotationBuilder.fromClass(elements, UnknownClass.class);

    public ClassValAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        if (this.getClass().equals(ClassValAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(
                Arrays.asList(
                        UnknownClass.class,
                        ClassVal.class,
                        ClassBound.class,
                        ClassValBottom.class));
    }

    private AnnotationMirror createClassVal(List<String> values) {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, ClassVal.class.getCanonicalName());
        builder.setValue("value", values);
        return builder.build();
    }

    private AnnotationMirror createClassBound(List<String> values) {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, ClassBound.class.getCanonicalName());
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Returns the list of classnames from {@code @ClassBound} or {@code @ClassVal} if anno is
     * {@code @ClassBound} or {@code @ClassVal}, otherwise returns an empty list.
     *
     * @param anno any AnnotationMirror
     * @return list of classnames in anno
     */
    public static List<String> getClassNamesFromAnnotation(AnnotationMirror anno) {
        if (AnnotationUtils.areSameByClass(anno, ClassBound.class)
                || AnnotationUtils.areSameByClass(anno, ClassVal.class)) {
            return AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
        }
        return new ArrayList<>();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new ClassValQualifierHierarchy(factory);
    }

    /** The qualifier hierarchy for the ClassVal type system. */
    protected class ClassValQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public ClassValQualifierHierarchy(MultiGraphFactory f) {
            super(f);
        }

        /*
         * Determines the least upper bound of a1 and a2. If both are ClassVal
         * annotations, then the least upper bound is the set of elements
         * obtained by combining the values of both annotations.
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (!AnnotationUtils.areSameIgnoringValues(
                    getTopAnnotation(a1), getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            } else {
                List<String> a1ClassNames = getClassNamesFromAnnotation(a1);
                List<String> a2ClassNames = getClassNamesFromAnnotation(a2);
                Set<String> lubClassNames = new TreeSet<>();
                lubClassNames.addAll(a1ClassNames);
                lubClassNames.addAll(a2ClassNames);

                // If either annotation is a ClassBound, the lub must also be a class bound.
                if (AnnotationUtils.areSameByClass(a1, ClassBound.class)
                        || AnnotationUtils.areSameByClass(a2, ClassBound.class)) {
                    return createClassBound(new ArrayList<>(lubClassNames));
                } else {
                    return createClassVal(new ArrayList<>(lubClassNames));
                }
            }
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (!AnnotationUtils.areSameIgnoringValues(
                    getTopAnnotation(a1), getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else {
                List<String> a1ClassNames = getClassNamesFromAnnotation(a1);
                List<String> a2ClassNames = getClassNamesFromAnnotation(a2);
                Set<String> glbClassNames = new TreeSet<>();
                glbClassNames.addAll(a1ClassNames);
                glbClassNames.retainAll(a2ClassNames);

                // If either annotation is a ClassVal, the glb must also be a ClassVal.
                // For example:
                // GLB( @ClassVal(a,b), @ClassBound(a,c)) is @ClassVal(a)
                // because @ClassBound(a) is not a subtype of @ClassVal(a,b)
                if (AnnotationUtils.areSameByClass(a1, ClassVal.class)
                        || AnnotationUtils.areSameByClass(a2, ClassVal.class)) {
                    return createClassVal(new ArrayList<>(glbClassNames));
                } else {
                    return createClassBound(new ArrayList<>(glbClassNames));
                }
            }
        }

        /*
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are ClassVal. In this case, rhs is
         * a subtype of lhs iff lhs contains at least every element of rhs.
         */
        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSame(subAnno, superAnno)
                    || AnnotationUtils.areSameByClass(superAnno, UnknownClass.class)
                    || AnnotationUtils.areSameByClass(subAnno, ClassValBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, UnknownClass.class)
                    || AnnotationUtils.areSameByClass(superAnno, ClassValBottom.class)) {
                return false;
            }
            if (AnnotationUtils.areSameByClass(superAnno, ClassVal.class)
                    && AnnotationUtils.areSameByClass(subAnno, ClassBound.class)) {
                return false;
            }

            // if super: ClassVal && sub is ClassVal
            // if super: ClassBound && (sub is ClassBound or ClassVal)

            List<String> supValues = getClassNamesFromAnnotation(superAnno);
            List<String> subValues = getClassNamesFromAnnotation(subAnno);

            return supValues.containsAll(subValues);
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new ClassValTreeAnnotator(this), super.createTreeAnnotator());
    }

    /**
     * Implements the following type inference rules.
     *
     * <pre>
     * C.class:             @ClassVal(fully qualified name of C)
     * Class.forName(name): @ClassVal("name")
     * exp.getClass():      @ClassBound(fully qualified classname of exp)
     * </pre>
     */
    protected class ClassValTreeAnnotator extends TreeAnnotator {

        protected ClassValTreeAnnotator(ClassValAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isClassLiteral(tree)) {
                // Create annotations for Class literals
                // C.class: @ClassVal(fully qualified name of C)
                ExpressionTree etree = tree.getExpression();
                Type classType = (Type) TreeUtils.typeOf(etree);
                String name = getClassNameFromType(classType);
                if (name != null) {
                    AnnotationMirror newQual = createClassVal(Arrays.asList(name));
                    type.replaceAnnotation(newQual);
                }
            }
            return null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {

            if (isForNameMethodInovaction(tree)) {
                // Class.forName(name): @ClassVal("name")
                ExpressionTree arg = tree.getArguments().get(0);
                List<String> classNames = getStringValues(arg);
                if (classNames != null) {
                    AnnotationMirror newQual = createClassVal(classNames);
                    type.replaceAnnotation(newQual);
                }
            } else if (isGetClassMethodInovaction(tree)) {
                // exp.getClass(): @ClassBound(fully qualified class name of exp)
                Type clType;
                if (TreeUtils.getReceiverTree(tree) != null) {
                    clType = (Type) TreeUtils.typeOf(TreeUtils.getReceiverTree(tree));
                } else { // receiver is null, so it is implicitly "this"
                    ClassTree classTree = TreeUtils.enclosingClass(getPath(tree));
                    clType = (Type) TreeUtils.typeOf(classTree);
                }
                String className = getClassNameFromType(clType);
                AnnotationMirror newQual = createClassBound(Arrays.asList(className));
                type.replaceAnnotation(newQual);
            }
            return null;
        }

        private boolean isForNameMethodInovaction(MethodInvocationTree tree) {
            return getDeclAnnotation(TreeUtils.elementFromTree(tree), ForName.class) != null;
        }

        private boolean isGetClassMethodInovaction(MethodInvocationTree tree) {
            return getDeclAnnotation(TreeUtils.elementFromTree(tree), GetClass.class) != null;
        }

        private List<String> getStringValues(ExpressionTree arg) {
            ValueAnnotatedTypeFactory valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
            AnnotationMirror annotation = valueATF.getAnnotationMirror(arg, StringVal.class);
            if (annotation == null) {
                return null;
            }
            return AnnotationUtils.getElementValueArray(annotation, "value", String.class, true);
        }

        /**
         * Return String representation of class name. This will not return the correct name for
         * anonymous classes.
         */
        private String getClassNameFromType(Type classType) {
            switch (classType.getKind()) {
                case ARRAY:
                    String array = "";
                    while (classType.getKind() == TypeKind.ARRAY) {
                        classType = ((ArrayType) classType).getComponentType();
                        array += "[]";
                    }
                    return getClassNameFromType(classType) + array;
                case DECLARED:
                    StringBuilder className =
                            new StringBuilder(
                                    TypesUtils.getQualifiedName((DeclaredType) classType)
                                            .toString());
                    if (classType.getEnclosingType() != null) {
                        while (classType.getEnclosingType().getKind() != TypeKind.NONE) {
                            classType = classType.getEnclosingType();
                            int last = className.lastIndexOf(".");
                            if (last > -1) {
                                className.replace(last, last + 1, "$");
                            }
                        }
                    }
                    return className.toString();
                case INTERSECTION:
                    // This could be more precise
                    return "java.lang.Object";
                case NULL:
                    return "java.lang.Object";
                case UNION:
                    classType = ((UnionClassType) classType).getLub();
                    return getClassNameFromType(classType);
                case TYPEVAR:
                case WILDCARD:
                    classType = classType.getUpperBound();
                    return getClassNameFromType(classType);
                case INT:
                    return int.class.getCanonicalName();
                case LONG:
                    return long.class.getCanonicalName();
                case SHORT:
                    return short.class.getCanonicalName();
                case BYTE:
                    return byte.class.getCanonicalName();
                case CHAR:
                    return char.class.getCanonicalName();
                case DOUBLE:
                    return double.class.getCanonicalName();
                case FLOAT:
                    return float.class.getCanonicalName();
                case BOOLEAN:
                    return boolean.class.getCanonicalName();
                case VOID:
                    return "void";
                default:
                    throw new BugInCF(
                            "ClassValAnnotatedTypeFactory.getClassname: did not expect "
                                    + classType.getKind());
            }
        }
    }
}
