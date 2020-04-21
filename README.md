# tpolecat/skunk bug reproduction

# Steps
1. run `docker-compose up` to start a container running postgres 11
2. run `sbt run` to run the example
3. optionally run `jps|grep BugReport|awk '{print $1}'|xargs kill -9` if the process refuses to die
