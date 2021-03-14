// Test case for Issue 753:
// https://github.com/typetools/checker-framework/issues/753

import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;

@SuppressWarnings({
  "purity",
  "contracts.precondition.not.satisfied",
  "lock.expression.possibly.not.final"
}) // Only test parsing
public class Issue753 extends ReentrantLock {
  final Issue753 field = new Issue753();
  final Issue753[] fields = {this, field};
  final Issue753[][] fieldsArray = {fields};
  final int zero = 0;
  final int[] zeros = {0};

  @Pure
  Issue753 getField(Object param) {
    return field;
  }

  @Pure
  Issue753 getField2() {
    return field;
  }

  @Pure
  Issue753 getField3(String str) {
    return field;
  }

  @Pure
  Issue753[] getFields() {
    return fields;
  }

  @Pure
  Issue753[][] getFieldsArray() {
    return fieldsArray;
  }

  @Pure
  int length(String str) {
    return str.length();
  }

  @Pure
  int[] zeros() {
    return zeros;
  }

  void method() {
    getField(field.field).field.lock();
    method2();
    getField(field.field).getField(field.field).field.lock();
    method3();
    getField(field.field).getField2().field.lock();
    method4();
    getField2().getField2().field.lock();
    method5();
    getField2().getField2().lock();
    method6();
    getField(getField(getField2()).field).field.lock();
    method7();
    this.getField3(")(in string.;))\")(still so.)\"").field.lock();
    method8();
    this.fieldsArray[zeros()[0]][zeros()[0]].fields[zeros()[0]].lock();
    method9();
    this.fieldsArray[length("[")][length("[")].fields[length("[")].field.lock();
    method10();
    this.fieldsArray["[".length()]["[".length()].fields["[".length()].field.lock();
    method11();
    this.getFields()[this.zero].field.lock();
    method12();
    this.getFieldsArray()[this.getField2().zero][this.zero].field.lock();
    method13();
    this.getFields()["][in string.;)]\"][still so.]\"".length()].field.lock();
    method14();
    this.fields[(("][in string.;)]\"][still so.]\"").length())].field.lock();
    method15();
  }

  @Holding("this.getField(this.field.field).field")
  void method2() {}

  @Holding("this.getField(this.field.field).getField(this.field.field).field")
  void method3() {}

  @Holding("this.getField(this.field.field).getField2().field")
  void method4() {}

  @Holding("this.getField2().getField2().field")
  void method5() {}

  @Holding("this.getField2().getField2()")
  void method6() {}

  @Holding("this.getField(this.getField(this.getField2()).field).field")
  void method7() {}

  @Holding("this.getField3(\")(in string.;))\\\")(still so.)\\\"\").field")
  void method8() {}

  @Holding("this.fieldsArray[zeros()[0]][zeros()[0]].fields[zeros()[0]]")
  void method9() {}

  @Holding("this.fieldsArray[length(\"[\")][length(\"[\")].fields[length(\"[\")].field")
  void method10() {}

  @Holding("this.fieldsArray[\"[\".length()][\"[\".length()].fields[\"[\".length()].field")
  void method11() {}

  @Holding("this.getFields()[this.zero].field")
  void method12() {}

  @Holding("this.getFieldsArray()[this.getField2().zero][this.zero].field")
  void method13() {}

  @Holding("this.getFields()[\"][in string.;)]\\\"][still so.]\\\"\".length()].field")
  void method14() {}

  @Holding("this.fields[((\"][in string.;)]\\\"][still so.]\\\"\").length())].field")
  void method15() {}
}
