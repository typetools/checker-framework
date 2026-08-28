// Test case for the `slow.typechecking` warning, under the *Value Checker*.
//
// Distilled from randoop/util/ClassFileConstants.java at randoop commit
// 6ecb7d5cd0, the parent of https://github.com/randoop/randoop/pull/1846
// ("Extract method to make type checking faster").  That PR moved the body of
// the `switch` below into a method of its own, purely to make type-checking
// faster; this file keeps the pre-PR shape.
//
// The expensive shape is `getConstants`: a doubly-nested loop whose innermost
// statement is a `switch` on an integer with about 200 `case` labels, all
// compile-time integer constants.
//
//   $CHECKERFRAMEWORK/checker/bin/javac -processor value \
//       -AslowTypecheckingSeconds=5 RandoopClassFileConstants.java
//
//   RandoopClassFileConstants.java:42: warning: [slow.typechecking]
//     typechecking took 22 seconds; consider making inferred type variables explicit
//
// Extracting the switch into its own method, as PR #1846 did, drops that to
// about 3 seconds.  The Nullness and Tainting Checkers take only 1-2 seconds on
// this file either way, so `-processor value` is required to reproduce.
//
// Everything Randoop depended on -- Apache BCEL, plume-lib, and randoop's own
// classes -- is stubbed at the bottom of this file, so the test case is
// self-contained.  The stubs are deliberately trivial: the cost is in the shape
// of `getConstants`, not in the types it manipulates.

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;

/**
 * Reads literals from a class file, including from the constant pool and from bytecodes that take
 * immediate arguments.
 */
public class RandoopClassFileConstants {

  /** Stores constant values from a class file. */
  public static class ConstantSet {
    /** Name of class containing the constants. */
    public @ClassGetName String classname = "java.lang.Object";

    /** Set of all int constants in a class. */
    public Set<Integer> ints = new TreeSet<>();

    /** Set of all long constants in a class. */
    public Set<Long> longs = new TreeSet<>();

    /** Set of all float constants in a class. */
    public Set<Float> floats = new TreeSet<>();

    /** Set of all double constants in a class. */
    public Set<Double> doubles = new TreeSet<>();

    /** Set of all string constants in a class. */
    public Set<String> strings = new TreeSet<>();

    /** Values that are non-receiver terms. */
    public Set<Class<?>> classes = new HashSet<>();

    /** Set of all enum constants in a class. */
    public Set<Enum<?>> enums = new HashSet<>();

    /** Map that stores the number of uses of each constant in the current class. */
    public Map<Object, Integer> constantFrequency = new HashMap<>();

    /** Creates a new ConstantSet. */
    public ConstantSet() {}
  }

  /** Do not instantiate. */
  private RandoopClassFileConstants() {
    throw new Error("Do not instantiate");
  }

  /**
   * Adds all the constants found in the given class into the given ConstantSet, and returns it.
   *
   * @param classname the name of the type
   * @param result the set of constants to which constants are added
   * @return the set of constants with new constants of given type added
   * @see #getConstants(String)
   */
  public static ConstantSet getConstants(String classname, ConstantSet result) {

    String classfileBase = classname.replace('.', '/');
    ClassParser cp;
    JavaClass jc;
    try (InputStream is = ClassPath.SYSTEM_CLASS_PATH.getInputStream(classfileBase, ".class")) {
      cp = new ClassParser(is, classname);
      jc = cp.parse();
    } catch (java.io.IOException e) {
      throw new Error("IOException while reading '" + classname + "': " + e.getMessage());
    }
    @SuppressWarnings("signature") // BCEL's JavaClass is not annotated for the Signature Checker
    @ClassGetName String resultClassname = jc.getClassName();
    result.classname = resultClassname;

    // Get all of the constants from the classfile's constant pool.
    ConstantPool constant_pool = jc.getConstantPool();
    for (Constant c : constant_pool.getConstantPool()) {
      // System.out.printf ("*Constant = %s [%s]%n", c, c.getClass());
      if (c == null
          || c instanceof ConstantClass
          || c instanceof ConstantFieldref
          || c instanceof ConstantInterfaceMethodref
          || c instanceof ConstantMethodref
          || c instanceof ConstantNameAndType
          || c instanceof ConstantMethodHandle
          || c instanceof ConstantMethodType
          || c instanceof ConstantInvokeDynamic
          || c instanceof ConstantUtf8) {
        continue;
      }
      if (c instanceof ConstantString) {
        result.strings.add((String) ((ConstantString) c).getConstantValue(constant_pool));
      } else if (c instanceof ConstantDouble) {
        result.doubles.add((Double) ((ConstantDouble) c).getConstantValue(constant_pool));
      } else if (c instanceof ConstantFloat) {
        result.floats.add((Float) ((ConstantFloat) c).getConstantValue(constant_pool));
      } else if (c instanceof ConstantInteger) {
        result.ints.add((Integer) ((ConstantInteger) c).getConstantValue(constant_pool));
      } else if (c instanceof ConstantLong) {
        result.longs.add((Long) ((ConstantLong) c).getConstantValue(constant_pool));
      } else {
        throw new RuntimeException("Unrecognized constant of type " + c.getClass() + ": " + c);
      }
    }

    ClassGen gen = new ClassGen(jc);
    ConstantPoolGen pool = gen.getConstantPool();

    // Process the code in each method looking for literals
    for (Method m : jc.getMethods()) {
      @SuppressWarnings("signature") // BCEL's JavaClass is not annotated for the Signature Checker
      MethodGen mg = new MethodGen(m, jc.getClassName(), pool);
      InstructionList il = mg.getInstructionList();
      if (il != null) {
        for (Instruction inst : il.getInstructions()) {
          switch (inst.getOpcode()) {

            // Compare two objects, no literals
            case Const.IF_ACMPEQ:
            case Const.IF_ACMPNE:
              break;

            // These instructions compare the integer on the top of the stack
            // to zero. There are no literals here (except 0).
            case Const.IFEQ:
            case Const.IFNE:
            case Const.IFLT:
            case Const.IFGE:
            case Const.IFGT:
            case Const.IFLE:
              {
                // If no instruction is followed by those instructions, then it is comparing to 0.
                registerIntegerConstant(0, result);
                break;
              }

            // InstanceOf pushes either 0 or 1 on the stack depending on
            // whether
            // the object on top of stack is of the specified type.
            // If we're interested in class literals, this would be interesting
            case Const.INSTANCEOF:
              break;

            // Duplicates the item on the top of stack. No literal.
            case Const.DUP:
              {
                break;
              }

            // Duplicates the item on the top of the stack and inserts it 2
            // values down in the stack. No literals
            case Const.DUP_X1:
              {
                break;
              }

            // Duplicates either the top 2 category 1 values or a single
            // category 2 value and inserts it 2 or 3 values down on the
            // stack.
            case Const.DUP2_X1:
              {
                break;
              }

            // Duplicate either one category 2 value or two category 1 values.
            case Const.DUP2:
              {
                break;
              }

            // Dup the category 1 value on the top of the stack and insert it
            // either
            // two or three values down on the stack.
            case Const.DUP_X2:
              {
                break;
              }

            case Const.DUP2_X2:
              {
                break;
              }

            // Pop instructions discard the top of the stack.
            case Const.POP:
              {
                break;
              }

            // Pops either the top 2 category 1 values or a single category 2
            // value
            // from the top of the stack.
            case Const.POP2:
              {
                break;
              }

            // Swaps the two category 1 types on the top of the stack.
            case Const.SWAP:
              {
                break;
              }

            // Compares two integers on the stack
            case Const.IF_ICMPEQ:
            case Const.IF_ICMPGE:
            case Const.IF_ICMPGT:
            case Const.IF_ICMPLE:
            case Const.IF_ICMPLT:
            case Const.IF_ICMPNE:
              {
                break;
              }

            // Get the value of a field
            case Const.GETFIELD:
              {
                break;
              }

            // stores the top of stack into a field
            case Const.PUTFIELD:
              {
                break;
              }

            // Pushes the value of a static field on the stack
            case Const.GETSTATIC:
              {
                FieldInstruction fieldInstruction = (FieldInstruction) inst;
                // Get the name of the referenced type that the instruction refers to
                String referencedTypeName = fieldInstruction.getReferenceType(pool).toString();

                if (!referencedTypeName.contains("$")) {
                  break; // out of `case Const.GETSTATIC:`
                }
                // It is a nested class, and it might be an enum.

                try {
                  Class<?> enumClass = Class.forName((@ClassGetName String) referencedTypeName);

                  // Example of how enum value can be extracted
                  // @SuppressWarnings("unchecked")
                  // Enum<?> enumConstant = Enum.valueOf((Class<Enum>) enumClass, "ENUM_ONE");

                  if (enumClass.isEnum()) {
                    @SuppressWarnings("unchecked")
                    Class<Enum> enumType = (Class<Enum>) enumClass;

                    String fieldName = fieldInstruction.getFieldName(pool);

                    // TODO: Use the more specific enumType in the valueOf call to avoid unchecked
                    // warning
                    @SuppressWarnings("unchecked")
                    Enum<?> enumConstant = Enum.valueOf(enumType, fieldName);

                    result.enums.add(enumConstant);
                    result.constantFrequency.put(
                        enumConstant, result.constantFrequency.getOrDefault(enumConstant, 0) + 1);
                  }

                } catch (ClassNotFoundException e) {
                  throw new RuntimeException(e);
                }
                break; // out of `case Const.GETSTATIC:`
              }

            // Pops a value off of the stack into a static field
            case Const.PUTSTATIC:
              {
                break;
              }

            // pushes a local onto the stack
            case Const.DLOAD:
            case Const.DLOAD_0:
            case Const.DLOAD_1:
            case Const.DLOAD_2:
            case Const.DLOAD_3:
            case Const.FLOAD:
            case Const.FLOAD_0:
            case Const.FLOAD_1:
            case Const.FLOAD_2:
            case Const.FLOAD_3:
            case Const.ILOAD:
            case Const.ILOAD_0:
            case Const.ILOAD_1:
            case Const.ILOAD_2:
            case Const.ILOAD_3:
            case Const.LLOAD:
            case Const.LLOAD_0:
            case Const.LLOAD_1:
            case Const.LLOAD_2:
            case Const.LLOAD_3:
              {
                break;
              }

            // Pops a value off of the stack into a local
            case Const.DSTORE:
            case Const.DSTORE_0:
            case Const.DSTORE_1:
            case Const.DSTORE_2:
            case Const.DSTORE_3:
            case Const.FSTORE:
            case Const.FSTORE_0:
            case Const.FSTORE_1:
            case Const.FSTORE_2:
            case Const.FSTORE_3:
            case Const.ISTORE:
            case Const.ISTORE_0:
            case Const.ISTORE_1:
            case Const.ISTORE_2:
            case Const.ISTORE_3:
            case Const.LSTORE:
            case Const.LSTORE_0:
            case Const.LSTORE_1:
            case Const.LSTORE_2:
            case Const.LSTORE_3:
              {
                break;
              }

            // Push a value from the constant pool. We'll get these
            // values when processing the constant pool itself.
            case Const.LDC:
              {
                LDC ldcInstruction = (LDC) inst;
                int index = ldcInstruction.getIndex();
                Constant constant = constant_pool.getConstant(index);
                registerConstant(constant, constant_pool, result);
                break;
              }
            case Const.LDC_W:
              // TODO: Could be redundant
              {
                LDC_W ldc_w = (LDC_W) inst;
                int index = ldc_w.getIndex();
                Constant constant = constant_pool.getConstant(index);
                registerConstant(constant, constant_pool, result);
                break;
              }
            case Const.LDC2_W:
              {
                // Like the LDC, but for longs and doubles
                LDC2_W ldc2_w = (LDC2_W) inst;
                int index = ldc2_w.getIndex();
                Constant constant = constant_pool.getConstant(index);
                registerConstant(constant, constant_pool, result);
                break;
              }

            // Push the length of an array on the stack
            case Const.ARRAYLENGTH:
              {
                break;
              }

            // Push small constants (-1..5) on the stack.
            case Const.DCONST_0:
              registerDoubleConstant(Double.valueOf(0), result);
              break;
            case Const.DCONST_1:
              registerDoubleConstant(Double.valueOf(1), result);
              break;
            case Const.FCONST_0:
              registerFloatConstant(Float.valueOf(0), result);
              break;
            case Const.FCONST_1:
              registerFloatConstant(Float.valueOf(1), result);
              break;
            case Const.FCONST_2:
              registerFloatConstant(Float.valueOf(2), result);
              break;
            case Const.ICONST_0:
              registerIntegerConstant(0, result);
              break;
            case Const.ICONST_1:
              registerIntegerConstant(1, result);
              break;
            case Const.ICONST_2:
              registerIntegerConstant(2, result);
              break;
            case Const.ICONST_3:
              registerIntegerConstant(3, result);
              break;
            case Const.ICONST_4:
              registerIntegerConstant(4, result);
              break;
            case Const.ICONST_5:
              registerIntegerConstant(5, result);
              break;
            case Const.ICONST_M1:
              registerIntegerConstant(-1, result);
              break;
            case Const.LCONST_0:
              registerLongConstant(Long.valueOf(0), result);
              break;
            case Const.LCONST_1:
              registerLongConstant(Long.valueOf(1), result);
              break;

            case Const.BIPUSH:
            case Const.SIPUSH:
              ConstantPushInstruction cpi = (ConstantPushInstruction) inst;
              registerIntegerConstant((Integer) cpi.getValue(), result);
              break;

            // Primitive Binary operators.
            case Const.DADD:
            case Const.DCMPG:
            case Const.DCMPL:
            case Const.DDIV:
            case Const.DMUL:
            case Const.DREM:
            case Const.DSUB:
            case Const.FADD:
            case Const.FCMPG:
            case Const.FCMPL:
            case Const.FDIV:
            case Const.FMUL:
            case Const.FREM:
            case Const.FSUB:
            case Const.IADD:
            case Const.IAND:
            case Const.IDIV:
            case Const.IMUL:
            case Const.IOR:
            case Const.IREM:
            case Const.ISHL:
            case Const.ISHR:
            case Const.ISUB:
            case Const.IUSHR:
            case Const.IXOR:
            case Const.LADD:
            case Const.LAND:
            case Const.LCMP:
            case Const.LDIV:
            case Const.LMUL:
            case Const.LOR:
            case Const.LREM:
            case Const.LSHL:
            case Const.LSHR:
            case Const.LSUB:
            case Const.LUSHR:
            case Const.LXOR:
              break;

            case Const.LOOKUPSWITCH:
            case Const.TABLESWITCH:
              break;

            case Const.ANEWARRAY:
            case Const.NEWARRAY:
              {
                break;
              }

            case Const.MULTIANEWARRAY:
              {
                break;
              }

            // push the value at an index in an array
            case Const.AALOAD:
            case Const.BALOAD:
            case Const.CALOAD:
            case Const.DALOAD:
            case Const.FALOAD:
            case Const.IALOAD:
            case Const.LALOAD:
            case Const.SALOAD:
              {
                break;
              }

            // Pop the top of stack into an array location
            case Const.AASTORE:
            case Const.BASTORE:
            case Const.CASTORE:
            case Const.DASTORE:
            case Const.FASTORE:
            case Const.IASTORE:
            case Const.LASTORE:
            case Const.SASTORE:
              break;

            case Const.ARETURN:
            case Const.DRETURN:
            case Const.FRETURN:
            case Const.IRETURN:
            case Const.LRETURN:
            case Const.RETURN:
              {
                break;
              }

            // subroutine calls.
            case Const.INVOKESTATIC:
            case Const.INVOKEVIRTUAL:
            case Const.INVOKESPECIAL:
            case Const.INVOKEINTERFACE:
            case Const.INVOKEDYNAMIC:
              break;

            // Throws an exception.
            case Const.ATHROW:
              break;

            // Opcodes that don't need any modifications. Here for reference.
            case Const.ACONST_NULL:
            case Const.ALOAD:
            case Const.ALOAD_0:
            case Const.ALOAD_1:
            case Const.ALOAD_2:
            case Const.ALOAD_3:
            case Const.ASTORE:
            case Const.ASTORE_0:
            case Const.ASTORE_1:
            case Const.ASTORE_2:
            case Const.ASTORE_3:
            case Const.CHECKCAST:
            case Const.D2F: // double to float
            case Const.D2I: // double to integer
            case Const.D2L: // double to long
            case Const.DNEG: // Negate double on top of stack
            case Const.F2D: // float to double
            case Const.F2I: // float to integer
            case Const.F2L: // float to long
            case Const.FNEG: // Negate float on top of stack
            case Const.GOTO:
            case Const.GOTO_W:
            case Const.I2B: // integer to byte
            case Const.I2C: // integer to char
            case Const.I2D: // integer to double
            case Const.I2F: // integer to float
            case Const.I2L: // integer to long
            case Const.I2S: // integer to short
            case Const.IFNONNULL:
            case Const.IFNULL:
            case Const.IINC: // increment local variable by a constant
            case Const.INEG: // negate integer on top of stack
            case Const.JSR: // pushes return address on the stack,
            case Const.JSR_W:
            case Const.L2D: // long to double
            case Const.L2F: // long to float
            case Const.L2I: // long to int
            case Const.LNEG: // negate long on top of stack
            case Const.MONITORENTER:
            case Const.MONITOREXIT:
            case Const.NEW:
            case Const.NOP:
            case Const.RET: // this is the internal JSR return
            case Const.WIDE:
              break;

            // Make sure we didn't miss anything
            default:
              throw new RandoopBug("instruction " + inst + " unsupported");
          }
        }
      }
    }
    return result;
  }

  /**
   * Register a constant in the given ConstantSet.
   *
   * @param constant the constant
   * @param constant_pool a constant pool that is used if the constant is a String, Class, or Enum
   * @param cs the ConstantSet
   */
  static void registerConstant(Constant constant, ConstantPool constant_pool, ConstantSet cs) {
    if (constant instanceof ConstantInteger) {
      int intValue = ((ConstantInteger) constant).getBytes();
      registerIntegerConstant(intValue, cs);
    } else if (constant instanceof ConstantFloat) {
      float floatValue = ((ConstantFloat) constant).getBytes();
      registerFloatConstant(floatValue, cs);
      // TODO: Long and Doubles could be redundant
    } else if (constant instanceof ConstantLong) {
      long longValue = ((ConstantLong) constant).getBytes();
      registerLongConstant(longValue, cs);
    } else if (constant instanceof ConstantDouble) {
      double doubleValue = ((ConstantDouble) constant).getBytes();
      registerDoubleConstant(doubleValue, cs);
    } else if (constant instanceof ConstantString) {
      String s = ((ConstantString) constant).getBytes(constant_pool);
      registerStringConstant(s, cs);
    } else if (constant instanceof ConstantClass) {
      String className = ((ConstantClass) constant).getBytes(constant_pool);
      className = className.replace('/', '.');
      try {
        @SuppressWarnings("signature:cast.unsafe") // TODO: How you know about this
        Class<?> c = Class.forName((@ClassGetName String) className);
        // Add to the classes only if it is used by LDC instruction in order to avoid
        // self classes and classes like Java.lang.Object.class and
        // Java.lang.System.class.
        registerClassConstant(c, cs);
      } catch (ClassNotFoundException e) {
        throw new RandoopBug(e);
      }
    } else {
      throw new RuntimeException("Unrecognized constant of type " + constant.getClass());
    }
  }

  /**
   * Register a double constant in the given ConstantSet.
   *
   * @param value the double constant
   * @param cs the ConstantSet
   */
  static void registerDoubleConstant(Double value, ConstantSet cs) {
    cs.doubles.add(value);
    MapsP.incrementMap(cs.constantFrequency, value);
  }

  /**
   * Register a float constant in the given ConstantSet.
   *
   * @param value the float constant
   * @param cs the ConstantSet
   */
  static void registerFloatConstant(Float value, ConstantSet cs) {
    cs.floats.add(value);
    MapsP.incrementMap(cs.constantFrequency, value);
  }

  /**
   * Register an integer constant in the given ConstantSet.
   *
   * @param value the integer constant
   * @param cs the ConstantSet
   */
  static void registerIntegerConstant(Integer value, ConstantSet cs) {
    cs.ints.add(value);
    MapsP.incrementMap(cs.constantFrequency, value);
  }

  /**
   * Register a long constant in the given ConstantSet.
   *
   * @param value the long constant
   * @param cs the ConstantSet
   */
  static void registerLongConstant(Long value, ConstantSet cs) {
    cs.longs.add(value);
    MapsP.incrementMap(cs.constantFrequency, value);
  }

  /**
   * Register a String constant in the given ConstantSet.
   *
   * @param value the String constant
   * @param cs the ConstantSet
   */
  static void registerStringConstant(String value, ConstantSet cs) {
    cs.strings.add(value);
    MapsP.incrementMap(cs.constantFrequency, value);
  }

  /**
   * Register a Class constant in the given ConstantSet.
   *
   * @param value the Class constant
   * @param cs the ConstantSet
   */
  static void registerClassConstant(Class<?> value, ConstantSet cs) {
    cs.classes.add(value);
    MapsP.incrementMap(cs.constantFrequency, value);
  }

  ////////////////////////////////////////////////////////////////////////////
  /// Stubs.
  ///
  /// These stand in for Apache BCEL, plume-lib, and randoop classes, so that
  /// this test case is self-contained.  They are not the point of the test;
  /// only the shape of `getConstants` above is.
  ///

  /** Stub for {@code org.apache.bcel.Const}: the JVM opcodes. */
  static class Const {
    static final short AALOAD = 0;
    static final short AASTORE = 1;
    static final short ACONST_NULL = 2;
    static final short ALOAD = 3;
    static final short ALOAD_0 = 4;
    static final short ALOAD_1 = 5;
    static final short ALOAD_2 = 6;
    static final short ALOAD_3 = 7;
    static final short ANEWARRAY = 8;
    static final short ARETURN = 9;
    static final short ARRAYLENGTH = 10;
    static final short ASTORE = 11;
    static final short ASTORE_0 = 12;
    static final short ASTORE_1 = 13;
    static final short ASTORE_2 = 14;
    static final short ASTORE_3 = 15;
    static final short ATHROW = 16;
    static final short BALOAD = 17;
    static final short BASTORE = 18;
    static final short BIPUSH = 19;
    static final short CALOAD = 20;
    static final short CASTORE = 21;
    static final short CHECKCAST = 22;
    static final short D2F = 23;
    static final short D2I = 24;
    static final short D2L = 25;
    static final short DADD = 26;
    static final short DALOAD = 27;
    static final short DASTORE = 28;
    static final short DCMPG = 29;
    static final short DCMPL = 30;
    static final short DCONST_0 = 31;
    static final short DCONST_1 = 32;
    static final short DDIV = 33;
    static final short DLOAD = 34;
    static final short DLOAD_0 = 35;
    static final short DLOAD_1 = 36;
    static final short DLOAD_2 = 37;
    static final short DLOAD_3 = 38;
    static final short DMUL = 39;
    static final short DNEG = 40;
    static final short DREM = 41;
    static final short DRETURN = 42;
    static final short DSTORE = 43;
    static final short DSTORE_0 = 44;
    static final short DSTORE_1 = 45;
    static final short DSTORE_2 = 46;
    static final short DSTORE_3 = 47;
    static final short DSUB = 48;
    static final short DUP = 49;
    static final short DUP_X1 = 50;
    static final short DUP_X2 = 51;
    static final short DUP2 = 52;
    static final short DUP2_X1 = 53;
    static final short DUP2_X2 = 54;
    static final short F2D = 55;
    static final short F2I = 56;
    static final short F2L = 57;
    static final short FADD = 58;
    static final short FALOAD = 59;
    static final short FASTORE = 60;
    static final short FCMPG = 61;
    static final short FCMPL = 62;
    static final short FCONST_0 = 63;
    static final short FCONST_1 = 64;
    static final short FCONST_2 = 65;
    static final short FDIV = 66;
    static final short FLOAD = 67;
    static final short FLOAD_0 = 68;
    static final short FLOAD_1 = 69;
    static final short FLOAD_2 = 70;
    static final short FLOAD_3 = 71;
    static final short FMUL = 72;
    static final short FNEG = 73;
    static final short FREM = 74;
    static final short FRETURN = 75;
    static final short FSTORE = 76;
    static final short FSTORE_0 = 77;
    static final short FSTORE_1 = 78;
    static final short FSTORE_2 = 79;
    static final short FSTORE_3 = 80;
    static final short FSUB = 81;
    static final short GETFIELD = 82;
    static final short GETSTATIC = 83;
    static final short GOTO = 84;
    static final short GOTO_W = 85;
    static final short I2B = 86;
    static final short I2C = 87;
    static final short I2D = 88;
    static final short I2F = 89;
    static final short I2L = 90;
    static final short I2S = 91;
    static final short IADD = 92;
    static final short IALOAD = 93;
    static final short IAND = 94;
    static final short IASTORE = 95;
    static final short ICONST_0 = 96;
    static final short ICONST_1 = 97;
    static final short ICONST_2 = 98;
    static final short ICONST_3 = 99;
    static final short ICONST_4 = 100;
    static final short ICONST_5 = 101;
    static final short ICONST_M1 = 102;
    static final short IDIV = 103;
    static final short IF_ACMPEQ = 104;
    static final short IF_ACMPNE = 105;
    static final short IF_ICMPEQ = 106;
    static final short IF_ICMPGE = 107;
    static final short IF_ICMPGT = 108;
    static final short IF_ICMPLE = 109;
    static final short IF_ICMPLT = 110;
    static final short IF_ICMPNE = 111;
    static final short IFEQ = 112;
    static final short IFGE = 113;
    static final short IFGT = 114;
    static final short IFLE = 115;
    static final short IFLT = 116;
    static final short IFNE = 117;
    static final short IFNONNULL = 118;
    static final short IFNULL = 119;
    static final short IINC = 120;
    static final short ILOAD = 121;
    static final short ILOAD_0 = 122;
    static final short ILOAD_1 = 123;
    static final short ILOAD_2 = 124;
    static final short ILOAD_3 = 125;
    static final short IMUL = 126;
    static final short INEG = 127;
    static final short INSTANCEOF = 128;
    static final short INVOKEDYNAMIC = 129;
    static final short INVOKEINTERFACE = 130;
    static final short INVOKESPECIAL = 131;
    static final short INVOKESTATIC = 132;
    static final short INVOKEVIRTUAL = 133;
    static final short IOR = 134;
    static final short IREM = 135;
    static final short IRETURN = 136;
    static final short ISHL = 137;
    static final short ISHR = 138;
    static final short ISTORE = 139;
    static final short ISTORE_0 = 140;
    static final short ISTORE_1 = 141;
    static final short ISTORE_2 = 142;
    static final short ISTORE_3 = 143;
    static final short ISUB = 144;
    static final short IUSHR = 145;
    static final short IXOR = 146;
    static final short JSR = 147;
    static final short JSR_W = 148;
    static final short L2D = 149;
    static final short L2F = 150;
    static final short L2I = 151;
    static final short LADD = 152;
    static final short LALOAD = 153;
    static final short LAND = 154;
    static final short LASTORE = 155;
    static final short LCMP = 156;
    static final short LCONST_0 = 157;
    static final short LCONST_1 = 158;
    static final short LDC = 159;
    static final short LDC_W = 160;
    static final short LDC2_W = 161;
    static final short LDIV = 162;
    static final short LLOAD = 163;
    static final short LLOAD_0 = 164;
    static final short LLOAD_1 = 165;
    static final short LLOAD_2 = 166;
    static final short LLOAD_3 = 167;
    static final short LMUL = 168;
    static final short LNEG = 169;
    static final short LOOKUPSWITCH = 170;
    static final short LOR = 171;
    static final short LREM = 172;
    static final short LRETURN = 173;
    static final short LSHL = 174;
    static final short LSHR = 175;
    static final short LSTORE = 176;
    static final short LSTORE_0 = 177;
    static final short LSTORE_1 = 178;
    static final short LSTORE_2 = 179;
    static final short LSTORE_3 = 180;
    static final short LSUB = 181;
    static final short LUSHR = 182;
    static final short LXOR = 183;
    static final short MONITORENTER = 184;
    static final short MONITOREXIT = 185;
    static final short MULTIANEWARRAY = 186;
    static final short NEW = 187;
    static final short NEWARRAY = 188;
    static final short NOP = 189;
    static final short POP = 190;
    static final short POP2 = 191;
    static final short PUTFIELD = 192;
    static final short PUTSTATIC = 193;
    static final short RET = 194;
    static final short RETURN = 195;
    static final short SALOAD = 196;
    static final short SASTORE = 197;
    static final short SIPUSH = 198;
    static final short SWAP = 199;
    static final short TABLESWITCH = 200;
    static final short WIDE = 201;
  }

  /** Stub for {@code org.apache.bcel.util.ClassPath}. */
  static class ClassPath {
    /** The system class path. */
    static final ClassPath SYSTEM_CLASS_PATH = new ClassPath();

    /** Creates a ClassPath. */
    ClassPath() {}

    /**
     * Returns a stream for the given class file.
     *
     * @param name the class name, with '/' separators
     * @param suffix the file suffix
     * @return a stream for the class file
     * @throws IOException if the class file cannot be read
     */
    InputStream getInputStream(String name, String suffix) throws IOException {
      throw new IOException(name + suffix);
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ClassParser}. */
  static class ClassParser {
    /**
     * Creates a ClassParser.
     *
     * @param is the stream to parse
     * @param classname the name of the class being parsed
     */
    ClassParser(InputStream is, String classname) {}

    /**
     * Parses the class file.
     *
     * @return the parsed class
     * @throws IOException if the class file cannot be read
     */
    JavaClass parse() throws IOException {
      throw new IOException();
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.JavaClass}. */
  static class JavaClass {
    /** Creates a JavaClass. */
    JavaClass() {}

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    String getClassName() {
      return "java.lang.Object";
    }

    /**
     * Returns the constant pool.
     *
     * @return the constant pool
     */
    ConstantPool getConstantPool() {
      return new ConstantPool();
    }

    /**
     * Returns the methods.
     *
     * @return the methods
     */
    Method[] getMethods() {
      return new Method[0];
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.Method}. */
  static class Method {
    /** Creates a Method. */
    Method() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantPool}. */
  static class ConstantPool {
    /** Creates a ConstantPool. */
    ConstantPool() {}

    /**
     * Returns the constants in this pool.
     *
     * @return the constants in this pool
     */
    Constant[] getConstantPool() {
      return new Constant[0];
    }

    /**
     * Returns the constant at the given index.
     *
     * @param index an index into the pool
     * @return the constant at the given index
     */
    Constant getConstant(int index) {
      return new Constant();
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.Constant}. */
  static class Constant {
    /** Creates a Constant. */
    Constant() {}

    /**
     * Returns this constant's value.
     *
     * @param pool the constant pool
     * @return this constant's value
     */
    Object getConstantValue(ConstantPool pool) {
      return this;
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantClass}. */
  static class ConstantClass extends Constant {
    /** Creates a ConstantClass. */
    ConstantClass() {}

    /**
     * Returns the class name.
     *
     * @param pool the constant pool
     * @return the class name
     */
    String getBytes(ConstantPool pool) {
      return "java.lang.Object";
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantString}. */
  static class ConstantString extends Constant {
    /** Creates a ConstantString. */
    ConstantString() {}

    /**
     * Returns the string value.
     *
     * @param pool the constant pool
     * @return the string value
     */
    String getBytes(ConstantPool pool) {
      return "";
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantInteger}. */
  static class ConstantInteger extends Constant {
    /** Creates a ConstantInteger. */
    ConstantInteger() {}

    /**
     * Returns the int value.
     *
     * @return the int value
     */
    int getBytes() {
      return 0;
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantLong}. */
  static class ConstantLong extends Constant {
    /** Creates a ConstantLong. */
    ConstantLong() {}

    /**
     * Returns the long value.
     *
     * @return the long value
     */
    long getBytes() {
      return 0L;
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantFloat}. */
  static class ConstantFloat extends Constant {
    /** Creates a ConstantFloat. */
    ConstantFloat() {}

    /**
     * Returns the float value.
     *
     * @return the float value
     */
    float getBytes() {
      return 0f;
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantDouble}. */
  static class ConstantDouble extends Constant {
    /** Creates a ConstantDouble. */
    ConstantDouble() {}

    /**
     * Returns the double value.
     *
     * @return the double value
     */
    double getBytes() {
      return 0d;
    }
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantFieldref}. */
  static class ConstantFieldref extends Constant {
    /** Creates a ConstantFieldref. */
    ConstantFieldref() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantInterfaceMethodref}. */
  static class ConstantInterfaceMethodref extends Constant {
    /** Creates a ConstantInterfaceMethodref. */
    ConstantInterfaceMethodref() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantMethodref}. */
  static class ConstantMethodref extends Constant {
    /** Creates a ConstantMethodref. */
    ConstantMethodref() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantNameAndType}. */
  static class ConstantNameAndType extends Constant {
    /** Creates a ConstantNameAndType. */
    ConstantNameAndType() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantMethodHandle}. */
  static class ConstantMethodHandle extends Constant {
    /** Creates a ConstantMethodHandle. */
    ConstantMethodHandle() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantMethodType}. */
  static class ConstantMethodType extends Constant {
    /** Creates a ConstantMethodType. */
    ConstantMethodType() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantInvokeDynamic}. */
  static class ConstantInvokeDynamic extends Constant {
    /** Creates a ConstantInvokeDynamic. */
    ConstantInvokeDynamic() {}
  }

  /** Stub for {@code org.apache.bcel.classfile.ConstantUtf8}. */
  static class ConstantUtf8 extends Constant {
    /** Creates a ConstantUtf8. */
    ConstantUtf8() {}
  }

  /** Stub for {@code org.apache.bcel.generic.ClassGen}. */
  static class ClassGen {
    /**
     * Creates a ClassGen.
     *
     * @param jc the class
     */
    ClassGen(JavaClass jc) {}

    /**
     * Returns the constant pool.
     *
     * @return the constant pool
     */
    ConstantPoolGen getConstantPool() {
      return new ConstantPoolGen();
    }
  }

  /** Stub for {@code org.apache.bcel.generic.ConstantPoolGen}. */
  static class ConstantPoolGen {
    /** Creates a ConstantPoolGen. */
    ConstantPoolGen() {}
  }

  /** Stub for {@code org.apache.bcel.generic.MethodGen}. */
  static class MethodGen {
    /**
     * Creates a MethodGen.
     *
     * @param m the method
     * @param classname the name of the class containing the method
     * @param pool the constant pool
     */
    MethodGen(Method m, String classname, ConstantPoolGen pool) {}

    /**
     * Returns the instructions of the method, or null if it is abstract or native.
     *
     * @return the instructions of the method, or null
     */
    @Nullable InstructionList getInstructionList() {
      return null;
    }
  }

  /** Stub for {@code org.apache.bcel.generic.InstructionList}. */
  static class InstructionList {
    /** Creates an InstructionList. */
    InstructionList() {}

    /**
     * Returns the instructions.
     *
     * @return the instructions
     */
    Instruction[] getInstructions() {
      return new Instruction[0];
    }
  }

  /** Stub for {@code org.apache.bcel.generic.Instruction}. */
  static class Instruction {
    /** Creates an Instruction. */
    Instruction() {}

    /**
     * Returns this instruction's opcode.
     *
     * @return this instruction's opcode
     */
    short getOpcode() {
      return Const.NOP;
    }
  }

  /** Stub for {@code org.apache.bcel.generic.FieldInstruction}. */
  static class FieldInstruction extends Instruction {
    /** Creates a FieldInstruction. */
    FieldInstruction() {}

    /**
     * Returns the type that this instruction refers to.
     *
     * @param pool the constant pool
     * @return the type that this instruction refers to
     */
    ReferenceType getReferenceType(ConstantPoolGen pool) {
      return new ReferenceType();
    }

    /**
     * Returns the name of the field that this instruction refers to.
     *
     * @param pool the constant pool
     * @return the name of the field that this instruction refers to
     */
    String getFieldName(ConstantPoolGen pool) {
      return "";
    }
  }

  /** Stub for {@code org.apache.bcel.generic.ReferenceType}. */
  static class ReferenceType {
    /** Creates a ReferenceType. */
    ReferenceType() {}

    @Override
    public String toString() {
      return "java.lang.Object";
    }
  }

  /** Stub for {@code org.apache.bcel.generic.LDC}. */
  static class LDC extends Instruction {
    /** Creates an LDC. */
    LDC() {}

    /**
     * Returns the constant-pool index.
     *
     * @return the constant-pool index
     */
    int getIndex() {
      return 0;
    }
  }

  /** Stub for {@code org.apache.bcel.generic.LDC_W}. */
  static class LDC_W extends LDC {
    /** Creates an LDC_W. */
    LDC_W() {}
  }

  /** Stub for {@code org.apache.bcel.generic.LDC2_W}. */
  static class LDC2_W extends Instruction {
    /** Creates an LDC2_W. */
    LDC2_W() {}

    /**
     * Returns the constant-pool index.
     *
     * @return the constant-pool index
     */
    int getIndex() {
      return 0;
    }
  }

  /** Stub for {@code org.apache.bcel.generic.ConstantPushInstruction}. */
  static class ConstantPushInstruction extends Instruction {
    /** Creates a ConstantPushInstruction. */
    ConstantPushInstruction() {}

    /**
     * Returns the pushed value.
     *
     * @return the pushed value
     */
    Number getValue() {
      return 0;
    }
  }

  /** Stub for {@code randoop.main.RandoopBug}. */
  static class RandoopBug extends Error {
    /** Unique identifier for serialization. */
    private static final long serialVersionUID = 20260826L;

    /**
     * Creates a RandoopBug.
     *
     * @param message the message
     */
    RandoopBug(String message) {
      super(message);
    }

    /**
     * Creates a RandoopBug.
     *
     * @param cause the cause
     */
    RandoopBug(Throwable cause) {
      super(cause);
    }
  }

  /** Stub for {@code org.plumelib.util.MapsP}. */
  static class MapsP {
    /** Creates a MapsP. */
    MapsP() {}

    /**
     * Increments the value associated with the given key.
     *
     * @param m the map
     * @param key the key
     */
    static void incrementMap(Map<Object, Integer> m, Object key) {
      m.put(key, m.getOrDefault(key, 0) + 1);
    }
  }
}
