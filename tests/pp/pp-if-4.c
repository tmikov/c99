#if -1 < 1
yes1;
#else
no1;
#endif

#if -1 < 1llu
no2;
#else
yes2;
#endif

#if !defined BLA
yes3;
#else
no3;
#endif

#if !!defined BLA
no4;
#else
yes4;
#endif

#if !defined( BLA )
yes5;
#else
no5;
#endif

#if BLA
no6;
#else
yes6;
#endif

#if !BLA
yes7;
#else
no7;
#endif

#define MY   1
#if MY
yes8;
#else
no8;
#endif

#if defined(MY)
yes9;
#else
no9;
#endif

#if defined MY
yes10;
#else
no10;
#endif

#if defined(MY) && MY
yes11;
#else
no11;
#endif
