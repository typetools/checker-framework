package org.checkerframework.common.basetype;

import java.util.List;
import javax.lang.model.element.VariableElement;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.Pair;

/**
 * A factory that extends {@link GenericAnnotatedTypeFactory} to use the default flow-sensitive
 * analysis as provided by {@link CFAnalysis}.
 */
public class BaseAnnotatedTypeFactory
    extends GenericAnnotatedTypeFactory<CFValue, CFStore, CFTransfer, CFAnalysis> {

  public BaseAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
    super(checker, useFlow);

    // Every subclass must call postInit!
    if (this.getClass() == BaseAnnotatedTypeFactory.class) {
      this.postInit();
    }
  }

  public BaseAnnotatedTypeFactory(BaseTypeChecker checker) {
    this(checker, flowByDefault);
  }

  @Override
  protected CFAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
    return new CFAnalysis(checker, this, fieldValues);
  }
}
