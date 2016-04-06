#/bin/sh
exec java -ea -jar ${0%/*}/java/out/artifacts/cpp_jar/cpp.jar "$@"
