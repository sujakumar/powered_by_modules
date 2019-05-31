#!/bin/bash
#export JAVA_OPTS="$JAVA_OPTS -Xms8G -Xmx8G"
#export GROOVY_OPTS="$GROOVY_OPTS -Xms8G -Xmx8G"

loop=0
max_file_size=100000
days_back=3
sleep_sec=360
extract_plan=-1

sdir="$(dirname $(readlink -f $0))"
current_groovy=groovy
work_dir=$sdir/work
mkdir $work_dir

log_file=$sdir/log/PriorityProconCoreConfig.log
extract_config_file=$sdir/config/PriorityProconCoreConfig.groovy
conn_config_file=$sdir/Config.groovy

usage()
{
    echo "usage: "
}

while [ "$1" != "" ]; do
    case $1 in
	-e | --extract_plan )	shift
				extract_plan=$1
				;;
        -m | --max_file_size )  shift
                                max_file_size=$1
                                ;;
        -l | --loop )           loop=1
                                ;;
        -d | --days_back )      shift
                                days_back=$1
                                ;;
        -s | --sleep )          shift
                                sleep_sec=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

log_file=$sdir/log/$extract_plan.log
extract_config_file=$sdir/config/${extract_plan}Config.groovy
conn_config_file=$sdir/Config.groovy

extract_cmd="$current_groovy $sdir/Extract.groovy $conn_config_file $extract_config_file $work_dir $days_back $max_file_size" 

while  
	echo "running $extract_cmd"
	savelog -n -l -c 100 $log_file
	echo "$extract_cmd"	
	$extract_cmd >> $log_file 2>&1
	echo "sleeping $sleep_sec seconds"
	sleep $sleep_sec
	echo "done sleeping"
	[ "$loop" -eq "1" ]
do true; done


