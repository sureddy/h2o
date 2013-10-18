#!/bin/bash

#set -x

h2oBuild=
benchmarks="benchmarks"
DATE=`date +%Y-%m-%d`
archive="Archive"

function all {
    doAlgo pca
    doAlgo glm
    doAlgo kmeans
    doAlgo gbm
    doAlgo glm2
    doAlgo gbmgrid
    doAlgo bigkmeans
}

function doAlgo {
    #echo "Clear caches!"
    #sudo bash -c "sync; echo 3 > /proc/sys/vm/drop_caches"

    echo "Running $1 benchmark..."

    pyScript="BMscripts/"$1"Bench.py"

    if [ ! $1 = "bigkmeans" ]
    then
        python ${pyScript} -cj BMscripts/${JSON} ${h2oBuild}
        wait
    else
        python ${pyScript} ${h2oBuild}
        wait
    fi
    zip -r  ${archive}/${h2oBuild}-${DATE}-$1 sandbox/
    wait
    rm -rf sandbox/ 
}

usage()
{
cat << EOF

USAGE: $0 [options]

This script obtains the latest h2o jar from S3 and runs the benchmarks for PCA, KMeans, GLM, and BigKMeans.

OPTIONS:
   -h      Show this message
   -t      Run task:
               Choices are:
                   all        -- Runs PCA, GLM, KMEANS, GBM, GLM2, GBMGRID, and BIGKMEANS
                   pca        -- Runs PCA on Airlines/AllBedrooms/Covtype data
                   kmeans     -- Runs KMeans on Airlines/AllBedrooms/Covtype data
                   glm        -- Runs logistic regression on Airlines/AllBedrooms/Covtype data
                   glm2       -- Runs logistic regression on Airlines/AllBedrooms/Covtype data
                   gbm        -- Runs GBM on Airlines/AllBedrooms/Covtype data
                   gbmgrid    -- Runs GBM grid search on Airlines/AllBedrooms/Covtype data
                   bigkmeans  -- Runs KMeans on 180 GB & 1TB of synthetic data
                   
   -j      JSON config:
               Choices are:
                   161        -- Runs benchmark(s) on single machine on 161 (100GB)
                   162        -- Runs benchmark(s) on single machine on 162 (100GB)
                   163        -- Runs benchmark(s) on single machine on 163 (100GB)
                   164        -- Runs benchmark(s) on single machine on 164 (100GB)
         		   161_163    -- Runs benchmark(s) on four machines 161-163 (133GB Each)
                   161_164    -- Runs benchmark(s) on four machines 161-164 (100GB Each)
EOF
}

TASK=
JSON=
while getopts "ht:j:" OPTION
do
  case $OPTION in
    h)
      usage
      exit 1
      ;;
    t)
      TEST=$OPTARG
      ;;
    j)
      JSON=$OPTARG
      ;;
    ?)
      usage
      exit 1
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done

if [ -z "$TEST" ] || [ -z "$JSON" ]
then
    usage
    exit
fi

#bash S3getLatest.sh
#wait
h2oBuild=`cat latest`

if [ ! -d ${benchmarks}/${h2oBuild}/${DATE} ]; then
  mkdir -p ${benchmarks}/${h2oBuild}/${DATE}
fi

if [ ! $TEST = "all" ]
then
    echo "$TEST"
    doAlgo $TEST
else
    $TEST
fi
wait

#remove annoying useless files
#rm pytest*flatfile*
#rm benchmark*log

#archive nohup
#if [ -a nohup.out ]; then
#    mv nohup.out ${archive}/${h2oBuild}-${DATE}-nohup.out
#fi