#define far    __attribute__((far))
#define cdecl  __attribute__((cdecl))


int far * ptr;
cdecl void far strlen ( void );