struct C2
{
    int a, b;
};
struct C3
{
    int d;
    //int a[2];
    struct C2 a;
    int z;
};

struct C3 c[] = { {1, 2, 3}, {4, 6} };
struct C3 c2[] = { {1, {2}, 3}, {{4}, {5}, 6} };
struct C3 c3[] = { 1, 2,3,4, 3, 4, 5, 6, 7, 8 };
