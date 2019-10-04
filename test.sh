#!/bin/bash
#export JAVA_OPTS="$JAVA_OPTS -Xms8G -Xmx8G"
#export GROOVY_OPTS="$GROOVY_OPTS -Xms8G -Xmx8G"


sdir="$(dirname $(readlink -f $0))"
current_groovy=groovy
work_dir=$sdir/work
mkdir $work_dir

usage()
{
    echo "usage: "
}

while [ "$1" != "" ]; do
    case $1 in
        -m | --max_file_size )  shift
                                max_file_size=$1
                                ;;
        -d | --days_back )      shift
                                days_back=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done



log_file=$sdir/log/Test.log
extract_config_file=$sdir/config/TestConfig.groovy
conn_config_file=$sdir/Config.groovy

extract_cmd="$current_groovy $sdir/Extract.groovy $conn_config_file $extract_config_file $work_dir $days_back $max_file_size"


echo "starting..."
savelog -n -l -c 100 $log_file
echo "$extract_cmd"
$extract_cmd >> $log_file 2>&1
echo "done!"
