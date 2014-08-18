int __attribute__((x86far(1))) x1;
int __attribute__((x86far)) x2;
int __attribute__(("x86far")) x3;
int __attribute__(("__x86far__")) x3;
int __attribute__((__x86far__)) x4;

#define far    __attribute__((x86far))
#define cdecl  __attribute__((cdecl))


int far * ptr;
void far cdecl strlen ( void );

int far f1;
int f1;
