#!/bin/bash
list_url="http://<ELB>/list"
add_url="http://<ELB>/add"
total=$(curl --connect-timeout 20 -s $list_url)
echo "DB Instance is up!"

while true 
do
	data="{\"name\":\"soldier$total\"}"
	add=$(curl --connect-timeout 20 -s $add_url -H "Content-Type: application/json" -X POST -d $data)
	total=$[$total+1]
	
	if [ "$add" != "Added" ]
	then 
		echo "Error1"
		total=$[$total-1] 
	elif [ $(curl --connect-timeout 20 -s $list_url) != "$total" ]
	then
			echo "Error"
			total=$[$total-1]	
	else 
		echo "Success"
		date 	
	fi
	echo "Sleeping for 15 seconds"
	sleep 15
	echo "Running next test"
done
