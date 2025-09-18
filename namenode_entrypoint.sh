#!/bin/bash
set -e
# Форматирование NameNode при первом запуске
if [ ! -d /tmp/hadoop/namenode ]; then
    echo "Formatting NameNode..."
    hdfs namenode -format -force
fi
exec "$@"