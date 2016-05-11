#!/bin/bash

app="../../parse.sh --debugInit"

for tst in *.c; do
   echo "Testing $tst"
   $app $tst 2>$tst.msg.run >$tst.ast.run
   if diff -q -a $tst.ast $tst.ast.run > /dev/null; then
     rm $tst.ast.run
   else
     diff -y -a $tst.ast $tst.ast.run | less -S
   fi
   if diff -q -a $tst.msg $tst.msg.run > /dev/null; then
     rm $tst.msg.run
   else
     diff -y -a $tst.msg $tst.msg.run | less -S
   fi
done
