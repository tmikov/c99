// Declaration faulure cases

int v1;
register int v2;
auto int v3;

extern int v4 = 0;

void func1 ( void )
{
  extern int v5;
  extern int v6 = 0;
}

void func2 (
  register p1,
  auto int p2,
  static int p3
);

struct S1
{
  int f1;
  static int f2;
  int f3[];
};

int v7;
char v7;

char const * v8;
char const * v8;
char const * const v8;

int v9;
int v9;
int v9 = 1;
int v9 = 2;
int v9;
static int v9;

int v10;

void func3 ( void )
{
  extern char v10;
}

int v11;
void func4 ( void )
{
  extern int v11;
  int v11;
}

int v12;
static int v12;

static int v13 = 0;
extern int v13;