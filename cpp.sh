#/bin/sh
exec java -ea -jar ${0%/*}/java/out/artifacts/c99j_jar/c99j.jar --cpp $@
