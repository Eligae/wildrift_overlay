#!/usr/bin/env zsh
set -e
cd "$(dirname "$0")/.."

if [[ -f app/wr-release.jks ]]; then
  echo "app/wr-release.jks already exists — remove it first if you want to regenerate." >&2
  exit 1
fi

read -s "?Keystore password: " KSPW
echo

keytool -genkey -keystore app/wr-release.jks -alias wr-release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Eligae,O=WR,C=KR" \
  -storepass "$KSPW" -keypass "$KSPW"

{
  echo "RELEASE_KEYSTORE_PATH=wr-release.jks"
  echo "RELEASE_KEYSTORE_PASSWORD=$KSPW"
  echo "RELEASE_KEY_ALIAS=wr-release"
  echo "RELEASE_KEY_PASSWORD=$KSPW"
} >> local.properties

echo OK
