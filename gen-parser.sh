#!/bin/bash
vis=""
if [ "$1" == "-x" ]; then
  vis="-x"
  shift
fi


ydir="java/c99j/y"
javadir="java/c99j/src/c99/parser"
skeldir="bison"

m4 --prefix-builtins $ydir/defs.m4 $ydir/CParser.m4.y > $ydir/CParser.y
bison $vis -Wall -S $skeldir/lalr1.java $ydir/CParser.y -o $javadir/CParser.java

if [ -n "$vis" ]; then
  mv $javadir/CParser.xml $ydir/

  datadir=`bison --print-datadir`
  xsltproc $datadir/xslt/xml2xhtml.xsl $ydir/CParser.xml > $ydir/CParser.html
fi
