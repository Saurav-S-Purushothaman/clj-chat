clj-chat

My solution to [Coding challenge : Build your own realtime chat server and client](https://codingchallenges.fyi/challenges/challenge-realtime-chat)

To start the server run the shell command

``` shell
clj -M -m clj-chat.core
```

The port is constant and configured to be 10000

To connect to the cleint
Use any server. In this example, I am using telnet

``` shell
telnet localhost 10000

```
