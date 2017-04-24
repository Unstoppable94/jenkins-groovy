#!/bin/sh
#---- 统计脚本 
#caution: java will read all output ,so be careful to add output content
set -x 
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

#echo $lastDetail
timeresult=$(mktemp ./tmp/jenkins-stat.XXXXXX)
awk -v beginDate="$beginDate" -v endDate="$endDate"  -F'\t' '$2 >= beginDate { p=1 }  $2 >= endDate { p=0 }  p { print $0 }' $lastDetail >$timeresult

result=$(mktemp ./tmp/jenkins-stat.XXXXXX)



#cat $timeresult |awk 'NR>0{arr[$4]++}END{print "[";for (a in arr) print "{statu: \""a"\",\"value\": \""arr[a]"\"},";print "]"}' >$result
cat $timeresult |awk 'NR>0{ arr[$3]++ } END { for (a in arr) print a" "arr[a]" " }' >$result

#cat temp112 |awk 'NR>0{arr[$4]++}END{print "[";for (i = $4-1; i >= 0; --i)  print "{statu=\""$4"\",\"value\"=\""arr[$4]"\"},";print "]"}' >$result

cat $result
exit 8   
  
  