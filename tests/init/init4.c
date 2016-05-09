struct S { int a, b; };
struct S s1;
//void f () {
    struct S s2 = (struct S)s1; // WARNING
    struct S s3 = (const struct S)s1; // WARNING
    struct S s4 = s1;
//}
