package ca.uwaterloo.flix.runtime

import ca.uwaterloo.flix.api._
import ca.uwaterloo.flix.language.ast.Symbol
import ca.uwaterloo.flix.util.{DebugBytecode, _}
import org.scalatest.FunSuite

class TestBackend extends FunSuite {

  private object HookSafeHelpers {
    case class MyObject(x: Int)

    implicit def f0h(f: Function0[IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f()
    }
    implicit def f1h(f: Function1[IValue,IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f(args(0))
    }
    implicit def f2h(f: Function2[IValue,IValue,IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f(args(0), args(1))
    }
    implicit def f3h(f: Function3[IValue,IValue,IValue,IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f(args(0), args(1), args(2))
    }
    implicit def f4h(f: Function4[IValue,IValue,IValue,IValue,IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f(args(0), args(1), args(2), args(3))
    }
    implicit def f5h(f: Function5[IValue,IValue,IValue,IValue,IValue,IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f(args(0), args(1), args(2), args(3), args(4))
    }
    implicit def f6h(f: Function6[IValue,IValue,IValue,IValue,IValue,IValue,IValue]): Invokable = new Invokable {
      override def apply(args: Array[IValue]): IValue = f(args(0), args(1), args(2), args(3), args(4), args(5))
    }
  }

  private object HookUnsafeHelpers {
    type JBool = java.lang.Boolean
    type JChar = java.lang.Character
    type JFloat = java.lang.Float
    type JDouble = java.lang.Double
    type JByte = java.lang.Byte
    type JShort = java.lang.Short
    type JInt = java.lang.Integer
    type JLong = java.lang.Long

    case class MyObject(x: Int)

    implicit def f0h[R <: AnyRef](f: Function0[R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
      )
    }
    implicit def f1h[P0,R <: AnyRef](f: Function1[P0,R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
        args(0).asInstanceOf[P0]
      )
    }
    implicit def f2h[P0,P1,R <: AnyRef](f: Function2[P0,P1,R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
        args(0).asInstanceOf[P0],
        args(1).asInstanceOf[P1]
      )
    }
    implicit def f3h[P0,P1,P2,R <: AnyRef](f: Function3[P0,P1,P2,R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
        args(0).asInstanceOf[P0],
        args(1).asInstanceOf[P1],
        args(2).asInstanceOf[P2]
      )
    }
    implicit def f4h[P0,P1,P2,P3,R <: AnyRef](f: Function4[P0,P1,P2,P3,R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
        args(0).asInstanceOf[P0],
        args(1).asInstanceOf[P1],
        args(2).asInstanceOf[P2],
        args(3).asInstanceOf[P3]
      )
    }
    implicit def f5h[P0,P1,P2,P3,P4,R <: AnyRef](f: Function5[P0,P1,P2,P3,P4,R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
        args(0).asInstanceOf[P0],
        args(1).asInstanceOf[P1],
        args(2).asInstanceOf[P2],
        args(3).asInstanceOf[P3],
        args(4).asInstanceOf[P4]
      )
    }
    implicit def f6h[P0,P1,P2,P3,P4,P5,R <: AnyRef](f: Function6[P0,P1,P2,P3,P4,P5,R]): InvokableUnsafe = new InvokableUnsafe {
      override def apply(args: Array[AnyRef]): AnyRef = f(
        args(0).asInstanceOf[P0],
        args(1).asInstanceOf[P1],
        args(2).asInstanceOf[P2],
        args(3).asInstanceOf[P3],
        args(4).asInstanceOf[P4],
        args(5).asInstanceOf[P5]
      )
    }
  }

  private class Tester(input: String, dumpBytecode: Boolean = false) {
    private def getModel(codegen: Boolean) = {
      val options = Options(
        debugger = Debugger.Disabled,
        print = Nil,
        verbosity = Verbosity.Silent,
        verify = Verify.Disabled,
        codegen = if (codegen) CodeGeneration.Enabled else CodeGeneration.Disabled,
        debugBytecode = if (dumpBytecode) DebugBytecode.Enabled else DebugBytecode.Disabled
      )
      new Flix().setOptions(options).addStr(input).solve().get
    }

    def runTest(expected: AnyRef, const: String): Unit = {
      assertResult(expected, s"- interpreter produced wrong value for $const")(interpreted.getConstant(const))
      assertResult(expected, s"- compiler produced wrong value for $const")(compiled.getConstant(const))
    }

    def runInterceptTest[T <: AnyRef](const:String)(implicit manifest: Manifest[T]): Unit = {
      withClue(s"interpreted value $const:") { intercept[T](interpreted.getConstant(const)) }
      withClue(s"compiled value $const:") { intercept[T](compiled.getConstant(const)) }
    }

    val interpreted = getModel(codegen = false)
    val compiled = getModel(codegen = true)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expression.{Unit,Bool,Char,Float32,Float64,Int8,Int16,Int32,Int64,Str}  //
  /////////////////////////////////////////////////////////////////////////////

  test("Expression.Unit") {
    val input = "def f: () = ()"
    val t = new Tester(input)
    t.runTest(Value.Unit, "f")
  }

  test("Expression.Bool.01") {
    val input = "def f: Bool = true"
    val t = new Tester(input)
    t.runTest(Value.True, "f")
  }

  test("Expression.Bool.02") {
    val input = "def f: Bool = false"
    val t = new Tester(input)
    t.runTest(Value.False, "f")
  }

  test("Expression.Char.01") {
    val input = "def f: Char = 'a'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('a'), "f")
  }

  test("Expression.Char.02") {
    val input = "def f: Char = '0'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('0'), "f")
  }

  test("Expression.Char.03") {
    // Minimum character value (NUL)
    val input = s"def f: Char = '${'\u0000'}'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('\u0000'), "f")
  }

  test("Expression.Char.04") {
    // Non-printable ASCII character DEL
    val input = s"def f: Char = '${'\u007f'}'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('\u007f'), "f")
  }

  test("Expression.Char.05") {
    // Maximum character value
    val input = s"def f: Char = '${'\uffff'}'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('\uffff'), "f")
  }

  test("Expression.Char.06") {
    // Chinese character for the number "ten"
    val input = s"def f: Char = '${'十'}'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('十'), "f")
  }

  test("Expression.Char.07") {
    // Zero-width space
    val input = s"def f: Char = '${'\u200b'}'"
    val t = new Tester(input)
    t.runTest(Value.mkChar('\u200b'), "f")
  }

  // TODO: More tests when we get the syntax for exponents. More tests when we have standard library (NaN, +/infinity).
  // See JLS 3.10.2:
  //   The largest positive finite literal of type float is 3.4028235e38f.
  //   The smallest positive finite non-zero literal of type float is 1.40e-45f.
  //   The largest positive finite literal of type double is 1.7976931348623157e308.
  //   The smallest positive finite non-zero literal of type double is 4.9e-324.

  test("Expression.Float.01") {
    val input = "def f: Float = 0.0"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(0.0), "f")
  }

  test("Expression.Float.02") {
    val input = "def f: Float = -0.0"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(-0.0), "f")
  }

  test("Expression.Float.03") {
    val input = "def f: Float = 4.2"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(4.2), "f")
  }

  test("Expression.Float.04") {
    val input = "def f: Float = 99999999999999999999999999999999999999999999999999999999999999999999999999999999.0"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(99999999999999999999999999999999999999999999999999999999999999999999999999999999.0), "f")
  }

  test("Expression.Float.05") {
    val input = "def f: Float = 0.000000000000000000000000000000000000000000000000000000000000000000000000000000001"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(0.000000000000000000000000000000000000000000000000000000000000000000000000000000001), "f")
  }

  test("Expression.Float.06") {
    val input = "def f: Float = -99999999999999999999999999999999999999999999999999999999999999999999999999999999.0"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(-99999999999999999999999999999999999999999999999999999999999999999999999999999999.0), "f")
  }

  test("Expression.Float.07") {
    val input = "def f: Float = -0.000000000000000000000000000000000000000000000000000000000000000000000000000000001"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(-0.000000000000000000000000000000000000000000000000000000000000000000000000000000001), "f")
  }

  /*
   * Note that there are specific bytecode instructions for constants 0.0f, 1.0f, and 2.0f.
   */

  test("Expression.Float32.01") {
    val input = "def f: Float32 = 0.0f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(0.0f), "f")
  }

  test("Expression.Float32.02") {
    val input = "def f: Float32 = -0.0f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(-0.0f), "f")
  }

  test("Expression.Float32.03") {
    val input = "def f: Float32 = 1.0f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(1.0f), "f")
  }

  test("Expression.Float32.04") {
    val input = "def f: Float32 = 2.0f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(2.0f), "f")
  }

  test("Expression.Float32.05") {
    val input = "def f: Float32 = 4.2f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(4.2f), "f")
  }

  test("Expression.Float32.06") {
    val input = "def f: Float32 = 999999999999999999999999999999.0f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(999999999999999999999999999999.0f), "f")
  }

  test("Expression.Float32.07") {
    val input = "def f: Float32 = 0.0000000000000000000000000000001f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(0.0000000000000000000000000000001f), "f")
  }

  test("Expression.Float32.08") {
    val input = "def f: Float32 = -999999999999999999999999999999.0f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(-999999999999999999999999999999.0f), "f")
  }

  test("Expression.Float32.09") {
    val input = "def f: Float32 = -0.0000000000000000000000000000001f32"
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(-0.0000000000000000000000000000001f), "f")
  }

  /*
   * Note that there are specific bytecode instructions for constants 0.0d and 1.0d.
   */

  test("Expression.Float64.01") {
    val input = "def f: Float64 = 0.0f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(0.0d), "f")
  }

  test("Expression.Float64.02") {
    val input = "def f: Float64 = -0.0f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(-0.0d), "f")
  }

  test("Expression.Float64.03") {
    val input = "def f: Float64 = 1.0f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(1.0d), "f")
  }

  test("Expression.Float64.04") {
    val input = "def f: Float64 = 2.0f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(2.0d), "f")
  }

  test("Expression.Float64.05") {
    val input = "def f: Float64 = 4.2f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(4.2d), "f")
  }

  test("Expression.Float64.06") {
    val input = "def f: Float64 = 99999999999999999999999999999999999999999999999999999999999999999999999999999999.0f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(99999999999999999999999999999999999999999999999999999999999999999999999999999999.0d), "f")
  }

  test("Expression.Float64.07") {
    val input = "def f: Float64 = 0.000000000000000000000000000000000000000000000000000000000000000000000000000000001f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(0.000000000000000000000000000000000000000000000000000000000000000000000000000000001d), "f")
  }

  test("Expression.Float64.08") {
    val input = "def f: Float64 = -99999999999999999999999999999999999999999999999999999999999999999999999999999999.0f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(-99999999999999999999999999999999999999999999999999999999999999999999999999999999.0d), "f")
  }

  test("Expression.Float64.09") {
    val input = "def f: Float64 = -0.000000000000000000000000000000000000000000000000000000000000000000000000000000001f64"
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(-0.000000000000000000000000000000000000000000000000000000000000000000000000000000001d), "f")
  }

  /*
   * Note that there are specific bytecode instructions for the constants -1 to 5, inclusive.
   */

  test("Expression.Int.01") {
    val input = "def f: Int = 0"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(0), "f")
  }

  test("Expression.Int.02") {
    val input = "def f: Int = -1"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(-1), "f")
  }

  test("Expression.Int.03") {
    val input = "def f: Int = 1"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(1), "f")
  }

  test("Expression.Int.04") {
    val input = "def f: Int = 5"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(5), "f")
  }

  test("Expression.Int.05") {
    val input = "def f: Int = -254542"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(-254542), "f")
  }

  test("Expression.Int.06") {
    val input = "def f: Int = 45649878"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(45649878), "f")
  }

  test("Expression.Int.07") {
    val input = s"def f: Int = ${Int.MaxValue}"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(Int.MaxValue), "f")
  }

  test("Expression.Int.08") {
    val input = s"def f: Int = ${Int.MinValue}"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(Int.MinValue), "f")
  }

  /*
   * Note that there is a specific bytecode instruction (BIPUSH) for pushing bytes
   * (that aren't handled by the -1 to 5 constant instructions).
   */

  test("Expression.Int8.01") {
    val input = "def f: Int8 = -105i8"
    val t = new Tester(input)
    t.runTest(Value.mkInt8(-105), "f")
  }

  test("Expression.Int8.02") {
    val input = "def f: Int8 = 121i8"
    val t = new Tester(input)
    t.runTest(Value.mkInt8(121), "f")
  }

  test("Expression.Int8.03") {
    val input = "def f: Int8 = -2i8"
    val t = new Tester(input)
    t.runTest(Value.mkInt8(-2), "f")
  }

  test("Expression.Int8.04") {
    val input = "def f: Int8 = 6i8"
    val t = new Tester(input)
    t.runTest(Value.mkInt8(6), "f")
  }

  test("Expression.Int8.05") {
    val input = s"def f: Int8 = ${Byte.MaxValue}i8"
    val t = new Tester(input)
    t.runTest(Value.mkInt8(Byte.MaxValue), "f")
  }

  test("Expression.Int8.06") {
    val input = s"def f: Int8 = ${Byte.MinValue}i8"
    val t = new Tester(input)
    t.runTest(Value.mkInt8(Byte.MinValue), "f")
  }

  /*
   * Note that there is a specific bytecode instruction (SIPUSH) for pushing shorts (that aren't handled by BIPUSH).
   */

  test("Expression.Int16.01") {
    val input = "def f: Int16 = -5320i16"
    val t = new Tester(input)
    t.runTest(Value.mkInt16(-5320), "f")
  }

  test("Expression.Int16.02") {
    val input = "def f: Int16 = 4568i16"
    val t = new Tester(input)
    t.runTest(Value.mkInt16(4568), "f")
  }

  test("Expression.Int16.03") {
    val input = s"def f: Int16 = ${Byte.MinValue - 1}i16"
    val t = new Tester(input)
    t.runTest(Value.mkInt16(Byte.MinValue - 1), "f")
  }

  test("Expression.Int16.04") {
    val input = s"def f: Int16 = ${Byte.MaxValue + 1}i16"
    val t = new Tester(input)
    t.runTest(Value.mkInt16(Byte.MaxValue + 1), "f")
  }

  test("Expression.Int16.05") {
    val input = s"def f: Int16 = ${Short.MaxValue}i16"
    val t = new Tester(input)
    t.runTest(Value.mkInt16(Short.MaxValue), "f")
  }

  test("Expression.Int16.06") {
    val input = s"def f: Int16 = ${Short.MinValue}i16"
    val t = new Tester(input)
    t.runTest(Value.mkInt16(Short.MinValue), "f")
  }

  /*
   * Larger int constants need to be loaded with LDC.
   */

  test("Expression.Int32.01") {
    val input = "def f: Int32 = -254542i32"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(-254542), "f")
  }

  test("Expression.Int32.02") {
    val input = "def f: Int32 = 45649878i32"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(45649878), "f")
  }

  test("Expression.Int32.03") {
    val input = s"def f: Int32 = ${Short.MinValue - 1}i32"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(Short.MinValue - 1), "f")
  }

  test("Expression.Int32.04") {
    val input = s"def f: Int32 = ${Short.MaxValue + 1}i32"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(Short.MaxValue + 1), "f")
  }

  test("Expression.Int32.05") {
    val input = s"def f: Int32 = ${Int.MaxValue}i32"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(Int.MaxValue), "f")
  }

  test("Expression.Int32.06") {
    val input = s"def f: Int32 = ${Int.MinValue}i32"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(Int.MinValue), "f")
  }

  /*
   * Note that there are specific bytecode instructions for the constants 0l and 1l.
   */

  test("Expression.Int64.01") {
    val input = "def f: Int64 = -254454121542i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(-254454121542L), "f")
  }

  test("Expression.Int64.02") {
    val input = "def f: Int64 = 45641198784545i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(45641198784545L), "f")
  }

  test("Expression.Int64.03") {
    val input = s"def f: Int64 = ${Int.MinValue - 1}i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(Int.MinValue - 1), "f")
  }

  test("Expression.Int64.04") {
    val input = s"def f: Int64 = ${Int.MaxValue + 1}i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(Int.MaxValue + 1), "f")
  }

  test("Expression.Int64.05") {
    val input = s"def f: Int64 = ${Long.MaxValue}i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(Long.MaxValue), "f")
  }

  test("Expression.Int64.06") {
    val input = s"def f: Int64 = ${Long.MinValue}i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(Long.MinValue), "f")
  }

  test("Expression.Int64.07") {
    val input = "def f: Int64 = 0i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(0L), "f")
  }

  test("Expression.Int64.08") {
    val input = "def f: Int64 = 1i64"
    val t = new Tester(input)
    t.runTest(Value.mkInt64(1L), "f")
  }

  test("Expression.Str.01") {
    val input = """def f: Str = """""
    val t = new Tester(input)
    t.runTest(Value.mkStr(""), "f")
  }

  test("Expression.Str.02") {
    val input = """def f: Str = "Hello World!""""
    val t = new Tester(input)
    t.runTest(Value.mkStr("Hello World!"), "f")
  }

  test("Expression.Str.03") {
    val input = """def f: Str = "asdf""""
    val t = new Tester(input)
    t.runTest(Value.mkStr("asdf"), "f")
  }

  /////////////////////////////////////////////////////////////////////////////
  // LoadExpression and StoreExpression                                      //
  /////////////////////////////////////////////////////////////////////////////

  // TODO: LoadExpression and StoreExpression tests.
  // {Load,Store}Expressions are generated, and not explicitly written in a Flix program

  /////////////////////////////////////////////////////////////////////////////
  // Expression.Var                                                          //
  // Tested indirectly by Expression.{Lambda,Let}.                           //
  /////////////////////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////////////////////////
  // Expression.Ref                                                          //
  /////////////////////////////////////////////////////////////////////////////

  test("Expression.Ref.01") {
    val input =
      """namespace Foo.Bar {
        |  def x: Bool = false
        |  def f: Str = "foo"
        |}
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkStr("foo"), "Foo.Bar/f")
  }

  test("Expression.Ref.02") {
    val input =
      """namespace Foo {
        |  def x: Int = 5
        |  def f: Int = x
        |}
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(5), "Foo/f")
  }

  test("Expression.Ref.03") {
    val input =
      """namespace Foo {
        |  def x: Bool = true
        |  def y: Bool = false
        |  def f: Bool = y
        |}
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.False, "Foo/f")
  }

  test("Expression.Ref.04") {
    val input =
      """namespace Foo {
        |  def x: Str = "hello"
        |}
        |namespace Bar {
        |  def x: Str = Foo/x
        |}
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkStr("hello"), "Bar/x")
  }

  test("Expression.Ref.05") {
    val input = "def x: Int = 42"
    val t = new Tester(input)
    t.runTest(Value.mkInt32(42), "x")
  }

  test("Expression.Ref.06") {
    val input =
      """namespace A.B {
        |  def a: Bool = false
        |}
        |namespace A {
        |  def b: Bool = !A.B/a
        |}
        |namespace A {
        |  namespace B {
        |    def c: Int = 0
        |
        |    namespace C {
        |      def d: Int = 42
        |    }
        |  }
        |}
        |def e: Int = -1
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.False, "A.B/a")
    t.runTest(Value.True, "A/b")
    t.runTest(Value.mkInt32(0), "A.B/c")
    t.runTest(Value.mkInt32(42), "A.B.C/d")
    t.runTest(Value.mkInt32(-1), "e")
  }

  /////////////////////////////////////////////////////////////////////////////
  // Lambdas - Expression.{MkClosureRef,ApplyRef,ApplyClosure}               //
  // Note that closure conversion and lambda lifting means we don't actually //
  // have lambdas in the AST. A lot of functionality is tested indirectly    //
  // by pattern matching.                                                    //
  /////////////////////////////////////////////////////////////////////////////

  // TODO: More tests when the typer handles lambda expressions.
  // Test actual lambda expressions (not just top-level definitions): passing them around, free variables, etc.

  test("Expression.Lambda.01") {
    val input =
      """namespace A.B {
        |  def f: Bool = false
        |}
        |namespace A {
        |  def g: Bool = A.B/f
        |}
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.False, "A/g")
  }

  test("Expression.Lambda.02") {
    val input =
      """namespace A { def f(x: Int): Int = 24 }
        |def g: Int = A/f(3)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(24), "g")
  }

  test("Expression.Lambda.03") {
    val input =
      """namespace A { def f(x: Int): Int = x }
        |namespace A { def g: Int = f(3) }
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(3), "A/g")
  }

  test("Expression.Lambda.04") {
    val input =
      """def f(x: Int64, y: Int64): Int64 = x * y - 6i64
        |def g: Int64 = f(3i64, 42i64)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(120), "g")
  }

  test("Expression.Lambda.05") {
    val input =
      """namespace A { def f(x: Int32): Int32 = let y = B/g(x + 1i32) in y * y }
        |namespace B { def g(x: Int32): Int32 = x - 4i32 }
        |namespace C { def h: Int32 = A/f(5i32) + B/g(0i32) }
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(0), "C/h")
  }

  test("Expression.Lambda.06") {
    val input =
      """def f(x: Int16): Int16 = g(x + 1i16)
        |def g(x: Int16): Int16 = h(x + 10i16)
        |def h(x: Int16): Int16 = x * x
        |def x: Int16 = f(3i16)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(196), "x")
  }

  test("Expression.Lambda.07") {
    val input =
      """def f(x: Int8, y: Int8): Int8 = x - y
        |def g(x: Int8): Int8 = x * 3i8
        |def h(x: Int8): Int8 = g(x - 1i8)
        |def x: Int8 = let x = 7i8 in f(g(3i8), h(h(x)))
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(-42), "x")
  }

  test("Expression.Lambda.08") {
    val input =
      """def f(x: Bool, y: Bool): Bool = if (x) true else y
        |def g01: Bool = f(true, true)
        |def g02: Bool = f(true, false)
        |def g03: Bool = f(false, false)
        |def g04: Bool = f(false, true)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.True, "g01")
    t.runTest(Value.True, "g02")
    t.runTest(Value.False, "g03")
    t.runTest(Value.True, "g04")
  }

  test("Expression.Lambda.09") {
    val input =
      """def f(x: Bool, y: Bool): Bool = if (x) y else false
        |def g01: Bool = f(true, true)
        |def g02: Bool = f(true, false)
        |def g03: Bool = f(false, false)
        |def g04: Bool = f(false, true)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.True, "g01")
    t.runTest(Value.False, "g02")
    t.runTest(Value.False, "g03")
    t.runTest(Value.False, "g04")
  }

  test("Expression.Lambda.10") {
    val input =
      """def f(x: Int, y: Int, z: Int): Int = x + y + z
        |def g: Int = f(2, 42, 5)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(49), "g")
  }

  test("Expression.Lambda.11") {
    val input =
      """def f(x: (Int) -> Int, y: Int): Int = x(y)
        |def g(x: Int): Int = x + 1
        |def h: Int = f(g, 5)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(6), "h")
  }

  test("Expression.Lambda.12") {
    val input =
      """def f(x: (Int) -> Int): (Int) -> Int = x
        |def g(x: Int): Int = x + 5
        |def h: Int = (f(g))(40)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkInt32(45), "h")
  }

  test("Expression.Lambda.13") {
    val input =
      """enum Val { case Val(Int) }
        |def f(x: Int): Val = Val.Val(x)
        |def g: Val = f(111)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkTag(Symbol.Resolved.mk("Val"), "Val", Value.mkInt32(111)), "g")
  }

  test("Expression.Lambda.14") {
    val input =
      """def f(a: Int, b: Int, c: Str, d: Int, e: Bool, f: ()): (Int, Int, Str, Int, Bool, ()) = (a, b, c, d, e, f)
        |def g: (Int, Int, Str, Int, Bool, ()) = f(24, 53, "qwertyuiop", 9978, false, ())
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.Tuple(Array(Value.mkInt32(24), Value.mkInt32(53), Value.mkStr("qwertyuiop"), Value.mkInt32(9978), Value.False, Value.Unit)), "g")
  }

  test("Expression.Lambda.15") {
    val input =
      """def f(a: Int, b: Int, c: Int): Set[Int] = #{a, b, c}
        |def g: Set[Int] = f(24, 53, 24)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkSet(Set(Value.mkInt32(24), Value.mkInt32(53), Value.mkInt32(24))), "g")
  }

  test("Expression.Lambda.17") {
    val input =
      """def f(a: Char, b: Char): Bool = a == b
        |def g: Bool = f('a', 'b')
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.False, "g")
  }

  test("Expression.Lambda.18") {
    val input =
      """def f(a: Float32, b: Float32): Float32 = a + b
        |def g: Float32 = f(1.2f32, 2.1f32)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkFloat32(3.3f), "g")
  }

  test("Expression.Lambda.19") {
    val input =
      """def f(a: Float64, b: Float64): Float64 = a + b
        |def g: Float64 = f(1.2f64, 2.1f64)
      """.stripMargin
    val t = new Tester(input)
    t.runTest(Value.mkFloat64(3.3d), "g")
  }

}
