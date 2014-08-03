m4_divert(-1)m4_dnl
m4_define(`dnl',`m4_dnl')

m4_define(`_PUSH0',`{ yyval = $<Object>0; }')

m4_define(`rule',`dnl
m4_ifelse(`$1',`',``'',`m4_divert(1)%type$1 $2`'m4_ifelse($3,`opt',` $2_opt')
m4_divert(2)')dnl
m4_ifelse($3,`opt',`$2_opt: %empty { $$ = null; } | $2; ')dnl
$2')

m4_define(`start_grammar',`m4_divert(2)')


m4_define(`end_grammar',`
m4_divert(0)
m4_undivert(1)
m4_undivert(2)')
m4_divert`'dnl
