#!/bin/bash

app=../../cpp.sh

for tst in *.c; do
   echo "Testing $tst"
   $app --date="Jul 13 2014 10:00:00" $tst 2>$tst.msg.run >$tst.i.run
   if diff -q -a $tst.i $tst.i.run > /dev/null; then
     rm $tst.i.run
   else
     diff -y -a $tst.i $tst.i.run | less -S
   fi
   if diff -q -a $tst.msg $tst.msg.run > /dev/null; then
     rm $tst.msg.run
   else
     diff -y -a $tst.msg $tst.msg.run | less -S
   fi
done
