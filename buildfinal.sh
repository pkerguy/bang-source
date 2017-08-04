rm -rf final/*
rmdir final
mkdir final
rm -rf obj/*
rmdir obj
mkdir obj
cp -R build/client/*.jar obj/desktop-1.1-SNAPSHOT.jar
java -Xms6G -Xmx16G -jar allatori.jar config.xml