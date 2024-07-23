// package org.checkerframework.checker.resourceleak;

// import org.checkerframework.checker.mustcall.MustCallChecker;
// import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
// import org.checkerframework.checker.nullness.qual.Nullable;
// import org.checkerframework.common.basetype.BaseTypeChecker;
// import org.checkerframework.dataflow.cfg.ControlFlowGraph;
// import org.checkerframework.framework.type.AnnotatedTypeFactory;
// import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

// /**
//  * The type factory for the Resource Leak Checker. The main difference between this and the
// Called
//  * Methods type factory from which it is derived is that this version's {@link
//  * #postAnalyze(ControlFlowGraph)} method checks that must-call obligations are fulfilled.
//  */
// public class ResourceLeakAnnotatedTypeFactory {

//   /**
//    * Creates a new ResourceLeakAnnotatedTypeFactory.
//    *
//    * @param checker the checker associated with this type factory
//    */
//   public ResourceLeakAnnotatedTypeFactory(BaseTypeChecker checker) {
//     super(checker);
//     this.postInit();
//   }

//   // @Override
//   // protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
//   //   return getBundledTypeQualifiers(
//   //       CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);
//   // }

//   // @Override
//   // protected ResourceLeakAnalysis createFlowAnalysis() {
//   //   return new ResourceLeakAnalysis((ResourceLeakChecker) checker, this);
//   // }

//   // @Override
//   // @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
//   // public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>> @Nullable T
// getTypeFactoryOfSubcheckerOrNull(Class<? extends BaseTypeChecker> subCheckerClass) {
//   //   if (subCheckerClass == MustCallChecker.class) {
//   //     if (!canCreateObligations()) {
//   //       return
// super.getTypeFactoryOfSubcheckerOrNull(MustCallNoCreatesMustCallForChecker.class);
//   //     }
//   //   }
//   //   return super.getTypeFactoryOfSubcheckerOrNull(subCheckerClass);
//   // }

//   @Override
//   public void postAnalyze(ControlFlowGraph cfg) {
//     MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
//         new MustCallConsistencyAnalyzer((ResourceLeakChecker) checker);
//     mustCallConsistencyAnalyzer.analyze(cfg);

//     // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
//     // finalizer methods and @InheritableMustCall annotations for the class declarations.
//     if (getWholeProgramInference() != null) {
//       if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
//         MustCallInference.runMustCallInference(
//             (ResourceLeakChecker) checker, cfg, mustCallConsistencyAnalyzer);
//       }
//     }

//     super.postAnalyze(cfg);
//     // tempVarToTree.clear();
//   }
// }
