    #!/bin/bash
    export TERM=xterm

    set -e


    mkdir -p /usr/local/hadoop/hdfs/namenode
    chmod -R 777 /usr/local/hadoop/hdfs/namenode


    # Проверяем, нужно ли форматировать NameNode
    if [ ! -f /hadoop/dfs/name/current/VERSION ]; then
        echo "Formatting Hadoop NameNode..."
        hdfs namenode -format -force -nonInteractive
        echo "NameNode formatted successfully!"
    else
        echo "NameNode already formatted, skipping format..."
    fi

    echo "Starting Hadoop NameNode..."
    exec hdfs namenode