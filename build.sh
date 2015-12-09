#!/usr/bin/env bash

set -e

VERSION=$1
DIST_NAME="math_engine-${VERSION}"
DIST_DIR="target/${DIST_NAME}"

BOOT_JVM_OPTIONS='-Xmx2g'

echo "Building..."
./boot fetch-obfuscating-deps
./boot build

mkdir ${DIST_DIR}
mkdir ${DIST_DIR}/conf
mkdir ${DIST_DIR}/bin
mkdir ${DIST_DIR}/lib

cp target/math_engine.final.jar ${DIST_DIR}/lib/math_engine.jar

echo "Copying configuration files..."
cp config.example.json ${DIST_DIR}/conf/config.json

echo "Copying logger files..."
cp resources/logback.production.xml ${DIST_DIR}/conf/logger-conf.xml

echo "Copying shell wrapper files..."
cp build/math_engine.sh ${DIST_DIR}/bin/math_engine.sh
chmod +x ${DIST_DIR}/bin/math_engine.sh

echo "Creating zip file..."
cd target && zip -r ${DIST_NAME}.zip ${DIST_NAME} && cd -

echo "Pushing release into s3..."
aws s3 cp --region eu-central-1 \
    target/${DIST_NAME}.zip \
    s3://com.betengines.releases/math_engine/${DIST_NAME}.zip

echo "Pushing changelog into s3..."
aws s3 cp --region eu-central-1 \
    ChangeLog.txt \
    s3://com.betengines.obfuscation.changelog/math_engine/ChangeLog-${VERSION}.txt

echo "Done"