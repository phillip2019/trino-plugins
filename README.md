# build csd
```aspectj
mvn clean package -DskipTests
```

# Trino Plugins

This repo contains to following Trino plugins.

 - [spreadsheet-storage-handler](https://github.com/fortitudetec/trino-plugins/tree/master/spreadsheet-storage-handler "spreadsheet-storage-handler")
 - [zookeeper-storage-handler](https://github.com/fortitudetec/trino-plugins/tree/master/zookeeper-storage-handler "zookeeper-storage-handler")


# 打包编译trino二进制发行包
```shell script
cd trino-binary-to-parcel/

bash trino-binary-to-parcel.sh trino-server-358.tar.gz
```