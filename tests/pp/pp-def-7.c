#define NEG(x)  (-(x))
#define ABS(x)  ((x) < 0 ? NEG(x) : (x))

ABS(1+a);

#define f(a) a*g
#define g(a) f(a)

f(2) (9)

#define REC(x)  x+REC(x-1)

REC(10); REC(11);

NEG(NEG(1));
NEG(ABS(NEG(1)));

#define RR( x )  x + RR

RR(1);
RR(2)(3);

#define RR2      1 + RR2
#define MM(x)    x

RR2;
MM(RR2);

#define M1   "M1"+M2
#define M2   "M2"+M1

M1
M2
