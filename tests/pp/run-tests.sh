#!/bin/bash

app=../../cpp.sh

for tst in *.c; do
   echo "Testing $tst"
   $app $tst 2>$tst.msg.run >$tst.i.run
   if diff -q $tst.i $tst.i.run > /dev/null; then
     rm $tst.i.run
   else
     diff -y $tst.i $tst.i.run | less -S
   fi
   if diff -q $tst.msg $tst.msg.run > /dev/null; then
     rm $tst.msg.run
   else
     diff -y $tst.msg $tst.msg.run | less -S
   fi
done