import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.symbology.BasicTacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.SymbologyConstants;
import gov.nasa.worldwind.symbology.TacticalSymbol;
import gov.nasa.worldwind.symbology.TacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.milstd2525.MilStd2525TacticalSymbol;
import gov.nasa.worldwind.symbology.milstd2525.SymbolCode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.danilopianini.lang.util.FasterString;
import org.protelis.lang.ProtelisLoader;
import org.protelis.vm.IProgram;
import org.protelis.vm.util.CodePath;

import visualizer.WorldWindVisualization;
import visualizer.util.NetworkConnectionVisualization;

/**
 * Minimal demonstration of an application using Protelis.
 * This demonstration does the following:
 * - Uses ProtelisLoader to obtain a program from the Protelis classes
 * - Create a collection of Protelis-based devices, each encapsulating a 
 *   ProtelisVM, execution environment, and network interface
 * - Run several rounds of synchronous execution
 */
public class SimpleVisualizedSimulation {
	/** Collection of devices */
	private static List<SimpleDevice> devices = new ArrayList<>();
	/** Network for moving messages between devices */
	private static Map<SimpleDevice,Set<SimpleDevice>> network = new HashMap<>();
	
	/** WorldWind visualization */
	private static WorldWindVisualization vis = new WorldWindVisualization("Visualized Protelis");
	private static Map<SimpleDevice,TacticalSymbol> visualizations = new HashMap<>();
	private static Map<SimpleDevice,NetworkConnectionVisualization> networkVis = new HashMap<>();
	
	/** Kludged output to either standard out or a string */
	public static PrintStream out = System.out;
	public static ByteArrayOutputStream outBuffer = null;
	
	/**
	 * Entry point for executing this demonstration
	 */
	public static void main(String[] args) {
		// if the first argument is "string", then log to a string; otherwise, log to standard out
		if(args.length>0 && args[0].equals("string")) {
			outBuffer = new ByteArrayOutputStream();
			out = new PrintStream(outBuffer);
		}
		
		// Create the network of devices and connections
		out.println("Creating grid network");
		createNetwork("hello");
		
		// Run until window signals to exit
		int round = 0;
		while(true) {
			out.println("Executing round "+round++);
			synchronousUpdate();
		}
	}
	
	/**
	 * Create an N x N grid of devices, each running the inducated program.
	 * @param program
	 */
	private static void createNetwork(String protelisModuleName) {
		// Make a square grid of EDGE_LENGTH x EDGE_LENGTH devices
		final int EDGE_LENGTH = 5;
		// One of these devices will be marked in its environment as a leader
		final int LEADER_ID = 5; // should have neighbors 1, 4, 6, 9

		SimpleDevice[][] cache = new SimpleDevice[EDGE_LENGTH][EDGE_LENGTH];
		// Create devices
		for(int i=0;i<EDGE_LENGTH;i++) {
			for(int j=0;j<EDGE_LENGTH;j++) {
				int id = i*4+j;
	    		Position pos = Position.fromDegrees(42.3858+(i*0.002), -71.1515+(j*0.002), 300);
	    		
				// Parse a new copy of the program for each device:
				// it will be marked up with values as the interpreter runs
				IProgram program = ProtelisLoader.parse(protelisModuleName);

				// Create the device
				SimpleDevice executionContext = new SimpleDevice(program,id,pos);
				devices.add(executionContext);
				visualizations.put(executionContext,makeUAVSymbol(executionContext));
				// Mark the leader
				if(id==LEADER_ID) { 
					executionContext.currentEnvironment().put(new FasterString("leader"), true); 
				}
				// Remember the devices in a grid, for later setting up the network
				cache[i][j] = executionContext;
				
				// Create holders for network information
		    	network.put(executionContext,new HashSet<>());
		    	NetworkConnectionVisualization path = new NetworkConnectionVisualization(pos,new HashSet<>());
		    	vis.addVisualization(path);
		    	networkVis.put(executionContext,path);
			}
		}
		
		// Link up the network
		updateNetwork();
	}

	private static TacticalSymbol makeUAVSymbol(SimpleDevice device) {
		// Get symbol code for UAV
    	SymbolCode code = new SymbolCode();
    	code.setBattleDimension(SymbologyConstants.BATTLE_DIMENSION_AIR);
    	code.setOrderOfBattle(SymbologyConstants.ORDER_OF_BATTLE_CIVILIAN);
    	code.setStandardIdentity(SymbologyConstants.STANDARD_IDENTITY_FRIEND);
    	code.setStatus(SymbologyConstants.STATUS_PRESENT);
    	code.setScheme(SymbologyConstants.SCHEME_WARFIGHTING);
    	code.setCategory(SymbologyConstants.CATEGORY_TASKS);
    	code.setFunctionId("MFQ"); // Drone
    	
    	
		TacticalSymbol symbol = new MilStd2525TacticalSymbol(code.toString(), device.getPosition());
		TacticalSymbolAttributes attrs = new BasicTacticalSymbolAttributes();
		attrs.setScale(0.2); // Make the symbol 75% its normal size.
		symbol.setAttributes(attrs);
		symbol.setShowTextModifiers(false);
    	vis.addVisualization(symbol);
    	
		return symbol;
	}

	/**
	 * Execute every device once, then deliver updates to all neighbors.
	 */
	private static void synchronousUpdate() {
		// Execute one cycle at each device
		for(SimpleDevice d : devices) {
			d.getVM().runCycle();
		}
		// Update network connectivity
		updateNetwork();
		
		// Deliver shared-state updates over the network
		for(SimpleDevice src : network.keySet()) {
			Map<CodePath,Object> message = src.accessNetworkManager().getSendCache();
			for(SimpleDevice dst : network.get(src)) {
				dst.accessNetworkManager().receiveFromNeighbor(src.getDeviceUID(),message);
			}
		}
		
		// update positions of visualization
		for(SimpleDevice src : network.keySet()) {
			TacticalSymbol symbol = visualizations.get(src);
			symbol.setPosition(src.getPosition());
		}
		vis.triggerRedraw();
	}
	
	private static final Globe EARTH = new Earth();
	private static final double COMMUNICATION_RANGE = 500; // meters
	/**
	 * Simple unit-disc network model
	 */
	private static void updateNetwork() {
		for(SimpleDevice self : devices) {
			Vec4 pSelf = EARTH.computePointFromPosition(self.getPosition());
			Set<SimpleDevice> nbrs = network.get(self);
			for(SimpleDevice other : devices) {
				Vec4 pOther = EARTH.computePointFromPosition(other.getPosition());
				double distance = pSelf.distanceTo3(pOther);
				if(distance <= COMMUNICATION_RANGE) {
					if(!nbrs.contains(other)) {
						nbrs.add(other);
					}
				} else {
					if(nbrs.contains(other)) {
						nbrs.remove(other);
					}
				}
			}
		}
		
		for(SimpleDevice self : devices) {
			NetworkConnectionVisualization netvis = networkVis.get(self);
			netvis.setPosition(self.getPosition());
			
			Set<Position> i = new HashSet<>();
			//Set<Position> i = (Set<Position>) netvis.getPositions();
			//i.clear();
			for(SimpleDevice nbr : network.get(self)) {
				i.add(self.getPosition());
				i.add(nbr.getPosition());
			}
			netvis.setNeighbors(i);
		}
		
	}
}
