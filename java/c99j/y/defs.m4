m4_divert(-1)m4_dnl
m4_define(`dnl',`m4_dnl')

m4_define(`Push0',`{ yyval = $<Object>0; }')

m4_define(`rule',`dnl
m4_ifelse($2,`opt',`$1_opt: %empty { $$ = null; } | $1;')dnl
$1')

m4_divert`'dnl
