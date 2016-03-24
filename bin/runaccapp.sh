#!/usr/bin/env sh

n=0
if [ -n "$1" ]; then
    n="$1"
fi
tag=latest
if [ -n "$2" ]; then
    tag="$2"
fi
host=192.168.99.100

docker run \
    -d \
    -p 800${n}:8000 \
    -p 255${n}:2552 \
    --name accapp${n} \
    hseeberger/akka-cluster-control-app:${tag} \
    -Dakka.remote.netty.tcp.hostname=${host} \
    -Dakka.remote.netty.tcp.port=255${n} \
    -Dconstructr.coordination.host=${host}
