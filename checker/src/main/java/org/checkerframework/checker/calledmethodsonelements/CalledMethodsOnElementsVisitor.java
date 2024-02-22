// package org.checkerframework.checker.calledmethodsonelements;

// import com.sun.source.tree.AssignmentTree;
// import javax.lang.model.element.VariableElement;
// import com.sun.source.tree.ArrayAccessTree;
// import com.sun.source.tree.CompilationUnitTree;
// import com.sun.source.tree.ExpressionTree;
// import com.sun.source.tree.IdentifierTree;
// import com.sun.source.tree.MemberReferenceTree;
// import com.sun.source.tree.MemberSelectTree;
// import com.sun.source.tree.MethodInvocationTree;
// import com.sun.source.tree.NewClassTree;
// import com.sun.source.tree.Tree;
// import java.lang.annotation.Annotation;
// import java.util.Arrays;
// import java.util.Collection;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.IdentityHashMap;
// import java.util.LinkedHashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import javax.annotation.processing.ProcessingEnvironment;
// import javax.lang.model.element.AnnotationMirror;
// import javax.lang.model.element.Element;
// import javax.lang.model.element.ElementKind;
// import javax.lang.model.element.ExecutableElement;
// import javax.lang.model.type.TypeMirror;
// import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
// import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElementsUnknown;
// import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
// import org.checkerframework.checker.mustcallonelements.MustCallOnElementsAnnotatedTypeFactory;
// import org.checkerframework.checker.mustcallonelements.MustCallOnElementsChecker;
// import org.checkerframework.checker.nullness.qual.Nullable;
// import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
// import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
// import org.checkerframework.common.basetype.BaseTypeChecker;
// import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
// import org.checkerframework.dataflow.cfg.node.Node;
// import org.checkerframework.framework.type.AnnotatedTypeMirror;
// import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
// import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
// import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
// import org.checkerframework.framework.type.QualifierHierarchy;
// import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
// import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
// import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
// import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
// import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
// import org.checkerframework.javacutil.AnnotationBuilder;
// import org.checkerframework.javacutil.TreeUtils;
// import org.checkerframework.javacutil.TypeSystemError;
// import org.checkerframework.javacutil.TypesUtils;
// import com.sun.source.tree.AnnotationTree;
// import com.sun.source.tree.MethodInvocationTree;
// import com.sun.source.tree.MethodTree;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
// import java.util.StringJoiner;
// import javax.lang.model.element.AnnotationMirror;
// import javax.lang.model.element.ExecutableElement;
// import javax.tools.Diagnostic;
// import org.checkerframework.checker.calledmethods.builder.BuilderFrameworkSupport;
// import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElements;
// import org.checkerframework.common.accumulation.AccumulationVisitor;
// import org.checkerframework.common.basetype.BaseTypeChecker;
// import org.checkerframework.dataflow.expression.JavaExpression;
// import org.checkerframework.framework.flow.CFAbstractStore;
// import org.checkerframework.framework.flow.CFAbstractValue;
// import org.checkerframework.framework.source.DiagMessage;
// import org.checkerframework.framework.type.AnnotatedTypeMirror;
// import org.checkerframework.framework.util.JavaExpressionParseUtil;
// import org.checkerframework.framework.util.StringToJavaExpression;
// import org.checkerframework.javacutil.AnnotationMirrorSet;
// import org.checkerframework.javacutil.AnnotationUtils;
// import org.checkerframework.javacutil.TreeUtils;

// /**
//  * This visitor implements the custom error message "finalizer.invocation". It also supports
//  * counting the number of framework build calls.
//  */
// public class CalledMethodsOnElementsVisitor extends AccumulationVisitor {

//   BaseTypeChecker checker;

//   /**
//    * Creates a new CalledMethodsOnElementsVisitor.
//    *
//    * @param checker the type-checker associated with this visitor
//    */
//   public CalledMethodsOnElementsVisitor(BaseTypeChecker checker) {
//     super(checker);
//     this.checker = checker;
//   }

//   /**
//    * Issues an error if the given re-assignment to an {@code @OwningArray} array is not valid. A
//    * re-assignment is valid if the called methods type of the lhs before the assignment satisfies
//    * the must-call obligations of the field.
//    *
//    * <p>Despite the name of this method, the argument {@code node} might be the first and only
//    * assignment to a field.
//    *
//    * @param obligations current tracked Obligations
//    * @param node an assignment to a non-final, owning field
//    */
//   @Override
//   public Void visitAssignment(AssignmentTree node, Void p) {
//     Void superRes = super.visitAssignment(node, p);
//     if (!MustCallOnElementsAnnotatedTypeFactory.doesAssignmentCreateArrayObligation(node)) {
//         return superRes;
//     }
//     assert(node.getVariable() instanceof ArrayAccessTree) :
//         "invariant violated: only assignments with LHS being an array-access are
// pattern-matched";
//     ArrayAccessTree lhs = (ArrayAccessTree) node.getVariable();
//     ExpressionTree arrayTree = lhs.getExpression();
//     MustCallOnElementsAnnotatedTypeFactory mcTypeFactory = new
// MustCallOnElementsAnnotatedTypeFactory(checker);
//     Element lhsElm = TreeUtils.elementFromTree(lhs);
//     AnnotatedTypeMirror atm = mcTypeFactory.getAnnotatedType(lhsElm);
//     AnnotationMirror mcAnno = atm.getPrimaryAnnotation(MustCallOnElements.class);
//     System.out.println("annotation: " + atm);
//     System.out.println("mcanno: " + mcAnno);
//     if (mcAnno == null) {
//       return superRes;
//     }
//     assert (mcAnno != null) : "implement mustcallonelements first";
//     List<String> mcValues =
//         AnnotationUtils.getElementValueArray(
//             mcAnno, mcTypeFactory.getMustCallOnElementsValueElement(), String.class);
//     if (mcValues.isEmpty()) {
//       return superRes;
//     }
//     VariableElement lhsElement = TreeUtils.variableElementFromTree(lhs);
//     checker.reportError(node, "unfulfilled.mustcallonelements.obligations");
//     return superRes;
//   }
// }
