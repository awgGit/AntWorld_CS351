# AntWorld

#Introduction:

The team of Mike Mazzella, Andre Green, Ryan Vary, and Shea Nord have written a simple AI that will explore the world, find food, and bring it back to the nest.

#How to Use the Program:

This project contains two main() methods: one for the server: antworld.server.AntWorld
and one for the client: antworld.client.ClientRandomWalk.
The server needs to be started first.
They can be tested on one machine, but to get the full performance, the server and
and each client should be run on different machines.

#Ant Pathfinding, Movement and AI:

The ants implement a random walk between graph nodes that will distribute evenly throughout the map on startup.
Ants will hop from graph node to graph node until no other neighbors can be found, then the ants will collapse back one graph node
and search through all their neighbors nodes. To get to each node the ants call A* which will find the shortest path between the
two graph nodes and since these distances are relatively small, the computation time is minimal. While the ants are traversing these nodes looking for food,
if their health falls below a certain threshold, they will immediately go to the closest source of water by casting out lines in all directions and
picking the shortest line to follow. They heal to full and then continue with what they were doing.

When an ant finds food, a spot will be marked indicating this is a possible area where food is spawning. Worker ants will then spawn and
move along a computed A* path from the nest to the food. They will then branch out and explore the area and pick up food and move back to a
separately computed A* path from the food source to the nest. This will continue for the rest of the game duration.





~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
