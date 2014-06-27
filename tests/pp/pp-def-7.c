#define NEG(x)  (-(x))
#define ABS(x)  ((x) < 0 ? NEG(x) : (x))

ABS(1+a);

#define f(a) a*g
#define g(a) f(a)

f(2) (9)

#define REC(x)  x+REC(x-1)

REC(10)