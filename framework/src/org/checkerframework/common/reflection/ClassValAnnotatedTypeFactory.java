package org.checkerframework.common.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.*;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.common.reflection.qual.ClassValBottom;
import org.checkerframework.common.reflection.qual.UnknownClass;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.tools.javac.code.Type;

/**
 * AnnotatedTypeFactory for the Class Value type system. This factory requires
 * an {@link AnnotationProvider} that provides {@link StringVal} annotations.
 *
 * @author plvines
 * @author rjust
 *
 */
@TypeQualifiers({ UnknownClass.class, ClassVal.class, ClassBound.class,
        ClassValBottom.class })
public class ClassValAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private final AnnotationMirror CLASSVAL = AnnotationUtils.fromClass(
            elements, ClassVal.class);
    private final AnnotationMirror CLASSBOUND = AnnotationUtils.fromClass(
            elements, ClassBound.class);
    private final AnnotationMirror CLASSVAL_BOTTOM = AnnotationUtils.fromClass(
            elements, ClassValBottom.class);
    private final AnnotationMirror UNKNOWN_CLASS = AnnotationUtils.fromClass(
            elements, UnknownClass.class);

    // variable for comparing a MethodInvocationTree against to determine if it
    // is pertinent to this
    private final ExecutableElement forName = TreeUtils.getMethod(
            "java.lang.Class", "forName", 1, processingEnv);
    private final ExecutableElement loadClass = TreeUtils.getMethod(
            "java.lang.ClassLoader", "loadClass", 1, processingEnv);
    private final ExecutableElement getClass = TreeUtils.getMethod(
            "java.lang.Object", "getClass", 0, processingEnv);

    private final AnnotationProvider annotationProvider;

    /**
     * Constructor. Initializes all the AnnotationMirror and Executable Element
     * variables, sets the default annotation.
     *
     * @param checker
     *            The checker used with this AnnotatedTypeFactory
     * @param annotationProvider
     *            The AnnotationProvider providing the necessary StringVal
     *            annotations.
     *
     */
    public ClassValAnnotatedTypeFactory(BaseTypeChecker checker,
            AnnotationProvider annotationProvider) {
        super(checker);
        this.annotationProvider = annotationProvider;
        // TODO This hack is error prone as it is not obvious whether and how
        // postInit has to be used in sub classes!
        if (this.getClass().equals(ClassValAnnotatedTypeFactory.class)) {
            this.postInit();
        }
        this.defaults.addAbsoluteDefault(
                AnnotationUtils.fromClass(elements, UnknownClass.class),
                DefaultLocation.ALL);
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

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new ClassValTreeAnnotator(this)
        );
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = createQualifierHierarchyFactory();
        factory.addQualifier(CLASSVAL_BOTTOM);
        factory.addQualifier(CLASSVAL);
        factory.addQualifier(CLASSBOUND);
        factory.addQualifier(UNKNOWN_CLASS);
        factory.addSubtype(CLASSVAL, UNKNOWN_CLASS);
        factory.addSubtype(CLASSVAL_BOTTOM, CLASSVAL);

        factory.addSubtype(CLASSBOUND, UNKNOWN_CLASS);
        factory.addSubtype(CLASSVAL_BOTTOM, CLASSBOUND);

        return new ClassValQualifierHierarchy(factory, CLASSVAL_BOTTOM);
    }

    /**
     * Constructs a ClassVal annotation by adding all the elements in values to
     * a new ClassVal annotation and returning it
     *
     * @param values
     *            The list of values to add to the new ClassVal annotation
     *
     * @return A new ClassVal annotation with values as its value
     */
    private AnnotationMirror createClassVal(List<String> values) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                ClassVal.class.getCanonicalName());
        builder.setValue("value", values);

        return builder.build();
    }

    /**
     * Constructs a ClassBound annotation by adding all the elements in values
     * to a new ClassBound annotation and returning it
     *
     * @param values
     *            The list of values to add to the new ClassBound annotation
     *
     * @return A new ClassBound annotation with values as its value
     */
    private AnnotationMirror createClassBound(List<String> values) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                ClassBound.class.getCanonicalName());
        builder.setValue("value", values);

        return builder.build();
    }

    /**
     * The qualifier hierarchy for the ClassVal type system
     */
    protected class ClassValQualifierHierarchy extends
            MultiGraphQualifierHierarchy {

        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         * @param bottom
         *            The bottom annotation in the constructed hierarchy
         */
        protected ClassValQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory,
                AnnotationMirror bottom) {
            super(factory, bottom);
        }

        /*
         * Determines the least upper bound of a1 and a2. If both are ClassVal
         * annotations, then the least upper bound is the set of elements
         * obtained by combining the values of both annotations.
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if (!AnnotationUtils.areSameIgnoringValues(getTopAnnotation(a1),
                    getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            } else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                List<String> a1Values = AnnotationUtils.getElementValueArray(
                        a1, "value", String.class, true);
                List<String> a2Values = AnnotationUtils.getElementValueArray(
                        a2, "value", String.class, true);
                ArrayList<String> newValues = new ArrayList<String>();

                for (String s : a1Values) {
                    if (!newValues.contains(s)) {
                        newValues.add(s);
                    }
                }
                for (String s : a2Values) {
                    if (!newValues.contains(s)) {
                        newValues.add(s);
                    }
                }

                AnnotationMirror result = createClassVal(newValues);

                return result;
            } else if (AnnotationUtils.areSameIgnoringValues(a1, CLASSBOUND)
                    && AnnotationUtils.areSameIgnoringValues(a2, CLASSVAL)) {
                List<String> a1Values = AnnotationUtils.getElementValueArray(
                        a1, "value", String.class, true);
                List<String> a2Values = AnnotationUtils.getElementValueArray(
                        a2, "value", String.class, true);
                ArrayList<String> newValues = new ArrayList<String>();

                for (String s : a1Values) {
                    if (!newValues.contains(s)) {
                        newValues.add(s);
                    }
                }
                for (String s : a2Values) {
                    if (!newValues.contains(s)) {
                        newValues.add(s);
                    }
                }

                AnnotationMirror result = createClassBound(newValues);

                return result;
            } else {
                return a1;
            }
        }

        /*
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are ClassVal. In this case, rhs is
         * a subtype of lhs iff lhs contains at least every element of rhs
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            // Are they both ClassVal?
            if (AnnotationUtils.areSameIgnoringValues(lhs, CLASSVAL)
                    && AnnotationUtils.areSameIgnoringValues(rhs, CLASSVAL)) {
                // Are their values the same, then?
                if (AnnotationUtils.areSame(rhs, lhs)) {
                    lhs = CLASSVAL;
                    rhs = CLASSVAL;

                    // return super.isSubtype(rhs, lhs) || true;
                    return true;
                } else {
                    List<String> lhsValues = AnnotationUtils
                            .getElementValueArray(lhs, "value", String.class,
                                    true);
                    List<String> rhsValues = AnnotationUtils
                            .getElementValueArray(rhs, "value", String.class,
                                    true);

                    return lhsValues.containsAll(rhsValues);
                }
            } else if (AnnotationUtils.areSameIgnoringValues(lhs, CLASSBOUND)
                    && AnnotationUtils.areSameIgnoringValues(rhs, CLASSVAL)) {
                // Are their values the same, then?
                if (AnnotationUtils.areSame(rhs, lhs)) {
                    return true;
                } else {
                    List<String> lhsValues = AnnotationUtils
                            .getElementValueArray(lhs, "value", String.class,
                                    true);
                    List<String> rhsValues = AnnotationUtils
                            .getElementValueArray(rhs, "value", String.class,
                                    true);

                    return lhsValues.containsAll(rhsValues);
                }
            }

            else if (AnnotationUtils.areSameIgnoringValues(lhs, CLASSBOUND)
                    && AnnotationUtils.areSameIgnoringValues(rhs, CLASSBOUND)) {
                // Are their values the same, then?
                if (AnnotationUtils.areSame(rhs, lhs)) {
                    return true;
                } else {
                    List<String> lhsValues = AnnotationUtils
                            .getElementValueArray(lhs, "value", String.class,
                                    true);
                    List<String> rhsValues = AnnotationUtils
                            .getElementValueArray(rhs, "value", String.class,
                                    true);

                    return lhsValues.containsAll(rhsValues);
                }
            }

            // Needed to allow ClassVal to be recognized in the supertype map
            if (AnnotationUtils.areSameIgnoringValues(lhs, CLASSVAL)) {
                lhs = CLASSVAL;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, CLASSVAL)) {
                rhs = CLASSVAL;
            }

            // Needed to allow ClassVal to be recognized in the supertype map
            if (AnnotationUtils.areSameIgnoringValues(lhs, CLASSBOUND)) {
                lhs = CLASSBOUND;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, CLASSBOUND)) {
                rhs = CLASSBOUND;
            }

            return super.isSubtype(rhs, lhs);
        }
    }

    /**
     * TreeAnnotator with the visitMemberSelect and visitMethodInvocation
     * methods overridden
     */
    protected class ClassValTreeAnnotator extends TreeAnnotator {

        protected ClassValTreeAnnotator(ClassValAnnotatedTypeFactory factory) {
            super(factory);
        }

        /*
         * Handles instances of TYPE.class to create a new ClassVal annotation
         * with their value
         */
        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            ExpressionTree etree = tree.getExpression();
            Element e = InternalUtils.symbol(etree);
            if (e != null) {
                String name = getClassname(e);
                AnnotationMirror newQual = createClassVal(Arrays.asList(name));
                type.replaceAnnotation(newQual);;
                return null;
            }
            return super.visitMemberSelect(tree, type);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            type.replaceAnnotation(UNKNOWN_CLASS);
            return super.visitLiteral(tree, type);
        }

        /*
         * Handles instances of Class.forName() by attempting to get the
         * StringVal annotation of the argument and then use that as the value
         * for the ClassVal annotation
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree,
                AnnotatedTypeMirror type) {

            if (TreeUtils.isMethodInvocation(tree, forName, processingEnv)
                    || TreeUtils.isMethodInvocation(tree, loadClass,
                            processingEnv)) {

                // forName or loadClass will only ever have 1 argument so get
                // just it
                ExpressionTree arg = tree.getArguments().get(0);

                AnnotationMirror annotation = annotationProvider
                        .getAnnotationMirror(arg, StringVal.class);
                if (annotation != null) {
                    List<String> argValues = AnnotationUtils
                            .getElementValueArray(annotation, "value",
                                    String.class, true);
                    AnnotationMirror newQual = createClassVal(argValues);
                    type.replaceAnnotation(newQual);

                    return null;
                }
            } else if (TreeUtils.isMethodInvocation(tree, getClass,
                    processingEnv)) {
                if (TreeUtils.getReceiverTree(tree) != null) {

                    Type clType = (Type) InternalUtils.typeOf(TreeUtils
                            .getReceiverTree(tree));
                    String className = getClassname(clType);
                    AnnotationMirror newQual = createClassBound(Arrays
                            .asList(className));
                    type.replaceAnnotation(newQual);
                }
                return null;
            }
            return super.visitMethodInvocation(tree, type);
        }

        /**
         * Return String representation of class name.
         */
        private String getClassname(Type classType) {
            StringBuilder className = new StringBuilder(TypesUtils.getQualifiedName((DeclaredType) classType).toString());
            if (classType.getEnclosingType() != null) {
                while (classType.getEnclosingType().getKind() != TypeKind.NONE) {
                    classType = classType.getEnclosingType();
                    int last = className.lastIndexOf(".");
                    if (last > -1)
                        className.replace(last, last + 1, "$");
                }
            }
            return className.toString();
        }

        private String getClassname(Element classType) {
            StringBuilder className = new StringBuilder(ElementUtils.getQualifiedClassName(classType));
            if (classType.getEnclosingElement() != null) {
                while (classType.getEnclosingElement().getKind() == ElementKind.CLASS  ) {
                    classType = classType.getEnclosingElement();
                    int last = className.lastIndexOf(".");
                    if (last > -1)
                        className.replace(last, last + 1, "$");
                    if (classType == null) break;
                }
            }
            return className.toString();
        }
    }
}
