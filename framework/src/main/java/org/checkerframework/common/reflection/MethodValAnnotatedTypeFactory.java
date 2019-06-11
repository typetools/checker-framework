package org.checkerframework.common.reflection;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.common.reflection.qual.GetConstructor;
import org.checkerframework.common.reflection.qual.GetMethod;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.reflection.qual.MethodValBottom;
import org.checkerframework.common.reflection.qual.UnknownMethod;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class MethodValAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private final AnnotationMirror METHODVAL_BOTTOM =
            AnnotationBuilder.fromClass(elements, MethodValBottom.class);
    private final AnnotationMirror UNKNOWN_METHOD =
            AnnotationBuilder.fromClass(elements, UnknownMethod.class);

    private static final int UNKNOWN_PARAM_LENGTH = -1;

    public MethodValAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        if (this.getClass().equals(MethodValAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(
                Arrays.asList(MethodVal.class, MethodValBottom.class, UnknownMethod.class));
    }

    @Override
    protected void initializeReflectionResolution() {
        boolean debug = "debug".equals(checker.getOption("resolveReflection"));
        reflectionResolver = new DefaultReflectionResolver(checker, this, debug);
    }

    static List<MethodSignature> getListOfMethodSignatures(AnnotationMirror anno) {
        List<MethodSignature> list = new ArrayList<>();
        List<String> methodNames =
                AnnotationUtils.getElementValueArray(anno, "methodName", String.class, true);
        List<String> classNames =
                AnnotationUtils.getElementValueArray(anno, "className", String.class, true);
        List<Integer> params =
                AnnotationUtils.getElementValueArray(anno, "params", Integer.class, true);
        for (int i = 0; i < methodNames.size(); i++) {
            list.add(new MethodSignature(classNames.get(i), methodNames.get(i), params.get(i)));
        }
        return list;
    }

    private AnnotationMirror createMethodVal(Set<MethodSignature> sigs) {
        List<String> classNames = new ArrayList<>();
        List<String> methodNames = new ArrayList<>();
        List<Integer> params = new ArrayList<>();
        for (MethodSignature sig : sigs) {
            classNames.add(sig.className);
            methodNames.add(sig.methodName);
            params.add(sig.params);
        }
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, MethodVal.class.getCanonicalName());
        builder.setValue("className", classNames);
        builder.setValue("methodName", methodNames);
        builder.setValue("params", params);
        return builder.build();
    }
    /**
     * Returns a list of class names for the given tree using the Class Val Checker.
     *
     * @param tree ExpressionTree whose class names are requested
     * @param mustBeExact whether @ClassBound may be used
     * @return list of class names or the empty list if no class names were found
     */
    private List<String> getClassNamesFromClassValChecker(
            ExpressionTree tree, boolean mustBeExact) {
        ClassValAnnotatedTypeFactory classValATF =
                getTypeFactoryOfSubchecker(ClassValChecker.class);
        AnnotatedTypeMirror classAnno = classValATF.getAnnotatedType(tree);
        List<String> classNames = new ArrayList<>();
        AnnotationMirror annotation = classAnno.getAnnotation(ClassVal.class);
        if (annotation != null) {
            classNames =
                    AnnotationUtils.getElementValueArray(annotation, "value", String.class, true);
        } else if (!mustBeExact) {
            // Could be ClassBound instead of ClassVal
            annotation = classAnno.getAnnotation(ClassBound.class);
            if (annotation != null) {
                classNames =
                        AnnotationUtils.getElementValueArray(
                                annotation, "value", String.class, true);
            }
        }
        return classNames;
    }
    /**
     * Returns the string values for the argument passed. The String Values are estimated using the
     * Value Checker.
     *
     * @param arg ExpressionTree whose string values are sought
     * @return string values of arg or the empty list if no values were found
     */
    private List<String> getMethodNamesFromStringArg(ExpressionTree arg) {
        List<String> methodNames = new ArrayList<>();
        ValueAnnotatedTypeFactory valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
        AnnotatedTypeMirror valueAnno = valueATF.getAnnotatedType(arg);
        AnnotationMirror annotation = valueAnno.getAnnotation(StringVal.class);
        if (annotation != null) {
            methodNames =
                    AnnotationUtils.getElementValueArray(annotation, "value", String.class, true);
        }
        return methodNames;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MethodValQualifierHierarchy(factory, METHODVAL_BOTTOM);
    }

    protected class MethodValQualifierHierarchy extends MultiGraphQualifierHierarchy {

        protected MethodValQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory, AnnotationMirror bottom) {
            super(factory, bottom);
        }

        /*
         * Determines the least upper bound of a1 and a2. If both are MethodVal
         * annotations, then the least upper bound is the result of
         * concatenating all value lists of a1 and a2.
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (!AnnotationUtils.areSameByName(getTopAnnotation(a1), getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            } else if (AnnotationUtils.areSameByName(a1, a2)) {
                List<MethodSignature> a1Sigs = getListOfMethodSignatures(a1);
                List<MethodSignature> a2Sigs = getListOfMethodSignatures(a2);

                Set<MethodSignature> lubSigs = new HashSet<>(a1Sigs);
                lubSigs.addAll(a2Sigs);

                AnnotationMirror result = createMethodVal(lubSigs);
                return result;
            }
            return null;
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSame(subAnno, superAnno)
                    || AnnotationUtils.areSameByClass(superAnno, UnknownMethod.class)
                    || AnnotationUtils.areSameByClass(subAnno, MethodValBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, UnknownMethod.class)
                    || AnnotationUtils.areSameByClass(superAnno, MethodValBottom.class)) {
                return false;
            }
            assert AnnotationUtils.areSameByClass(subAnno, MethodVal.class)
                            && AnnotationUtils.areSameByClass(superAnno, MethodVal.class)
                    : "Unexpected annotation in MethodVal";
            List<MethodSignature> subSignatures = getListOfMethodSignatures(subAnno);
            List<MethodSignature> superSignatures = getListOfMethodSignatures(superAnno);
            for (MethodSignature sig : subSignatures) {
                if (!superSignatures.contains(sig)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new MethodValTreeAnnotator(this), super.createTreeAnnotator());
    }

    /** TreeAnnotator with the visitMethodInvocation method overridden. */
    protected class MethodValTreeAnnotator extends TreeAnnotator {

        protected MethodValTreeAnnotator(MethodValAnnotatedTypeFactory factory) {
            super(factory);
        }

        /*
         * Special handling of getMethod and getDeclaredMethod calls. Attempts
         * to get the annotation on the Class object receiver to get the
         * className, the annotation on the String argument to get the
         * methodName, and the parameters argument to get the param, and then
         * builds a new MethodVal annotation from these
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {

            List<String> methodNames;
            List<Integer> params;
            List<String> classNames;
            if (isGetConstructorMethodInvocation(tree)) {
                // method name for constructors is always <init>
                methodNames = Arrays.asList(ReflectionResolver.INIT);
                params = getConstructorParamsLen(tree.getArguments());
                classNames =
                        getClassNamesFromClassValChecker(TreeUtils.getReceiverTree(tree), true);

            } else if (isGetMethodMethodInvocation(tree)) {
                ExpressionTree methodNameArg = tree.getArguments().get(0);
                methodNames = getMethodNamesFromStringArg(methodNameArg);
                params = getMethodParamsLen(tree.getArguments());
                classNames =
                        getClassNamesFromClassValChecker(TreeUtils.getReceiverTree(tree), false);
            } else {
                // Not a covered method invocation
                return null;
            }

            // Create MethodVal
            if (methodNames.isEmpty() || classNames.isEmpty()) {
                // No method name or classname is found, it could be any
                // class or method, so, return @UnknownMethod.
                type.replaceAnnotation(UNKNOWN_METHOD);
                return null;
            }

            Set<MethodSignature> methodSigs = new HashSet<>();

            // The possible method signatures are the Cartesian product of all
            // found class, method, and parameter lengths
            for (String methodName : methodNames) {
                for (String className : classNames) {
                    for (Integer param : params) {
                        methodSigs.add(new MethodSignature(className, methodName, param));
                    }
                }
            }

            AnnotationMirror newQual = createMethodVal(methodSigs);
            type.replaceAnnotation(newQual);
            return null;
        }

        /**
         * Returns true if the method being invoked is annotated with @GetConstructor. An example of
         * such a method is Class.getConstructor.
         */
        private boolean isGetConstructorMethodInvocation(MethodInvocationTree tree) {
            return getDeclAnnotation(TreeUtils.elementFromTree(tree), GetConstructor.class) != null;
        }

        /**
         * Returns true if the method being invoked is annotated with @GetMethod. An example of such
         * a method is Class.getMethod.
         */
        private boolean isGetMethodMethodInvocation(MethodInvocationTree tree) {
            return getDeclAnnotation(TreeUtils.elementFromTree(tree), GetMethod.class) != null;
        }

        private List<Integer> getMethodParamsLen(List<? extends ExpressionTree> args) {
            assert !args.isEmpty() : "getMethod must have at least one parameter";

            // Number of parameters in the created method object
            int numParams = args.size() - 1;
            if (numParams == 1) {
                return getNumberOfParameterOneArg(args.get(1));
            }
            return Collections.singletonList(numParams);
        }

        private List<Integer> getConstructorParamsLen(List<? extends ExpressionTree> args) {
            // Number of parameters in the created method object
            int numParams = args.size();
            if (numParams == 1) {
                return getNumberOfParameterOneArg(args.get(0));
            }
            return Collections.singletonList(numParams);
        }

        /**
         * if getMethod(Object receiver, Object... params) or getConstrutor(Object... params) have
         * one argument for params, then the number of parameters in the underlying method or
         * constructor must be:
         *
         * <ul>
         *   <li>0: if the argument is null
         *   <li>x: if the argument is an array with @ArrayLen(x)
         *   <li>UNKNOWN_PARAM_LENGTH: if the argument is an array with @UnknownVal
         *   <li>1: otherwise
         * </ul>
         */
        private List<Integer> getNumberOfParameterOneArg(ExpressionTree argument) {
            AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(argument);
            switch (atm.getKind()) {
                case ARRAY:
                    ValueAnnotatedTypeFactory valueATF =
                            getTypeFactoryOfSubchecker(ValueChecker.class);
                    AnnotatedTypeMirror valueAnno = valueATF.getAnnotatedType(argument);
                    if (valueAnno.getAnnotation(ArrayLen.class) != null) {
                        AnnotationMirror annotation = valueAnno.getAnnotation(ArrayLen.class);
                        return AnnotationUtils.getElementValueArray(
                                annotation, "value", Integer.class, true);
                    } else if (valueAnno.getAnnotation(BottomVal.class) != null) {
                        // happens in this case: (Class[]) null
                        return Collections.singletonList(0);
                    }
                    // the argument is an array with unknown array length
                    return Collections.singletonList(UNKNOWN_PARAM_LENGTH);
                case NULL:
                    // null is treated as the empty list of parameters, so size
                    // is 0
                    return Collections.singletonList(0);
                default:
                    // The argument is not an array or null,
                    // so it must be a class.
                    return Collections.singletonList(1);
            }
        }
    }
}
/**
 * An object that represents a the tuple that identifies a method signature: (fully qualified class
 * name, method name, number of parameters).
 */
class MethodSignature {
    String className;
    String methodName;
    int params;

    public MethodSignature(String className, String methodName, int params) {
        this.className = className;
        this.methodName = methodName;
        this.params = params;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, params);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MethodSignature other = (MethodSignature) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (methodName == null) {
            if (other.methodName != null) {
                return false;
            }
        } else if (!methodName.equals(other.methodName)) {
            return false;
        }
        if (params != other.params) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MethodSignature [className="
                + className
                + ", methodName="
                + methodName
                + ", params="
                + params
                + "]";
    }
}
