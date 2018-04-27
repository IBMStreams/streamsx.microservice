# Schema Evolution & Compability

## Overview
As a microservice evolves it might have the need to modify the data it publishes, for example adding an addition attribute to each tuple.
In order to maintain the indepdence between publishers and subscribers such changes should be made in a way that allows existing
subscribers to continue to process data correctly. That is, the model is that:

 * Some existing publishers might continue to run producing the old style data
 * Existing publishers may modified and restarted to produce the new style data
 * New publishers may be created that produce the new style data
 * Existing subscribers continue to run without restarting, subscribing to the same topics, and continue processing correctly.

## Solutions

There are a number of solutions to schema evolution.

As a simple example assuming we have a trucking company *TruckCo* that initially publishes a stream containing live locations of its trucks
with four attributes: `truckId`, `ts` (timestamp), `latitude`, `longitude`, `speed`.

In general published streams should evolve only by adding attributes, though potentially there are cases where
an attribute is removed or changes datatype.

## JSON only

In this case microservices always publish JSON and then use the capabilities of JSON to allow schema evolution.

So:
 * Single topic `truckco/trucks/locations`
 * Tuple initially looks like `{"truckId": "AEC892", "ts": 1524849497091, "latitude": 34.933066, "longitude":-118.925333, "speed": 67}`

Schema evolution with JSON can be fairly easy if subscribers:
 * simply ignore new attributes.
 * are flexible in data type handling, e.g. handle `speed` as integer or float
    * this requires planning in what could be the possible data types, e.g. can `ts` be a string with a value like `Fri, 27 Apr 2018 17:42:15 GMT`. 
 * are developed to handle missing attributes for those that might not be available, e.g. `TruckCo` acquires
   another trucking company with older trucks that cannot provide speed so its new microservice publishes to the same
   topic using JSON but its tuples are missing the `speed` property.
    * or the microservice adds the property but uses `null` as its value.

Thus:
 * Adding an attribute such as `temperature` as a new property in the JSON object representing a tuple will simply
   be ignored by existing microservices.
 * Changing a data type for a property is pro-actively handled by subscribers, though need agreement on the potential data types and their meaning up-front.
 * Removing a property is pro-actively handled by subscribers, though need agreement which properties might be missing or removed up-front.

## Single topic

A single topic (e.g. `truckco/trucks/locations`) can be published with multiple topics, for example a structured schema and JSON.

