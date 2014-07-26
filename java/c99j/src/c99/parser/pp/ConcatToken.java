package c99.parser.pp;

import java.io.IOException;
import java.io.OutputStream;

import c99.parser.Code;

/**
 * A '##' between two tokens.
 *
 * It is left associative, meaning that multiple '##'-s aways generate a left tree.
 * 'a ## b ## c' always produces
 * <p>{@code concat( concat( a, b ), c )}
 */
final class ConcatToken extends PPDefs.AbstractToken
{
final PPDefs.AbstractToken tokens[];

public ConcatToken ( PPDefs.AbstractToken left, PPDefs.AbstractToken right )
{
  assert !(right instanceof ConcatToken);

  m_code = Code.CONCAT;
  if (!(left instanceof ConcatToken))
    this.tokens = new PPDefs.AbstractToken[]{ left, right };
  else
  {
    final ConcatToken lt = (ConcatToken)left;
    final int len = lt.tokens.length;
    this.tokens = new PPDefs.AbstractToken[len+1];
    System.arraycopy( lt.tokens, 0, this.tokens, 0, len );
    this.tokens[len] = right;
  }
}

ConcatToken ( PPDefs.AbstractToken[] tokens )
{
  this.tokens = tokens;
}

@SuppressWarnings("CloneDoesntCallSuperClone")
@Override
public ConcatToken clone ()
{
  PPDefs.AbstractToken[] t = new PPDefs.AbstractToken[this.tokens.length];
  for ( int i = 0; i < this.tokens.length; ++i )
    t[i] = this.tokens[i].clone();
  ConcatToken res = new ConcatToken( t );
  res.setRange( this );
  return res;
}

@Override
public boolean same ( final PPDefs.AbstractToken tok )
{
  if (tok.m_code != m_code)
    return false;
  ConcatToken t = (ConcatToken)tok;
  if (this.tokens.length != t.tokens.length)
    return false;
  for ( int i = 0; i < tokens.length; ++i )
    if (!this.tokens[i].same( t.tokens[i] ))
      return false;
  return true;
}

@Override
public int length ()
{
  int len = 0;
  for ( PPDefs.AbstractToken tok : this.tokens )
    len += tok.length();
  return len;
}

@Override
public String toString ()
{
  StringBuilder b = new StringBuilder();
  b.append(  "ConcatToken{" );
  for ( int i = 0; i < this.tokens.length; ++i )
  {
    if (i > 0)
      b.append( ", " );
    b.append( i ).append( '=' ).append( this.tokens[i].toString() );
  }
  b.append( '}' );
  return b.toString();
}

private static final byte[] s_text = " ## ".getBytes();
@Override
public void output ( final OutputStream out ) throws IOException
{
  for ( int i = 0; i < this.tokens.length; ++i )
  {
    if (i > 0)
      out.write( s_text );
    this.tokens[i].output( out );
  }
}
}
