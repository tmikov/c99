// tag failure cases

struct S1;
struct S1
{
  int x, y;
};

struct S2
{
  int x;
};
struct S2 * v1;
struct S2
{
  int x;
};

struct S3
{
  int y;
};

union S3
{
  int y;
};

struct S4;
union S4;