#!/usr/bin/env sh

host=192.168.99.100
port=2379

docker run \
    -d \
    -p ${port}:${port} \
    --name etcd \
    quay.io/coreos/etcd:v2.2.5 \
    -advertise-client-urls http://${host}:${port} \
    -listen-client-urls http://0.0.0.0:${port}
