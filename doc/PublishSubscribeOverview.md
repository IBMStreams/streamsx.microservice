# Publish-subscribe overview
Publish-subscribe provides the foundation to create streaming microservices.
## Streaming microservices
Streaming microservices publish streams that are independently subscribed to by other streaming microservices.
A streaming microservice (henceforth referred to as simply a *microservice*) is a job running in a IBM Streams instance.
Publish-subscribe thus works between microservices (jobs) running within the same instance. The instance can be the one provided by
Streaming Analytics service on IBM Cloud or an on-prem deployment of IBM Streams.

See also: https://community.ibm.com/community/user/cloudpakfordata/viewdocument/analytics-microservice-architecture?CommunityKey=c0c16ff2-10ef-4b50-ae4c-57d769937235&tab=librarydocuments

### Dyanamic and independent
The publish-subscribe model allows the publisher and subscriber to be independent. Any number of publishers and subscribers can exist and be unware of any implementation details of any microservice. No pre-registration is needed, if a subscriber's stream subscription matches a published stream then the connection is made and subscriber microservice starts consuming data from the publishing microservice.

Publish-subscribe is also dynamic, when a subscribing microservice is submitted it will connect to any matching published streams that exist at the time. Subsequently if new publishing microservices are submitted they will connect to any existing subscribing microservice that have a matching subscription.

### Typical pattern
A typical pattern is to have three types of microservices:
 * **Ingest microservices** that publish streams of data from external sources. An ingest microservice may publish the raw data or
 provide value-add through data cleansing and/or enrichment. Providing source data as published stream provides isolation and
 allows developers of downstream applications to be unaware of external source details, such as connection credentials, URLs, hostnames etc.
 * **Analytical microservices** that subscribe to streams published by _ingest microservices_ and/or _analytical microservices_, perform streaming data analysis and then
 publish streams containing insights from the data. These published streams can contain final results for _egress microservices_
 or intermediate results for other _analytical microservices_. 
 * **Egress microservices** that subscribe to streams published by _analytics microservices_ and send the insights to external systems.
 Again this allows upstream developers to be unaware of details of how to write data to an external system.
 
 This pattern is just an example, a microservice can publish and/or subscribe to any number of streams.
 
 ## Implementation language independence
 Microservices can be implemented in any programming language supported by IBM Streams, so for example a Python
 _analytics microservice_ can subscribe to streams published by an SPL _ingest microservice_.
 
 ## Published stream
 
 A stream is published to a *topic* and has a *type*. Subscribers match published stream based upon the _topic_ and _type_.
 
 ### Published topic
Publishing a stream publishes it to a single _topic_. Topic syntax is the same as MQTT topic syntax. A topic consists of one or more topic levels. Each topic level is separated by a forward slash (topic level separator), for example `twitter/tweets/raw`. There can be an arbitrary number of levels, typically made up of three parts:
  * A topic domain space, typically one or two levels - allows microservices to be developed independently without fear of clashes leading to incorrect subscriptions. For example:
     * `twitter/tweets/raw` - the domain is _twitter_ indicating any topic starting with _twitter_ is from Twitter.
     * `streamsx/transportation/nextbus/actransit/locations` - the domain is _streamsx/transportation_ and is the pattern used by the toolkits from IBMStreams organization at github.com using _streamsx_ as the first element and then the project name as the second. This ensures that re-useable microservices implemented by different projects will not end up accidentially using the same topic.
  * The category of the data within the domain, for example within the `twitter` domain these could exist:
      * `twitter/tweets/raw` - _tweets_ is the category and means that any published stream with a topic starting with _twitter/tweets_ is related to tweets, in this case the raw JSON of the tweet.
      * `twitter/users/new` - _users_ is the category and means that any published stream with a topic starting with _twitter/users_ is related to users, such as for this topic a stream containing users that have just registered.
      * `streamsx/transportation/nextbus/actransit/locations` - _nextbus_ is the category for data from NextBus.
  * Levels following the domain and category topic levels that indicate the data being published. For example:
      * `twitter/tweets/raw` - A published stream containing the full raw JSON of each tweet.
      * `twitter/tweets/text` - A published stream containing just the text of each tweet.
      * `streamsx/transportation/nextbus/actransit/locations` - Nextbus locations for AC Transit agency.
      * `streamsx/transportation/nextbus/sf-muni/locations` -  Nextbus locations for San Francisco Muni agency.
 
Topics are not-predefined, they are dynamically created when a microservice is running that publishes to the topic.

Subscribers can use topic filters to subscribe to multiple topics, for example `streamsx/transportation/nextbus/+/locations` subscribes
to published streams for NextBus locations from any agency being published.
 
Before designing a system comprising of multiple microservices the topic scheme used must be designed with publishers and subscriber use cases in mind.

### Published type
A stream is published with its stream type, this is one of:
  * The stream's schema - its SPL or structured schema. Supported by all programming languages.
    * For example a published stream of sensor data might have the schema `tuple<timestamp ts, rstring id, float64 value>`
    * Each tuple is handled as streams tuple with named attributes, tuple in SPL, `com.ibm.streams.operator.Tuple` in Java, `dict`, `tuple` or named tuple in Python.
  * An interchange type - A type that treats each tuple as a single value, a JSON object, a string, binary data or an XML document.
    * Each tuple is handled as a single object, for example with string `java.lang.String` in Java, `str` in Python.
  * A Java class - Each tuple on the stream is an instance of the declared class or interface. Only supported by Java applications.
  * A Python object - Each tuple on the stream is a Python object of any type. Only supported by Python applications.
 
### Publishing streams
An application/microservice publishes streams using the `Publish` operator in SPL or `publish` methods in Java or Python. Only the _topic_ is specified as type is taken from the actual type of the stream.

Three common patterns for publishing a stream are:
 * Publish the stream as its SPL/structured type.
 * Publsh the stream as JSON
 * Publish the stream as its SPL/structed type and as JSON by transforming the structured stream to JSON and then publishing using `TupleToJSON` operator in SPL or equivalent Java/Python methods.
     * The two streams can be published to the same _topic_ with different types since subscription is by _topic_ **and** _type_
     * or published to two different, but related, topics, for example:
         * `twitter/tweets/users/new`
         * `twitter/tweets/users/new/json`

Publishing using the SPL/structured schema will most likely perform better than JSON but lacks flexibility with schema evolution. See the _schema evolution & compability_ advanced topic below.

Again, designing how streams will be published and the scheme used for topic levels and filters is important before devloping a complete system using streaming microservices.
 
 Also see these advanced topics - TO BE WRITTEN:
  * Schema evolution & compability of published streams - 
  * Congestion - better name needed
  * Filtering
  
## Subscribing to streams
An application/microservice subcribes to streams using the `Subscribe` operator in SPL or `subscribe` methods in Java or Python specifying the _topic filter_ and _type_.

### Subscribe topic filter

A _topic filter_ matches published topics. When a filter matches multiple published topics the resultant subscribed stream will contain tuples from all publishers publishing to the set of matching topics.

Note that for any matching topic a match is **only** made if published and subscribed _type_ match as well.

A _topic filter_ has a similar syntax to a topic with the addition support of two wildcards:
 * `+` - single level wildcard - Matches any topic at its level, it must be at a level by itself, e.g. `streamsx/transportation/nextbus/+/locations`
 * `#` - multuple level wildcard - Matches any topic at its level or below, it must be used as the last level of the filter, e.g. `streamsx/transportation/nextbus/#`
 
 A _topic filter_ without a wild card character is an exact match for the topic, that is a filter of `twitter/tweets/text` only matches `twitter/tweets/text`.
 
 Single-level filter (`+`) match examples are:
  * filter `+` matches `a` and `b` but not `a/b`
  * filter `a/+` matches `a/b`, `a/c` and `a/` but not `a`, `b/c` or `a/b/c`
  * filter `+/+` matches `a/b`, `b/c`, `d/` and `/` but not `a` or `a/b/c`

Multi-level filter (`#`) match examples are:
  * filter `#` matches every topic name such as `a`, `b/c`, `//`
  * filter `a/b/#` matches `a/b` (parent), `a/b/c`, `a/b/d` and `a/b/c/d`

### Susbcribe type

Publish-susbcribe always matches on **exact** type:
  * SPL/structured schemas - Schemas must be identical same number, order and name and type of attributes.
  * JSON - Matches JSON streams according to _topic filter_ regardless of content of the JSON.
  * Java Object - Matches exactly on the declared type of the stream.
     * No sub-classing is performed, e.g. if interface `B` extends `A` then subscribing using `B` will **not** match a stream published using `A`.
     * The subscriber must have access to the Java classes for all objects that are present on the stream.
   * Python object - Matches any published Python object stream, Python object streams are not typed, just arbitrary Python objects.

## Microservice API
A microservice effectively defines its API through its published and subscribed streams. For example a twitter microservice could declare its API as publishing two streams of:
 * Published raw tweets with topic `twitter/tweets/raw` with schema `Json`
 * Published text of tweets with topic `twitter/tweets/text` with schema `String`
 
 Similarly a subscribing application can define its API through the streams it subscribes to and any it publishes. For example a matching microservice might declare its api as:
  * Subscribes to topic `twitter/tweets/raw` with schema `Json`
  * Publishes alerts to topic `twitter/alerts` with schema `Json`.
 
## Data guarantees

Publish-subscribe is an **at-most-once** model between publisher and subscriber.

Specifically:
  * A subscriber will see tuples submitted from a publisher after the connection is made.
  * A subscriber configured with a buffered connection with a `DropLast` or `DropFirst` policy will drop tuples from publishers when it cannot keep up with the incoming rate from all publishers.
  * A publisher configured with a congestion policy of XXXX (TODO) can break connections with subscribers that would cause it to block. When a connection is broken tuples submitted by the publisher are not seen by disconnected subscribers, once a connection is restablished a subscriber will see any tuples subsequently submitted by the publisher.
  * Otherwise without failures or PE restarts, tuples are not lost between publisher and subscriber
  * Upon failure or restart of the PE containing the subscribe, tuples submitted by publishers are lost until the connection is re-established .Other subscribers are unaffected.
  * Upon failure or restart of the PE containing a publisher, tuples from that publisher may be lost by subscribers . Other publishers are unaffected.
