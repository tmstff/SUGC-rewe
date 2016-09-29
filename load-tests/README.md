# Load Tests for the Shopping List Service

### To be executed with:

`./activator test`

### possible configurations

the test can be configured by adding parameters to the line above, i.e.:
`./activator -D<paramName>=<value>`

**users**
number of users per second that can act in parallel at the most, default is `10`

**ramp_up_duration**
duration in seconds until the final request rate is reached, default is `30`

**duration**
duration in seconds the request rate is maintained, default is `30`

**request_rate**
the request rate in requests per seconds, default is `200`

**environment**
the environment against which the load test is ran, default is `PLAY`
possible environments are:
`PLAY` `AKKA_HTTP` `FINATRA

-Denvironment=PLAY
-Denvironment=AKKA_HTTP
-Denvironment=FINATRA

**target_75th_percentile**
the target response time (for success of the test) for the 75th percentile in milliseconds, default is `60`

**target_95th_percentile**
the target response time (for success of the test) for the 95th percentile in milliseconds, default is `220`

**target_successful_request_rate**
the target rate of successfull requests (for success of the test) in occurrences **per million**, default is `990000`

**target_mean_requests_rate**
the target mean request rate (for success of the test) in requests per seconds, default is `125`