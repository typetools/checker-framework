import checkers.nullness.quals.*;

public class AssertIfChecked {

  boolean unknown = false;

  @Nullable Object value;

  //:: error: (assertiftrue.only.on.boolean)
  @AssertNonNullIfTrue("value") public void badform1() {
  }

  //:: error: (assertiftrue.only.on.boolean)
  @AssertNonNullIfTrue("value") public Object badform2() {
    return new Object();
  }

  //:: error: (assertiffalse.only.on.boolean)
  @AssertNonNullIfFalse("value") public void badform3() {
  }

  //:: error: (assertiffalse.only.on.boolean)
  @AssertNonNullIfFalse("value") public Object badform4() {
    return new Object();
  }

  @AssertNonNullIfTrue("value")
  public boolean goodt1() {
    return value!=null;
  }

  @AssertNonNullIfTrue("value")
  public boolean badt1() {
    //:: error: (assertiftrue.postcondition.not.satisfied)
    return value==null;
  }

  @AssertNonNullIfFalse("value")
  public boolean goodf1() {
    return value==null;
  }

  @AssertNonNullIfFalse("value")
  public boolean badf1() {
    //:: error: (assertiffalse.postcondition.not.satisfied)
    return value!=null;
  }


  @AssertNonNullIfTrue("value")
  public boolean bad2() {
    //:: error: (assertiftrue.nullness.condition.error)
    return value==null || unknown;
  }

  @AssertNonNullIfFalse("value")
  public boolean bad3() {
    //:: error: (assertiffalse.nullness.condition.error)
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
    //:: error: (assertiftrue.postcondition.not.satisfied)
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
    //:: error: (assertiffalse.postcondition.not.satisfied)
    if (param==null) return false;
    return true;
  }

  @AssertNonNullIfFalse("#0")
  boolean testLitFFbad2(@Nullable Object param) {
    //:: error: (assertiffalse.postcondition.not.satisfied)
    return false;
  }

  @Nullable Object getValueUnpure() { return value; }
  @Pure @Nullable Object getValuePure() { return value; }
  
  @AssertNonNullIfTrue("getValueUnpure()")
  public boolean hasValueUnpure() {
      //:: error: (assertiftrue.postcondition.not.satisfied)
      return getValueUnpure() != null;
  }
  
  @AssertNonNullIfTrue("getValuePure()")
  public boolean hasValuePure() {
      return getValuePure() != null;
  }

  
  /*
   * The next two methods are from Daikon's class Quant. They verify that
   * AssertNonNullIfTrue is correctly added to the assumptions after a check.
   */

  /*@AssertNonNullIfTrue({"#0", "#1"}) */
  /* pure */ public static boolean sameLength(boolean /*@Nullable */[] seq1,
          boolean /*@Nullable */[] seq2) {
      return ((seq1 != null) && (seq2 != null) && seq1.length == seq2.length);
  }

  /* pure */public static boolean isReverse(boolean /*@Nullable */[] seq1,
          boolean /*@Nullable */[] seq2) {
      if (!sameLength(seq1, seq2)) {
          return false;
      }
      // This assert is not needed for inference.
      // assert seq1 != null && seq2 != null; // because sameLength() = true

      int length = seq1.length;
      for (int i = 0; i < length; i++) {
          if (seq1[i] != seq2[length - i - 1]) {
              return false;
          }
      }
      return true;
  }

}
