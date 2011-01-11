import checkers.nullness.quals.*;

public class AssertIfChecked {

  boolean unknown;
	
  @Nullable Object value;

  //:: (assertifxxx.only.on.boolean)
  @AssertNonNullIfTrue("value") public void badform1() {
  }
  
  //:: (assertifxxx.only.on.boolean)
  @AssertNonNullIfTrue("value") public Object badform2() {
	  return new Object();
  }
  
  @AssertNonNullIfTrue("value")
  public boolean goodt1() {
	  return value!=null;
  }

  @AssertNonNullIfTrue("value")
  public boolean badt1() {
	  //:: (assertiftrue.postcondition.not.satisfied)
	  return value==null;
  }
  
  @AssertNonNullIfFalse("value")
  public boolean goodf1() {
	  return value==null;
  }

  @AssertNonNullIfFalse("value")
  public boolean badf1() {
	  //:: (assertiffalse.postcondition.not.satisfied)
	  return value!=null;
  }

  
  @AssertNonNullIfTrue("value")
  public boolean bad2() {
	  //:: (assertiftrue.nullness.condition.error)
	  return value==null || unknown;
  }

  @AssertNonNullIfFalse("value")
  public boolean bad3() {
	  //:: (assertiffalse.nullness.condition.error)
	  return value==null && unknown;
  }

  @AssertNonNullIfTrue("#0")
  boolean testParam(@Nullable Object param) {
	  return param!=null;
  }
  
  
  @AssertNonNullIfTrue("#0")
  boolean testLitTTgood1(@Nullable Object param) {
	  if (param==null) return false;
	  return true;
  }

  @AssertNonNullIfTrue("#0")
  boolean testLitTTbad1(@Nullable Object param) {
	  //:: (assertiftrue.postcondition.not.satisfied)
	  return true;
  }

  @AssertNonNullIfFalse("#0")
  boolean testLitFFgood1(@Nullable Object param) {
	  return true;
  }

  @AssertNonNullIfFalse("#0")
  boolean testLitFFgood2(@Nullable Object param) {
	  if (param==null) return true;
	  return false;
  }
	
  @AssertNonNullIfFalse("#0")
  boolean testLitFFbad1(@Nullable Object param) {
	  //:: (assertiffalse.postcondition.not.satisfied)
	  if (param==null) return false;
	  return true;
  }
	  
  @AssertNonNullIfFalse("#0")
  boolean testLitFFbad2(@Nullable Object param) {
	  //:: (assertiffalse.postcondition.not.satisfied)
	  return false;
  }

}
