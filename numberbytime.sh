#!/bin/sh
#---- 统计脚本 
#caution: java will read all output ,so be careful to add output content
set -x 
export LANG=C
#the shell be call by java, so first make sure the work directory is corrent
BASEDIR=$(dirname "$0")
#echo "$BASEDIR"
cd "$BASEDIR"

beginDate=$1
endDate=$2

#echo 
#echo $beginDate
#echo $endDate
 
#export LANG=C
tmpfile1=$(mktemp ./tmp/jenkins-stat.XXXXXX)
tmpfile2=$(mktemp ./tmp/jenkins-stat.XXXXXX)

#prepare compare file
cat allresults|awk '{print $1}' >$tmpfile1
sort $tmpfile1 -o $tmpfile1
cat allbuilds|awk '{print $1}' >$tmpfile2
sort $tmpfile2 -o $tmpfile2

runningFile=$(mktemp ./tmp/jenkins-stat.XXXXXX)

comm -23 $tmpfile2 $tmpfile1 >$runningFile
lastDetail=$(mktemp ./tmp/jenkins-stat.XXXXXX)

join -t $'\t' -j 1 allbuilds $runningFile  |awk ' {print $0" RUNNING"}' >$lastDetail
join -t $'\t' -j 1 allbuilds allresults  >>$lastDetail


lastDetail2=$(mktemp ./tmp/jenkins-stat.XXXXXX)

cat $lastDetail|awk '{print $2" "$3}' >$lastDetail2
#must sort before use awk time compare
sort $lastDetail2 -o $lastDetail2

#echo $lastDetail
timeresult=$(mktemp ./tmp/jenkins-stat.XXXXXX)

awk -v beginDate="$beginDate" -v endDate="$endDate"   '$1 >= beginDate { p=1 }  $1 >= endDate { p=0 }  p { print $0 }' $lastDetail2 >$timeresult

tempresult=$(mktemp ./tmp/jenkins-stat.XXXXXX)
 

#cat $timeresult |awk 'NR>0{arr[$4]++}END{print "[";for (a in arr) print "{statu: \""a"\",\"value\": \""arr[a]"\"},";print "]"}' >$result
cat $timeresult |awk 'NR>0{ arr[substr($1,1,7)]++ } END { for (a in arr) print a" "arr[a]" " }' >$tempresult

#cat temp112 |awk 'NR>0{arr[$4]++}END{print "[";for (i = $4-1; i >= 0; --i)  print "{statu=\""$4"\",\"value\"=\""arr[$4]"\"},";print "]"}' >$result

#awk 'BEGIN {print "["} { if (NR >1)  { print "{\"runTime\":" p1",\"number\":"p2"}," } {p1=$1;p2=$2 } }  END { print "{\"runTime\":" p1",\"number\":"p2"}]" }' $result
#cat  $tempresult
tempresult2=$(mktemp ./tmp/jenkins-stat.XXXXXX)

sort $tempresult >$tempresult2
awk 'BEGIN {print "["} { if (NR >1)  { print "{\"runTime\":" p1"000000,\"number\":"p2"}," } {p1=$1;p2=$2 } }  END { print "{\"runTime\":" p1"000000,\"number\":"p2"}]" }' $tempresult2 
#cat $result
exit 8   
  
  