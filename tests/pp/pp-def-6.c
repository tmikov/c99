#define LOG( sev, ... )  \
  printf( #sev ": " ); \
  printf( __VA_ARGS__ )


LOG( warn, "Error" );
LOG( err, "errno %d", errno );
LOG( info, "value %d=%d", valIndex-1, value+1 );
LOG( err );
