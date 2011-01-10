import checkers.nullness.quals.*;

public class AssertIfChecked {

  boolean unknown;
	
  @Nullable Object value;

  //:: (assert.nullness.only.on.boolean)
  @AssertNonNullIfTrue("value") public void badform1() {
  }
  
  //:: (assert.nullness.only.on.boolean)
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
}
