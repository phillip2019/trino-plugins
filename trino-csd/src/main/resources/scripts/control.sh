#!/bin/bash
set -x

TRINO_HOME="/var/lib/trino"
TRINO_ETC="./etc"
TRINO_LOG_LEVELS="$TRINO_ETC/log.properties"
TRINO_CONFIG_FILE="$TRINO_ETC/config.properties"
TRINO_NODE_FILE="$TRINO_ETC/node.properties"
TRINO_JVM_FILE="$TRINO_ETC/jvm.config"
TRINO_LOG_FILE=/var/log/trino
DISCOVERY_HOST_PORT=`cat trino-coordinator.properties | sed 's/http-server\.http.port\=//'`
DISCOVERY_URI="http://${DISCOVERY_HOST_PORT}"
export PATH=$PATH:$JAVA_HOME/bin

setup_node_id() {
  echo "setup_node_id"
  NODE_ID_FILE=$TRINO_HOME/node.id
  if [ ! -f $NODE_ID_FILE ]; then
    uuidgen > $NODE_ID_FILE
  fi
  export TRINO_NODE_ID=`cat $NODE_ID_FILE`
}

setup_config() {
  echo "setup_config"
  if [ "$TRINO_PROCESS_TYPE" == "worker" ]; then
      cat >> $TRINO_CONFIG_FILE <<CONFIG
coordinator=false
discovery.uri=${DISCOVERY_URI}
CONFIG
  fi

  if [ "$TRINO_PROCESS_TYPE" == "coordinator" ]; then
      cat >> $TRINO_CONFIG_FILE <<CONFIG
coordinator=true
node-scheduler.include-coordinator=true
discovery-server.enabled=true
discovery.uri=${DISCOVERY_URI}
CONFIG
  fi
}

setup_log_levels() {
  echo "setup_log_levels"
#  cat > $TRINO_LOG_LEVELS <<LOG
#io.trino=DEBUG
#LOG
 \cp log.properties $TRINO_LOG_LEVELS
}

setup_etc() {
  JAVA_HEAP=$1
  echo "setup_etc"
  if [ ! -d $TRINO_ETC ]; then
    mkdir $TRINO_ETC
    
    setup_node_id
    echo "Node id $TRINO_NODE_ID"
    echo "Copying node.properties to $TRINO_NODE_FILE"
    cp node.properties $TRINO_NODE_FILE
    cat >>$TRINO_NODE_FILE <<NODE_PROP
node.id=${TRINO_NODE_ID}
NODE_PROP

    echo "Copying config.properties to $TRINO_CONFIG_FILE"
    cp config.properties $TRINO_CONFIG_FILE
    setup_config
    
    echo "Setting up log level file $TRINO_LOG_LEVELS"
    setup_log_levels
 
    echo "Writing jvm.config to $TRINO_JVM_FILE"
    JVM_CONFIG=`cat jvm.config | sed 's/jvm\.config=//'`
    IFS=' ' read -ra JVM_CONFIG_ARRAY <<< "$JVM_CONFIG"
    jlen=${#JVM_CONFIG_ARRAY[@]}
    for ((i=0; i<${jlen}; i++));
    do
      echo ${JVM_CONFIG_ARRAY[$i]} >> $TRINO_JVM_FILE
    done
    JVM_OPTS="${JVM_CONFIG}"
    echo "-Xmx$JAVA_HEAP" >> $TRINO_JVM_FILE
    JVM_OPTS="-Xmx$JAVA_HEAP ${JVM_OPTS}"
    echo "-Xms$JAVA_HEAP" >> $TRINO_JVM_FILE
    JVM_OPTS="-Xms$JAVA_HEAP ${JVM_OPTS}"
    export JVM_OPTS
  else 
    echo "etc dir already exists."
  fi

}

containsElement() {
  echo "containsElement"
  local e
  for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done
  return 1
}

createJMXCatalog() {
  echo 'connector.name=jmx'>etc/catalog/jmx.properties
}

# csd创建，配置文件转义:-> \:
createHiveCatalog() {
  echo 'connector.name=hive-hadoop2'> etc/catalog/hive.properties
  echo 'hive.metastore.uri=thrift://bigdata-util-gateway-01.chinagoods.te:9083'>> etc/catalog/hive.properties
  echo 'hive.config.resources=/etc/hadoop/conf/core-site.xml,/etc/hadoop/conf/hdfs-site.xml,/etc/sentry/sentry-site.xml,/etc/hive/conf/hive-site.xml'>> etc/catalog/hive.properties
  echo 'hive.metastore.username=hive'>> etc/catalog/hive.properties
  echo 'hive.security=sentry'>> etc/catalog/hive.properties
  echo 'sentry.server=sentryserver'>> etc/catalog/hive.properties
  echo 'sentry.admin-user=admin1'>> etc/catalog/hive.properties
  echo 'sentry.rpc-addresses=bigdata-util-gateway-01.chinagoods.te'>> etc/catalog/hive.properties
  echo 'sentry.rpc-port=8038'>> etc/catalog/hive.properties
}

createCDHCatalogs() {
  if [ -s catalog.json ]; then
    cat catalog.json | java -jar $TRINO_INSTALL/ft_json_to_catalog/trino-json-catalog-*.jar etc/catalog/
  fi
}

setup_environment() {
  echo "setup_environment"
  pwd
  mkdir -p etc/catalog
  if [ $? -ne 0 ]; then
	echo "Could not create etc/catalog"
    exit 1
  fi

  createJMXCatalog
  createCDHCatalogs
#  createHiveCatalog

  HADOOP_USER_NAME=trino hadoop fs -get catalog/* etc/catalog/
  if [ $? -ne 0 ]; then
	echo "Could not copy catalog from HDFS to etc/catalog"
  fi

  PARCEL_DIR=$TRINO_INSTALL

  mkdir lib
  # Link existing libs
  cd lib
  for f in $PARCEL_DIR/lib/*; do
    ln -s $f || pwd
    if [ $? -ne 0 ]; then
      echo "Could not link $PARCEL_DIR/lib/$f locally"
      exit 1
    fi
  done
  cd ..

  pwd
  mkdir -p plugin
  if [ $? -ne 0 ]; then
	  echo "Could not create plugin dir"
    exit 1
  fi

  cd plugin
  # Link existing plugins
  for f in $PARCEL_DIR/plugin/*; do
    ln -s $f
  done
    
  IFS=':' read -ra PLUGINS <<< "$TRINO_PLUGINS"
  for f in "${PLUGINS[@]}"; do
    IFS='-' read -ra FILE_ARRAY <<< "$f"
    NEWPLUGIN="${FILE_ARRAY[0]}"
    IFS='|' read -ra NEWPLUGIN_PLUS_ALIAS_ARRAY <<< "NEWPLUGIN"
    if [ ${#NEWPLUGIN_PLUS_ALIAS_ARRAY[@]} -eq 1 ]; then
      LINKNAME=`basename $NEWPLUGIN`
    else
      NEWPLUGIN=${NEWPLUGIN_PLUS_ALIAS_ARRAY[0]}
      LINKNAME=${NEWPLUGIN_PLUS_ALIAS_ARRAY[1]}
    fi
    # remove any existing plugins for an overide plugin
    if [ -h $LINKNAME ]; then
      echo "Removing plugin link $LINKNAME -> $NEWPLUGIN"
      rm $LINKNAME
    fi
	echo "Adding Plugin $NEWPLUGIN"
    ln -s $NEWPLUGIN
  done
  cd ..  
  
  cp -r $PARCEL_DIR/bin .
  if [ $? -ne 0 ]; then
	echo "Could not cp $PARCEL_DIR/bin"
    exit 1
  fi

  # chown -R trino:trino .
}

start_process() {
  echo "start_process"
  setup_etc $1
  setup_environment
  echo "Starting trino process"

  TRINO_DATA_DIR=$2
  TRINO_ENVIRONMENT=$3

  exec java -cp lib/\* $JAVA_OPTS \
-Dlog.output-file=/var/log/trino/server.log \
-Dnode.data-dir=$TRINO_DATA_DIR \
-Dnode.id=$TRINO_NODE_ID \
-Dnode.environment=$TRINO_ENVIRONMENT \
-Dlog.enable-console=false \
-Dlog.levels-file=etc/log.properties \
-Dconfig=etc/config.properties \
io.trino.server.TrinoServer

}

start_coordinator() {
  echo "start_coordinator"
  export TRINO_PROCESS_TYPE="coordinator"
  create_dir $1
  create_dir $2
  start_process $3 $1 $4
}

start_worker() {
  echo "start_worker"
  export TRINO_PROCESS_TYPE="worker"
  create_dir $1
  create_dir $2
  start_process $3 $1 $4
}

create_dir() {
   echo "create_dir $1"
  NEW_DIR=$1
  if [ ! -d $NEW_DIR ]; then
    mkdir -p $NEW_DIR
    if [ $? -ne 0 ]; then
  	  echo "Could not create $NEW_DIR"
      exit 1
    fi
    echo "Dir [$NEW_DIR] created."
  else 
    echo "Dir [$NEW_DIR] already exists."
  fi
}

action="$1"

if [ "${action}" == "" ] ;then
  usage
fi

case ${action} in
  (start-coordinator)
  	start_coordinator $2 $3 $4 $5
    ;;
  (start-worker)
  	start_worker $2 $3 $4 $5
    ;; 
  (*)
    echo "Unknown command[${action}]"
    ;;
esac







