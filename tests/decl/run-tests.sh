#!/bin/bash

app=../../parse.sh

for tst in *.c; do
   echo "Testing $tst"
   $app $tst 2>$tst.msg.run
   if diff -q -a $tst.msg $tst.msg.run > /dev/null; then
     rm $tst.msg.run
   else
     diff -y -a $tst.msg $tst.msg.run | less -S
   fi
done
