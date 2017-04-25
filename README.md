# AntWorld
This project contains two main() methods: one for the server: antworld.server.AntWorld
and one for the client: antworld.client.ClientRandomWalk

The server needs to be started first.
They can be tested on one machine, but to get the full performance, the server and
and each client should be run on different machines.

##Building and Running
In order for the server to find the resources correctly, you must mark the `resources` folder in the project root as a resource root. This will copy the inner directory `resources/resources` on build to the output directory and allow the image loading routines to find the files. **This is as of 3:25PM on 2017-04-22**, and was changd to allow the server to run within a self-contained jar.
##Grading

For Spring 2017, there are two grading phases:
1) Beat the bots: This is due Friday, May 5 at midnight. 
     Grade C: When placed in a random nest where all other nests are populated by SimpleSolidAI, your AI scores equal or slightly better 
     than the average SimpleSolidAI score after a 1 hour game in at least 1 of two trys.
     Grade B: Your AI beats average SimpleSolidAI by scoring 1.5x higher.
     Grade A: Your AI beats average SimpleSolidAI by scoring >2x higher.

2) Tourney: Tuesday, May 9 10:00 a.m.‐noon in CS lab room. 
   Snacks will be served.
   To qualify for the Tourney, your AI must score a C or better verses SimpleSolidAI.
   First Place in Tourney: +50 point and Hexbug mini robot for each member.
   Second Place in Tourney: +25 point and Hexbug mini robot for each member.
   Third Place in Tourney: +15 point and Hexbug mini robot for each member.
