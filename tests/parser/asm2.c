#define asm __asm__

int x asm("myvar") = 100;


int func ( void ) asm("myfunc");
int func ( void )
{
  register int * foo asm("%edx") = 0;
  return *foo;
}
