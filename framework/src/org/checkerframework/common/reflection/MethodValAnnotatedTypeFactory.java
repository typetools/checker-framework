package org.checkerframework.common.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.reflection.qual.MethodValBottom;
import org.checkerframework.common.reflection.qual.UnknownMethod;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * AnnotatedTypeFactory for the Method Value type system. This factory requires
 * an {@link AnnotationProvider} that provides {@link ClassVal},
 * {@link StringVal}, and {@link ArrayLen} annotations.
 *
 * @author plvines
 * @author rjust
 */
public class MethodValAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private final AnnotationMirror METHODVAL = AnnotationUtils.fromClass(
            elements, MethodVal.class);
    private final AnnotationMirror METHODVAL_BOTTOM = AnnotationUtils
            .fromClass(elements, MethodValBottom.class);
    private final AnnotationMirror UNKNOWN_METHOD = AnnotationUtils.fromClass(
            elements, UnknownMethod.class);

    // variable for comparing a MethodInvocationTree against to determine if it
    // is pertinent to this
    private final ExecutableElement getMethod = TreeUtils.getMethod(
            "java.lang.Class", "getMethod", 2, processingEnv);
    private final ExecutableElement getDeclaredMethod = TreeUtils.getMethod(
            "java.lang.Class", "getDeclaredMethod", 2, processingEnv);
    private final ExecutableElement getConstructor = TreeUtils.getMethod(
            "java.lang.Class", "getConstructor", 1, processingEnv);

    private final AnnotationProvider annotationProvider;

    /**
     * Constructor. Initializes all the AnnotationMirror and Executable Element
     * variables, sets the default annotation.
     *
     * @param checker
     *            The checker used with this AnnotatedTypeFactory
     * @param annotationProvider
     *            The AnnotationProvider providing the necessary ClassVal,
     *            StringVal, and ArrayLen annotations.
     *
     */
    public MethodValAnnotatedTypeFactory(BaseTypeChecker checker,
            AnnotationProvider annotationProvider) {
        super(checker);
        this.annotationProvider = annotationProvider;
        // TODO This hack is error prone as it is not obvious whether and how
        // postInit has to be used in sub classes!
        if (this.getClass().equals(MethodValAnnotatedTypeFactory.class)) {
            this.postInit();
        }
        this.defaults.addAbsoluteDefault(
                AnnotationUtils.fromClass(elements, UnknownMethod.class),
                DefaultLocation.ALL);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> supported = new HashSet<>();
        supported.add(MethodVal.class);
        supported.add(MethodValBottom.class);
        supported.add(UnknownMethod.class);

        return supported;
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        // The super implementation uses the name of the checker
        // to reflectively create a transfer with the checker name followed
        // by Transfer. Since this factory is intended to be used with
        // any checker, explicitly create the default CFTransfer
        return new CFTransfer(analysis);
    }

    /**
     * Constructs a MethodVal annotation by adding all the elements in
     * classNames, methodNames, and params to a new MethodVal annotation and
     * returning it
     *
     * @param classNames
     *            The list of possible class names.
     * @param methodNames
     *            The list of possible method names.
     * @param params
     *            The list of possible numbers of parameters.
     *
     * @return A MethodVal annotation with all provided values.
     */
    private AnnotationMirror createMethodVal(List<String> classNames,
            List<String> methodNames, List<Integer> params) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                MethodVal.class.getCanonicalName());
        builder.setValue("className", classNames);
        builder.setValue("methodName", methodNames);
        builder.setValue("params", params);
        return builder.build();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = createQualifierHierarchyFactory();
        factory.addQualifier(METHODVAL_BOTTOM);
        factory.addQualifier(METHODVAL);
        factory.addQualifier(UNKNOWN_METHOD);
        factory.addSubtype(METHODVAL, UNKNOWN_METHOD);
        factory.addSubtype(METHODVAL_BOTTOM, METHODVAL);

        return new MethodValQualifierHierarchy(factory, METHODVAL_BOTTOM);
    }

    /**
     * The qualifier hierarchy for the MethodVal type system
     */
    protected class MethodValQualifierHierarchy extends
            MultiGraphQualifierHierarchy {

        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         * @param bottom
         *            the bottom annotation in the constructed hierarchy
         */
        protected MethodValQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory,
                AnnotationMirror bottom) {
            super(factory, bottom);
        }

        /*
         * Determines the least upper bound of a1 and a2. If both are MethodVal
         * annotations, then the least upper bound is the result of
         * concatenating all value lists of a1 and a2.
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            // CLASSVAL HANDLING
            if (!AnnotationUtils.areSameIgnoringValues(getTopAnnotation(a1),
                    getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            } else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                List<String> a1MethodNames = AnnotationUtils
                        .getElementValueArray(a1, "methodName", String.class,
                                true);
                List<String> a2MethodNames = AnnotationUtils
                        .getElementValueArray(a2, "methodName", String.class,
                                true);
                ArrayList<String> newMethodNames = new ArrayList<String>();
                newMethodNames.addAll(a1MethodNames);

                List<String> a1ClassNames = AnnotationUtils
                        .getElementValueArray(a1, "className", String.class,
                                true);
                List<String> a2ClassNames = AnnotationUtils
                        .getElementValueArray(a2, "className", String.class,
                                true);
                ArrayList<String> newClassNames = new ArrayList<String>();
                newClassNames.addAll(a1ClassNames);

                List<Integer> a1Params = AnnotationUtils.getElementValueArray(
                        a1, "params", Integer.class, true);
                List<Integer> a2Params = AnnotationUtils.getElementValueArray(
                        a2, "params", Integer.class, true);
                ArrayList<Integer> newParams = new ArrayList<Integer>();
                newParams.addAll(a1Params);

                // Do not need to do any sort of cartesian product
                // of the two annotations here
                for (int i = 0; i < a2MethodNames.size(); i++) {
                    // If this method was only in one of the
                    // annotations, then add it
                    if (!newMethodNames.contains(a2MethodNames.get(i))) {
                        newMethodNames.add(a2MethodNames.get(i));
                        newClassNames.add(a2ClassNames.get(i));
                        newParams.add(a2Params.get(i));
                    }
                    // If this method was in both, but it had
                    // different classes, add it again
                    else if (!newClassNames.contains(a2ClassNames.get(i))) {
                        newMethodNames.add(a2MethodNames.get(i));
                        newClassNames.add(a2ClassNames.get(i));
                        newParams.add(a2Params.get(i));
                    }
                    // If this method was in both with the same
                    // class, but with different params numbers,
                    // add it again
                    else if (!newParams.contains(a2Params.get(i))) {
                        newMethodNames.add(a2MethodNames.get(i));
                        newClassNames.add(a2ClassNames.get(i));
                        newParams.add(a2Params.get(i));
                    }
                }

                AnnotationMirror result = createMethodVal(newClassNames,
                        newMethodNames, newParams);
                return result;
            } else {
                return a1;
            }
        }

        /*
         * NOTE: This subtyping is strictly literal class names, it does not do
         * any fancy parsing of, for example, "java.lang.String" versus
         * "String", and would consider those to be two different classes. This
         * is secure, since it will not allow the system to be fooled by
         * overloading a class name with a local class, and in the actual
         * eventual use of the MethodVal annotation it will not cause any
         * additional problems, it will just make the annotations larger and
         * create a little redundant work. This will not matter once ClassVal
         * annotations are changed to use Class objects instead of Strings
         *
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are MethodVal. In this case, rhs is
         * a subtype of lhs iff, for each rhs.methodName[i], rhs.className[i],
         * rhs.param[i] there is a j such that: lhs.methodName[j] ==
         * rhs.methodName[i] and lhs.className[j] == rhs.className[i] and
         * lhs.param[j] == rhs.param[i]
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            // Are they both MethodVal?
            if (AnnotationUtils.areSameIgnoringValues(lhs, METHODVAL)
                    && AnnotationUtils.areSameIgnoringValues(rhs, METHODVAL)) {
                // Are their methodNames the same, then?
                if (AnnotationUtils.areSame(rhs, lhs)) {
                    lhs = METHODVAL;
                    rhs = METHODVAL;
                    return true;
                } else {
                    List<String> lhsMethodNames = AnnotationUtils
                            .getElementValueArray(lhs, "methodName",
                                    String.class, true);
                    List<String> rhsMethodNames = AnnotationUtils
                            .getElementValueArray(rhs, "methodName",
                                    String.class, true);
                    List<String> lhsClassNames = AnnotationUtils
                            .getElementValueArray(lhs, "className",
                                    String.class, true);
                    List<String> rhsClassNames = AnnotationUtils
                            .getElementValueArray(rhs, "className",
                                    String.class, true);
                    List<Integer> lhsParams = AnnotationUtils
                            .getElementValueArray(lhs, "params", Integer.class,
                                    true);
                    List<Integer> rhsParams = AnnotationUtils
                            .getElementValueArray(rhs, "params", Integer.class,
                                    true);

                    assert lhsMethodNames.size() == lhsClassNames.size()
                            && lhsMethodNames.size() == lhsParams.size() : "Method, Class, and Param annotations are of differing lengths";
                    assert rhsMethodNames.size() == rhsClassNames.size()
                            && rhsMethodNames.size() == rhsParams.size() : "Method, Class, and Param annotations are of differing lengths";

                    // Compare each triplet, a methodname, a
                    // classname, and a params list size, in
                    // rhs and lhs to find if lhs contains all
                    // the triplets in rhs.
                    // ORDER MATTERS, these triplets are only
                    // connected via having the same index in
                    // their respective arrays
                    boolean matching = true;
                    for (int i = 0; i < rhsMethodNames.size() && matching; i++) {
                        int lhsIndex = lhsMethodNames.indexOf(rhsMethodNames
                                .get(i));

                        // The method name was found in lhs,
                        // move on to checking the class name
                        // and params list
                        if (lhsIndex >= 0) {
                            // Get corresponding classnames and
                            // params for i in rhs and lhsIndex
                            // in lhs and make sure they are equal
                            matching = (lhsClassNames.get(lhsIndex).equals(
                                    rhsClassNames.get(i)) && lhsParams.get(
                                    lhsIndex).equals(rhsParams.get(i)));
                        } else {
                            matching = false;
                        }
                    }

                    return matching;
                }
            }

            // Needed to allow MethodVal to be recognized in the supertype map
            if (AnnotationUtils.areSameIgnoringValues(lhs, METHODVAL)) {
                lhs = METHODVAL;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, METHODVAL)) {
                rhs = METHODVAL;
            }

            return super.isSubtype(rhs, lhs);
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new MethodValTreeAnnotator(this)
        );
    }

    /**
     * TreeAnnotator with the visitMethodInvocation method overridden
     */
    protected class MethodValTreeAnnotator extends TreeAnnotator {

        protected MethodValTreeAnnotator(MethodValAnnotatedTypeFactory factory) {
            super(factory);
        }

        /*
         * Assigns the UNKNOWN_CLASS annotation to all literal trees. Not sure
         * why this is necessary since it seems that defaulting should take care
         * of it, but for some reason the CFGBuilder's handling of increment and
         * decrement causes the occurrence of literals with incorrect defaults.
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            type.replaceAnnotation(UNKNOWN_METHOD);
            return super.visitLiteral(tree, type);
        }

        /*
         * Special handling of getMethod and getDeclaredMethod calls. Attempts
         * to get the annotation on the Class object receiver to get the
         * className, the annotation on the String argument to get the
         * methodName, and the parameters argument to get the param, and then
         * builds a new MethodVal annotation from these
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree,
                AnnotatedTypeMirror type) {
            if (!(TreeUtils.isMethodInvocation(tree, getMethod, processingEnv)
                    || TreeUtils.isMethodInvocation(tree, getDeclaredMethod,
                            processingEnv) || TreeUtils.isMethodInvocation(
                    tree, getConstructor, processingEnv))) {
                return super.visitMethodInvocation(tree, type);
            }
            // handle a call to getMethod(), getDeclaredMethod(), or
            // getConstructor
            List<? extends ExpressionTree> args = tree.getArguments();
            ExpressionTree methodNameArg = tree.getArguments().get(0);
            ExpressionTree classReceiver = TreeUtils.getReceiverTree(tree);

            List<String> methodNames;
            List<Integer> params;
            // method name for constructors is always <init>
            if (TreeUtils.isMethodInvocation(tree, getConstructor,
                    processingEnv)) {
                // check if it's a classbound, which is no good for constructors
                methodNames = Arrays.asList(ReflectionResolver.INIT);
                params = getConstructorParamsLen(tree, args);
            } else {
                methodNames = getMethodNames(methodNameArg);
                params = getMethodParamsLen(tree, args);
            }
            List<String> classNames = getClassNames(classReceiver,
                    TreeUtils.isMethodInvocation(tree, getConstructor,
                            processingEnv));

            // *** CREATE CARTESIAN PRODUCT ***
            // Since subtyping is checked by comparing each triplet of
            // the same index across methodNames, classNames, and
            // params, the cartesian product needs to be explicitly
            // created
            List<String> finalMethodNames = new ArrayList<String>();
            List<String> finalClassNames = new ArrayList<String>();
            List<Integer> finalParams = new ArrayList<Integer>();

            // This simple cartesian product is fine, because based on
            // the sources of this, three separate annotations, we
            // know that every element of those three should be used
            // for the cartesian product.
            for (String methodName : methodNames) {
                for (String className : classNames) {
                    for (Integer param : params) {
                        finalMethodNames.add(methodName);
                        finalClassNames.add(className);
                        finalParams.add(param);
                    }
                }
            }

            AnnotationMirror newQual = createMethodVal(finalClassNames,
                    finalMethodNames, finalParams);
            type.replaceAnnotation(newQual);

            // TODO: Why is this returning null rather than calling
            // super????
            return null;
        }

        private List<Integer> getMethodParamsLen(MethodInvocationTree tree,
                List<? extends ExpressionTree> args) {
            // *** HANDLE PARAMETERS LIST ***
            // If they use "null" as the array or there was no array
            List<Integer> params = new ArrayList<>();
            ExpressionTree paramsListArg = null;

            // EDITED 1-15-2014 by plvines
            // Added supporting lists of classes (getMethod("method",
            // String.class, Object.class)) and restructured this whole block to
            // be more understandable (I think).

            // No params arg or null literal
            if (args.size() == 1
                    || args.get(1).getKind() == Tree.Kind.NULL_LITERAL) {
                params.add(0);
            }

            // Two args, second is a single Class or an array
            else if (args.size() == 2) {
                paramsListArg = tree.getArguments().get(1);

                AnnotationMirror annotation = annotationProvider
                        .getAnnotationMirror(paramsListArg, ArrayLen.class);
                if (annotation != null) {
                    params = AnnotationUtils.getElementValueArray(annotation,
                            "value", Integer.class, true);
                }

                // If the parameters list was not an array or null, let's
                // try for a single class object or a list of them
                // TODO BOUND
                else {
                    annotation = annotationProvider.getAnnotationMirror(
                            paramsListArg, ClassVal.class);
                    if (annotation != null) {
                        params.add(1);
                    } else {
                        // Was not a class, error
                        //System.out.println("HERE");
                        params.add(-1);
                    }
                }
            }

            // more than 2 args, so a list of class objects as params
            else if (args.size() > 2) {
                // Check that all the arguments are Class objects
                boolean allAreClasses = true;
                // TODO BOUND
                for (int i = 1; i < args.size() && allAreClasses; i++) {
                    if (annotationProvider.getAnnotationMirror(args.get(i),
                            ClassVal.class) == null) {
                        allAreClasses = false;
                        break;
                    }
                }

                if (allAreClasses) {
                    params.add(args.size() - 1);
                } else {
                    // were not all classes, error
                   // System.out.println("HERE2");
                    params.add(-1);
                }
            }

            // If current annotation system doesn't handle it, we
            // wind up here
            else {
              //  System.out.println("HERE3");
                params.add(-1);
            }
            return params;
        }

        private List<Integer> getConstructorParamsLen(
                MethodInvocationTree tree, List<? extends ExpressionTree> args) {
            // *** HANDLE PARAMETERS LIST ***
            // If they use "null" as the array or there was no array
            List<Integer> params = new ArrayList<>();
            ExpressionTree paramsListArg = null;
            if (args.size() == 1) {
                paramsListArg = tree.getArguments().get(0);
            }
            if (paramsListArg == null
                    || paramsListArg.getKind() == Tree.Kind.NULL_LITERAL) {
                params.add(0);
            } else {
                AnnotationMirror annotation = annotationProvider
                        .getAnnotationMirror(paramsListArg, ArrayLen.class);
                if (annotation != null) {
                    params = AnnotationUtils.getElementValueArray(annotation,
                            "value", Integer.class, true);
                } else {
                    params.add(0);
                }
            }
            // If the parameters list was not an array or null, let's
            // try for a single class object
            // TODO BOUND
            if (params.size() == 0) {
                AnnotationMirror annotation = annotationProvider
                        .getAnnotationMirror(paramsListArg, ClassVal.class);
                if (annotation != null) {
                    params.add(1);
                }
                // If current annotation system doesn't handle it, we
                // wind up here
                else {
                    params.add(-1);
                }
            }
            return params;
        }

        private List<String> getClassNames(ExpressionTree classReceiver,
                boolean mustBeExact) {
            // *** HANDLE CLASS NAME ***
            List<String> classNames = new ArrayList<>();
            // TODO
            // EDITED 1-2-14 by plvines
            // TODO resolved by adding InternalUtils.typeOf(...)? Seems to work.
            // Calling from object.getClass()
            if (classReceiver.getKind() == Tree.Kind.METHOD_INVOCATION) {
                // EDITED 1-3-14 by plvines
                // Implicit "this" in the call to getClass()
                if (TreeUtils.getReceiverTree(classReceiver) == null) {
                    classNames.add(visitorState.getClassTree().toString());
                } else {
                    if (mustBeExact) {
                        classNames.add("Upper Bound: "
                                + classReceiver.toString());
                    } else {
                        classNames.add(InternalUtils.typeOf(
                                TreeUtils.getReceiverTree(classReceiver))
                                .toString());
                    }
                }
                // classNames.add("Unhandled GetClass()");
            }
            // Calling from a Class object
            // TODO BOUND
            else if (classReceiver.getKind() == Tree.Kind.IDENTIFIER ||classReceiver.getKind()== Tree.Kind.MEMBER_SELECT) {
                AnnotationMirror annotation = annotationProvider
                        .getAnnotationMirror(classReceiver, ClassVal.class);
                if (annotation != null) {
                    classNames = AnnotationUtils.getElementValueArray(
                            annotation, "value", String.class, true);
                } else {
                    // Could be ClassBound instead of ClassVal
                    annotation = annotationProvider.getAnnotationMirror(
                            classReceiver, ClassBound.class);
                    if (annotation != null) {
                        if (mustBeExact) {
                            classNames.add("Upper Bound: "
                                    + classReceiver.toString());
                        } else {
                            classNames = AnnotationUtils.getElementValueArray(
                                    annotation, "value", String.class, true);
                        }
                    } else {
                        classNames.add("Unannotated Class: "
                                + classReceiver.toString());
                    }
                }
            }
            return classNames;
        }

        private List<String> getMethodNames(ExpressionTree arg) {
            // *** HANDLE METHOD NAME ***
            List<String> methodNames = new ArrayList<>();

            AnnotationMirror annotation = annotationProvider
                    .getAnnotationMirror(arg, StringVal.class);
            if (annotation != null) {
                methodNames = AnnotationUtils.getElementValueArray(annotation,
                        "value", String.class, true);
            } else {
                methodNames.add("Unannotated Method: " + arg.toString());
            }
            return methodNames;
        }
    }
}
