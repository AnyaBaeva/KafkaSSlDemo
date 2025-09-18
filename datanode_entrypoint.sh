#!/bin/bash
set -e
# Создание директории для DataNode
mkdir -p /tmp/hadoop/datanode
chmod -R 755 /tmp/hadoop
exec "$@"