package c99.parser;

public final class ParamScope extends Scope
{
private boolean m_ellipsis;

public ParamScope ( Scope parent )
{
  super( Kind.PARAM, parent );
}

public final boolean getEllipsis ()
{
  assert this.kind == Kind.PARAM;
  return m_ellipsis;
}

public final void setEllipsis ()
{
  assert this.kind == Kind.PARAM;
  m_ellipsis = true;
}
}
