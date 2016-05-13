#define __huge __attribute__((x86huge))

char c;
long l;
unsigned u;
int __huge * hp1;
int __huge * hp2 = hp1 + c;
int __huge * hp3 = hp1 + l;
int __huge * hp4 = hp1 + u;

int * p4;
int * p5 = p4 + c;
int * p6 = p4 + l;
int * p7 = p4 + u;
