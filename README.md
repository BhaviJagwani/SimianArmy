## Guide to setting up and testing Netflix's Simian-Army Chaos Monkey

Simian Army consists of services (Monkeys) in the cloud for generating various kinds of failures, detecting abnormal conditions, and testing our ability to survive them.
(Find the original README [here](https://github.com/Netflix/SimianArmy/blob/master/README.md))
This fork contains 
- A sample application to test the Simian-Army setup
- An extension to ChaosMonkey for RDS instances (RDSChaosMonkey) 

### Setting up Chaos Monkey

Chaos Monkey is a service which identifies groups of systems and randomly terminates one of the systems in a group.

- [Sign up for an AWS Account](https://www.amazon.com/ap/signin?openid.assoc_handle=aws&openid.return_to=https%3A%2F%2Fsignin.aws.amazon.com%2Foauth%3Fresponse_type%3Dcode%26client_id%3Darn%253Aaws%253Aiam%253A%253A015428540659%253Auser%252Fawssignupportal%26redirect_uri%3Dhttps%253A%252F%252Fportal.aws.amazon.com%252Fbilling%252Fsignup%253Fredirect_url%253Dhttps%25253A%25252F%25252Faws.amazon.com%25252Fregistration-confirmation%2526state%253DhashArgs%252523%2526isauthcode%253Dtrue%26noAuthCookie%3Dtrue&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&action=&disableCorpSignUp=&clientContext=&marketPlaceId=&poolName=&authCookies=&pageId=aws.ssop&siteState=unregistered%2Cen_US&accountStatusPolicy=P1&sso=&openid.pape.preferred_auth_policies=MultifactorPhysical&openid.pape.max_auth_age=120&openid.ns.pape=http%3A%2F%2Fspecs.openid.net%2Fextensions%2Fpape%2F1.0&server=%2Fap%2Fsignin%3Fie%3DUTF8&accountPoolAlias=&forceMobileApp=0&language=en_US&forceMobileLayout=0).
AWS has a lot of services under the [free tier](https://aws.amazon.com/free/) for the first 12 months after account creation

- Follow the [Quick Start Guide](https://github.com/Netflix/SimianArmy/wiki/Quick-Start-Guide) to 
  * Setup an Auto Scaling Group 
  * Setup a User Policy 
  * [Setup Simple Email Service Account](https://github.com/Netflix/SimianArmy/wiki/Quick-Start-Guide#setup-simple-email-service-account) (Optional; if you want to receive email notifications)
  * Clone the git repo and [Build the Monkeys with Gradle](https://github.com/Netflix/SimianArmy/wiki/Quick-Start-Guide#build-the-monkeys-with-gradle)
  
- For testing modify client.properties and simianarmy.properties 
  ```
  * set aws_secret_key, aws_account_key, aws_region in client.properties
  * set simianarmy.calendar.isMonkeyTime=true (if on weekend or not in 9-3pm) in simianarmy.properties
  * set simianarmy.chaos.ASG.enabled=true
  ```
  to make instances in all auto-scaling groups elgible for termination. 
  Optionally, 
  ```
  simianarmy.chaos.ASG.enabled= false 
  simianarmy.chaos.ASG.<asgname>.enabled=true
  simianarmy.chaos.ASG.<asgname>.probability=1.0
  ```
  **Note:** The simianarmy.chaos.leashed=true setting should still be set, this will run Chaos Monkey in a test-only mode, no terminations will be done.
  Other Chaos Monkey Configuration options can be found [here](https://github.com/Netflix/SimianArmy/wiki/Chaos-Settings)

- Run the gradle jetty server to start the Monkeys
```$ gradle jettyRun ```
To guarantee that a termination happens, you can set the probability to "6.0" (ie 600% which will make sure it kills an instance on the first run).


### Extending ChaosMonkey to create RDSChaosMonkey
This Monkey extends the basic chaos monkey and randomly picks an RDS Instance from the region specified for rebooting. It forces a failover if Multi-AZ is enabled for the instance.
- Clone and build [this fork](https://github.com/BhaviJagwani/SimianArmy)
- Configure RDSChaosMonkey by modifying rdschaos.properties (The configuration is similar to chaos monkey configuration)

**ToDo**:
- Add EmailNotifier
- Create filtering of instances by name or tags based on configuration


### Testing the setup 

- Create a sample java application like [this](link to repo here) which connects to an RDS Endpoint and has APIs to add and list entries. 
- Deploy it to an ec2-instance and configure it to run on start up. (You can do this by creating a script and adding it to /etc/rc.local)
 
  ```
  nohup java -jar /home/ec2-user/hello-world-0.1.0.jar > /home/ec2-user/log.txt &
  ```
- Create and run a script to test failover of the web servers and RDS instances
  
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

