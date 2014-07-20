package c99.parser.pp;

import java.io.IOException;
import java.io.OutputStream;

final class ParamToken extends PPDefs.AbstractToken
{
public final ParamDecl param;
public boolean stringify;

ParamToken ( final ParamDecl param )
{
  m_code = PPDefs.Code.MACRO_PARAM;
  this.param = param;
}

@SuppressWarnings("CloneDoesntCallSuperClone")
@Override
public ParamToken clone ()
{
  ParamToken res = new ParamToken( this.param );
  res.setRange( this );
  return res;
}

@Override
public boolean same ( final PPDefs.AbstractToken tok )
{
  return this.m_code == tok.m_code && this.param.same( ((ParamToken)tok).param );
}

@Override
public int length ()
{
  return this.param.symbol.length();
}

@Override
public void output ( final OutputStream out ) throws IOException
{
  out.write(this.param.symbol.bytes);
}
}
