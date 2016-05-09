// 6.7.8#14
char a[] = "aaa";
char b[] = {"aaa"};
// ERROR
char c[] = {{"aaa"}};

struct S2 { int a; char b[10]; };
struct S2 s2_1 = { 10, "xx" };
struct S2 s2_2 = { 10, {"xx"} };
// ERROR
struct S2 s2_3 = { 10, {{"xx"}} };
