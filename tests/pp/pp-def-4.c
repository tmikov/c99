#define INC(x)  ((x)+1)
INC( a   b +    c    )

#define ADD(a,b)  ( ( a )   +   ( b )  )

ADD( 1+2, 3+a )

#define DOSTR(x) \
  printf("%s\n", #x);\
  x

DOSTR( a )
DOSTR( exit(1) );
DOSTR( printf( "Hello\n" ) );
DOSTR();
