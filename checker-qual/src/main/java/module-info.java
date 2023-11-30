/**
 * This module contains annotations (type qualifiers) that a programmer writes to specify Java code
 * for type-checking by the Checker Framework.
 */
module org.checkerframework.checker.qual {
  exports org.checkerframework.checker.builder.qual;
  exports org.checkerframework.checker.calledmethods.qual;
  exports org.checkerframework.checker.compilermsgs.qual;
  exports org.checkerframework.checker.fenum.qual;
  exports org.checkerframework.checker.formatter.qual;
  exports org.checkerframework.checker.guieffect.qual;
  exports org.checkerframework.checker.i18n.qual;
  exports org.checkerframework.checker.i18nformatter.qual;
  exports org.checkerframework.checker.index.qual;
  exports org.checkerframework.checker.initialization.qual;
  exports org.checkerframework.checker.interning.qual;
  exports org.checkerframework.checker.lock.qual;
  exports org.checkerframework.checker.mustcall.qual;
  exports org.checkerframework.checker.nullness.qual;
  exports org.checkerframework.checker.optional.qual;
  exports org.checkerframework.checker.propkey.qual;
  exports org.checkerframework.checker.regex.qual;
  exports org.checkerframework.checker.signature.qual;
  exports org.checkerframework.checker.signedness.qual;
  exports org.checkerframework.checker.tainting.qual;
  exports org.checkerframework.checker.units.qual;
  exports org.checkerframework.common.aliasing.qual;
  exports org.checkerframework.common.initializedfields.qual;
  exports org.checkerframework.common.reflection.qual;
  exports org.checkerframework.common.returnsreceiver.qual;
  exports org.checkerframework.common.subtyping.qual;
  exports org.checkerframework.common.util.report.qual;
  exports org.checkerframework.common.value.qual;
  exports org.checkerframework.dataflow.qual;
  exports org.checkerframework.framework.qual;

  // requires static org.checkerframework.checker;

  requires static java.desktop;
  requires static java.compiler;
  requires static jdk.compiler;
}
