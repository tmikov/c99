package c99;

import org.junit.Test;

import static c99.Constant.makeLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConstantTest
{

private void add ( long a, long b, long res, TypeSpec spec )
{
  Constant.IntC rc = Constant.newIntConstant( spec );

  rc.add( makeLong( spec, a ), makeLong( spec, b ) );
  assertEquals( "add", makeLong( spec, res ), rc );
}

private void sub ( long a, long b, long res, TypeSpec spec )
{
  Constant.IntC rc = Constant.newIntConstant( spec );

  rc.sub( makeLong( spec, a ), makeLong( spec, b ) );
  assertEquals( "sub", makeLong( spec, res ), rc );
}

private void shr ( long a, long b, long res, TypeSpec spec )
{
  Constant.IntC rc = Constant.newIntConstant( spec );

  rc.shr( makeLong( spec, a ), makeLong( TypeSpec.UINT, b ) );
  assertEquals( "shr", makeLong( spec, res ), rc );
}

private void div ( long a, long b, long res, TypeSpec spec )
{
  Constant.IntC rc = Constant.newIntConstant( spec );

  rc.div( makeLong( spec, a ), makeLong( spec, b ) );
  assertEquals( "div", makeLong( spec, res ), rc );
}

private void rem ( long a, long b, long res, TypeSpec spec )
{
  Constant.IntC rc = Constant.newIntConstant( spec );

  rc.rem( makeLong( spec, a ), makeLong( spec, b ) );
  assertEquals( "rem", makeLong( spec, res ), rc );
}

@Test
public void testNewConstant () throws Exception
{
  add( 10, 20, 30, TypeSpec.UINT );
  add( 10, 20, 30, TypeSpec.ULLONG );
  add( 0xFFFFFFFFFFFFFFF0L, 0x01, 0xFFFFFFFFFFFFFFF1L, TypeSpec.ULLONG );

  add( 255, 1, 0, TypeSpec.UCHAR );
  sub( 0, 1, 255, TypeSpec.UCHAR );

  add( 127, 1, -128, TypeSpec.SCHAR );
  sub( 0, 1, -1, TypeSpec.SCHAR );

  shr( 0xFFFFFFFFFFFFFFF0L, 0x04, 0x0FFFFFFFFFFFFFFFL, TypeSpec.ULLONG );
  shr( 0xFFFFFFFFFFFFFFF0L, 0x04, 0xFFFFFFFFFFFFFFFFL, TypeSpec.SLLONG );

  div( 0xFFFFFFFFFFFFFFF0L, 16, 0x0FFFFFFFFFFFFFFFL, TypeSpec.ULLONG );
  rem( 0xFFFFFFFFFFFFFFF0L, 16, 0, TypeSpec.ULLONG );

  div( 0xFFFFFFFFFFFFFFF1L, 16, 0x0FFFFFFFFFFFFFFFL, TypeSpec.ULLONG );
  rem( 0xFFFFFFFFFFFFFFF1L, 16, 1, TypeSpec.ULLONG );

  assertTrue( makeLong( TypeSpec.UINT, 10 ).eq(  makeLong( TypeSpec.UINT, 10 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 10 ).ne(  makeLong( TypeSpec.UINT, 20 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 10 ).lt(  makeLong( TypeSpec.UINT, 20 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 10 ).le(  makeLong( TypeSpec.UINT, 20 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 10 ).le(  makeLong( TypeSpec.UINT, 10 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 20 ).gt(  makeLong( TypeSpec.UINT, 10 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 20 ).ge(  makeLong( TypeSpec.UINT, 10 ) ) );
  assertTrue( makeLong( TypeSpec.UINT, 20 ).ge(  makeLong( TypeSpec.UINT, 20 ) ) );

  assertTrue( makeLong( TypeSpec.UINT, -1 ).gt(  makeLong( TypeSpec.UINT, 1 ) ) );
  assertTrue( makeLong( TypeSpec.SINT, -1 ).lt(  makeLong( TypeSpec.SINT, 1 ) ) );

  assertTrue( makeLong( TypeSpec.ULLONG, -1 ).gt(  makeLong( TypeSpec.ULLONG, 1 ) ) );
  assertTrue( makeLong( TypeSpec.SLLONG, -1 ).lt(  makeLong( TypeSpec.SLLONG, 1 ) ) );
}
} // class

