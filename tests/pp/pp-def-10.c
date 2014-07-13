#define PAR      (x)
#define NEG(x)   (-(x))
#define MM(p)    NEG p
#define MMM(p)   p
#define TT       NEG PAR

NEG(x)
NEG PAR
MM(PAR)
MMM( NEG PAR )
TT
