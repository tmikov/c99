struct S { int a, b, c; };
struct T { int a; struct S s; int c; };

struct S s;

//void func () {
    struct T t = { 1, s, 2 };
//}
