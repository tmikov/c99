// declaration specifiers failure cases

#define TDUP(spec, other, name) spec other name; spec spec other name
#define TDUP2(spec, other, name) spec other name; spec spec other name ## _1

TDUP(const,int,v1);
TDUP(signed,int,v2);
TDUP(unsigned,int,v2__1);
signed unsigned v2__2;
TDUP(_Thread_local,int,v3);
TDUP(_Complex,float,v3__1);
TDUP(_Imaginary,float,v3__2);
TDUP2(typedef,int,T1);
typedef typedef typedef int T2;
TDUP(extern,int,v4);
TDUP(static,int,v5);

void func ( void )
{
  TDUP2(auto,int,v6);
  TDUP2(register,int,v7);
}

typedef extern int x1;
extern static int v10;
extern static auto int v10_1;

TDUP(_Bool,,v8);
TDUP(char,,v9);
TDUP(int,,v10);
TDUP(void,*,v11);
TDUP(float,,v12);
TDUP(double,,v13);
_Bool int v14;
int char v15;
float int v16;
int float char v17;

typedef int INT;

TDUP(struct S1,,v18);
struct S1 union S2 v19;
// TODO: enum

short v20;
short short v21;
short long v22;
long v23;
long long v24;
long long long v26;
long short long v27;
short long long v28;
long long short v29;

_Complex v30;
extern v31;

#define TSL(base,name)  \
  base name; signed base name ## _si; unsigned base name ## __un; short base name ## _sh; long base name ## _lo

TSL(_Bool,v32);
TSL(void *,v33);
TSL(float,v34);
TSL(double,v35);
TSL(struct S3,v36);
TSL(union S4,v37);
TSL(char,v38);
