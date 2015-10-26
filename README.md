# Protelis demo application

This project demonstrates a minimal application visualizing the use of 
Protelis in a simulator (in particular, using [NASA's WorldWind GIS/visualization framework](http://worldwind.arc.nasa.gov/java/)).

Protelis requires Java 8+.  This project has been set up to run in Eclipse with Maven, and should be able to be 
directly imported and executed if you have these set up.

## Contents:

* src/main/java:
  * SimpleVisualizedSimulation.java: entry point to run the demo on a simple simulated network
  * SimpleDevice.java: devices hosting a ProtelisVM and network interface
  * CachingNetworkManager.java: network interface for simulation, which simply records the 
 	most recent values sent to neighbors and received from neighbors.
  * IntegerUID.java: utility class for numerical device identifiers
  * visualizer/: package with visualization classes adapted from NASA WorldWind examples
    * visualizer/WorldWindVisualization.java: simple visualization module, which can also be independently
  	  executed to test whether visualization is working on your system.
  	* visualizer/util/*: classes to help with visualization window
* src/main/protelis:
  * hello.pt: Protelis program to be executed
* src/main/resources:
  * config/protelisww.xml: XML file setting configuration of WorldWind visualization, including
  	where the view is originally pointing and what file contains visualization layers
  * config/protelislayers.xml: XML file that specifies the visualization layers available
  	to be drawn, such as aerial imagery, maps, and navigational overlays
* pom.xml: Maven configuration of the project
* README.md: this file

## To run:

To run normally, execute "SimpleVisualizedSimulation"

You should see a set of 25 blue half-ellipse icons with black chevrons appear in
a grid over a map of part of Cambridge, Massachusetts, USA.  Each of these represents
a quad-copter flying a few hundred meters above the ground.  These quad-copters use
short-range wireless communication to talk with others within a 500 meter range.
The quad-copters should jitter around as they move randomly.

You can navigate around by clicking, dragging, and scrolling, as well as using the
clickable navigation interface, and can turn layers of the visualization on or off.

## To run against local Protelis:

To run against a local (e.g., development or pre-release) version of Protelis,
delete the Protelis dependency from pom.xml, then use Eclipse project properties
to change the Build Path to point to your local copy or Protelis.
