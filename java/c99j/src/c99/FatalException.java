package c99;

public class FatalException extends RuntimeException
{
public FatalException ()
{
}

public FatalException ( final String message )
{
  super( message );
}

public FatalException ( final String message, final Throwable cause )
{
  super( message, cause );
}

public FatalException ( final Throwable cause )
{
  super( cause );
}
} // class
