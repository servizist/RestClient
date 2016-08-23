# RestClient
A simple to use REST (HTTP) Client, with built-in Retry and Circuit Breaker patters for use in unreliable (internet/cloud) environments.

It collects various nice Java libraries: 
- OkHttp for fast and easy HTTP handling; 
- Joda Time for sensible Date/Time management;
- GSON for fast JSON serializing/deserializing
- [guava-retrying](https://github.com/rholder/guava-retrying) for retry/circuit breaker policies

#Getting started

This project is built with Maven. You just need to follow the usual steps:

    ldematte@client13-207:/projects/RestClient$ mvn install
