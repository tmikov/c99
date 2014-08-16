#define asm __asm__

asm("nop");

static inline int add ( int a, int b )
{
  int res = a;
  asm ("addl %[var],%0" : "+r" (res) : [var] "r" (b));
  return res;
}

int func ( int a, int b )
{
  return add(a, b)+1;
}