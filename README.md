## DESCRIPTION

The Simian Army is a suite of tools for keeping your cloud operating in top form.  Chaos Monkey, the first member, is a resiliency tool that
helps ensure that your applications can tolerate random instance failures

## Build status

[![Build Status](https://travis-ci.org/Netflix/SimianArmy.svg?branch=master)](https://travis-ci.org/Netflix/SimianArmy)
[![Apache 2.0](https://img.shields.io/github/license/Netflix/SimianArmy.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## DETAILS

Please see the [wiki](https://github.com/Netflix/SimianArmy/wiki).

## SUPPORT

[Simian Army Google group](http://groups.google.com/group/simianarmy-users).

## LICENSE

Copyright 2012-2016 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the “License”); you may not use this file except in
compliance with the License. You may obtain a copy of the License at

<<<<<<< HEAD
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
=======
### Testing the setup 

- Create a sample java application like [this](https://github.com/BhaviJagwani/SimianArmy/tree/master/test_setup/HelloWorld) which connects to an RDS Endpoint and has APIs to add and list entries. 
- Deploy it to an ec2-instance and configure it to run on start up. (You can do this by creating a script and adding it to /etc/rc.local)

  ```
  nohup java -jar /home/ec2-user/hello-world-0.1.0.jar > /home/ec2-user/log.txt &
  ```
- Create and run a [script](https://github.com/BhaviJagwani/SimianArmy/blob/master/test_setup/status.sh) to test failover of the web servers and RDS instances

  ```
  list_url="http://<myELB>/list"
  add_url="http://<myELB>/add"
  total=$(curl --connect-timeout 20 -s $list_url)

  echo "DB Instance is up!"
  while true 
  do
	data="{\"name\":\"soldier$total\"}"
	add=$(curl --connect-timeout 20 -s $add_url -H "Content-Type: application/json" -X POST -d $data)
	total=$[$total+1]
	if [ "$add" != "Added" ]
	then 
		echo "Error"
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
  ```
- Run gradle jetty server ```$gradle jettyRun``` to start the Monkeys

>>>>>>> 1c26acd822d109a631ecbe50a3b5995975bd19f0
