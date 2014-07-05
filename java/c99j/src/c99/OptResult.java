package c99;

public class OptResult<E>
{
private final E m_res;

public OptResult ( E e )
{
  m_res = e;
}

public E get ()
{
  return m_res;
}

@SuppressWarnings("unchecked")
public static <E> OptResult<E> nullValue ()
{
  return s_null;
}

@SuppressWarnings("unchecked")
private static final OptResult s_null = new OptResult(null);

@SuppressWarnings("unchecked")
public static <E> OptResult<E> fail ()
{
  return null;
}

}
