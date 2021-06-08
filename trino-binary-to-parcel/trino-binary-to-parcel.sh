#!/bin/bash

if [ -z "$1" ]; then
  echo "No trino binary tarball specified."
  exit 1
fi

TRINO_TAR_FILE=$1
TMP_VAR=`echo $TRINO_TAR_FILE | sed -e 's/\.tar\.gz//g'`
IFS='-' read -a array <<< "$TMP_VAR"
TRINO_VERSION="${array[2]}"
echo "Trino version for binary is $TRINO_VERSION"

PROJECT_DIR=`dirname "$0"`
PROJECT_DIR=`cd "$PROJECT_DIR"; pwd`
echo "Project directory is $TRINO_VERSION"

PJSON2CAT=$PROJECT_DIR/trino-json-catalog/
cd $PJSON2CAT
mvn clean install
echo "Compile trino-json-catalog is finished"
cd $PROJECT_DIR

PROJECT_DIR_TMP=$PROJECT_DIR/tmp

if [ -d $PROJECT_DIR_TMP ] ; then
  rm -r $PROJECT_DIR_TMP
fi

mkdir $PROJECT_DIR_TMP
echo "Extracting $TRINO_TAR_FILE file."
tar -xzf $TRINO_TAR_FILE -C $PROJECT_DIR_TMP

cd $PROJECT_DIR_TMP
ls -1 | sed -e 'p;s/-server-/-/' | xargs -n2 mv
#TRINO_DIR=`ls -1`
TRINO_DIR_LOWERCASE=`ls -1`
echo "Trino directory is $TRINO_DIR_LOWERCASE"
TRINO_DIR=`echo $TRINO_DIR_LOWERCASE | awk '{print toupper($0)}'`
mv $TRINO_DIR_LOWERCASE $TRINO_DIR

TRINO_DIR_META=$TRINO_DIR/meta
mkdir $TRINO_DIR_META

echo "Writing $TRINO_DIR_META/parcel.json file."
cat > $TRINO_DIR_META/parcel.json <<JSON
{
  "schema_version": 1,
  "name": "TRINO",
  "version" : "${TRINO_VERSION}",
  "setActiveSymlink": true,
  "depends": "",
  "replaces":"TRINO",
  "conflicts":"",
  "provides": ["TRINOCOORDINATOR", "TRINOWORKER"],
  "scripts": {"defines":"trino_parcel_env.sh"},
  "components": [
    {
      "name" : "TRINO",
      "version" : "${TRINO_VERSION}",
      "pkg_version": "${TRINO_VERSION}"
    }
  ],
  "packages" : [],
  "users":  {
    "trino": {
      "longname": "trino",
      "shell": "/bin/bash",
      "home": "/var/lib/trino",
      "extra_groups": ["hive", "trino"]
    }
  },
  "groups": []
}
JSON

echo "Writing $TRINO_DIR_META/trino_parcel_env.sh file."
cat > $TRINO_DIR_META/trino_parcel_env.sh <<TRINO_SCRIPT
#!/bin/bash
export TRINO_INSTALL=\$PARCELS_ROOT/\$PARCEL_DIRNAME
echo "***** PREFIX:"
env
echo "**** DONE."
TRINO_SCRIPT

PARCEL_FILE="$PROJECT_DIR/TRINO-$TRINO_VERSION.parcel"

mkdir $TRINO_DIR/ft_json_to_catalog

cp $PJSON2CAT/target/trino-json-catalog-*.jar $TRINO_DIR/ft_json_to_catalog

if [ -f $PARCEL_FILE ]; then
  echo "Removing old parcel file $PARCEL_FILE"
  rm $PARCEL_FILE
fi

echo "Creating parcel $PARCEL_FILE"
tar -czf $PARCEL_FILE TRINO-$TRINO_VERSION
cd $PROJECT_DIR
echo "Removing tmp dir"
rm -r $PROJECT_DIR_TMP

PARCEL_FILE_SHA=$PARCEL_FILE.sha
shasum $PARCEL_FILE | awk '{print $1}' > $PARCEL_FILE_SHA
HASH=`cat $PARCEL_FILE_SHA`
LAST_UPDATED_SEC=`date +%s`
LAST_UPDATED="${LAST_UPDATED_SEC}0000"
HTTP_DIR="$PROJECT_DIR/http"
if [ -d $HTTP_DIR ]; then
  rm -r $HTTP_DIR
fi
mkdir $HTTP_DIR
MANIFEST="$HTTP_DIR/manifest.json"
echo "{\"lastUpdated\":${LAST_UPDATED},\"parcels\": [" > $MANIFEST
for DISTRO in el5 el6 el7 sles11 lucid precise trusty squeeze wheezy
do
	if [ $DISTRO != "el5" ] ; then
		echo "," >> $MANIFEST
	fi
	DISTRO_PARCEL="TRINO-${TRINO_VERSION}-${DISTRO}.parcel"
	DISTRO_PARCEL_SHA="TRINO-${TRINO_VERSION}-${DISTRO}.parcel.sha"
	ln $PARCEL_FILE "${HTTP_DIR}/${DISTRO_PARCEL}"
	ln $PARCEL_FILE_SHA "${HTTP_DIR}/${DISTRO_PARCEL_SHA}"
	echo "{\"parcelName\":\"${DISTRO_PARCEL}\",\"components\": [{\"name\" : \"TRINO\",\"version\" : \"${TRINO_VERSION}\",\"pkg_version\": \"${TRINO_VERSION}\"}],\"hash\":\"${HASH}\"}" >> $MANIFEST
done
echo "]}" >> $MANIFEST

echo "To start parcel server:"
echo "cd http"
echo "python -m SimpleHTTPServer"


