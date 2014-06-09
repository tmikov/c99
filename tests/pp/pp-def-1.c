#define MAX_COUNT 100
#define MAX_COUNT 100
#define MAX_COUNT 200

#define BAD1(
#define BAD2(+
#define BAD3(a,
#define BAD4(...
#define BAD5(a,a)
#define GOOD5(a,b)  (a)+(b)

#define BAD6 ##
#define BAD7 a ##
#define GOOD7(a) a ## 1

#define BAD8 # p
#define GOOD8(p) # p

#define BAD9(p)  __VA_ARGS__
#define GOOD9(p,...)   printf( p, __VA_ARGS__ )
