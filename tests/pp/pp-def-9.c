#define CAT2(a,b)  a ## b
#define CAT(a,b)   CAT2(a, b )

CAT2( x, __LINE__ );
CAT( y, __LINE__ );


