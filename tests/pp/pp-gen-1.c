#define foo() bar

foo()baz

#define BAD(x) x
+BAD(1)
+BAD(=)
+BAD(+)
>BAD(>)
>>BAD(=)
>BAD(>=)


