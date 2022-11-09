#!/bin/sh

# Abort on any error (including if wait-for-it fails).
set -e

# Wait for the config server to be up.
if [ -n "$CONFIG_SERVER_HOST" ]; then
./wait-for-it.sh "$CONFIG_SERVER_HOST:$CONFIG_SERVER_PORT" -t 0
fi

# Wait for the discovery server to be up.
if [ -n "$DISCOVERY_SERVER_HOST" ]; then
./wait-for-it.sh "$DISCOVERY_SERVER_HOST:$DISCOVERY_SERVER_PORT" -t 0
fi

# Wait for the minio server to be up.
# if [ -n "$MINIO_HOST" ]; then
# ./wait-for-it.sh "$MINIO_HOST:$MINIO_PORT" -t 0
# fi

# Run the main container command.
exec "$@"