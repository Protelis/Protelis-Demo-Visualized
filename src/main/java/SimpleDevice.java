import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import it.unibo.alchemist.model.interfaces.IPosition;

import java.util.HashMap;
import java.util.Map;

import org.danilopianini.lang.util.FasterString;
import org.protelis.lang.datatype.DeviceUID;
import org.protelis.lang.datatype.Tuple;
import org.protelis.vm.IProgram;
import org.protelis.vm.ProtelisVM;
import org.protelis.vm.impl.AbstractExecutionContext;

/**
 * A simple implementation of a Protelis-based device, encapsulating
 * a ProtelisVM and a network interface
 */
public class SimpleDevice extends AbstractExecutionContext {
	/** Device numerical identifier */
	private final IntegerUID uid;
	/** The Protelis VM to be executed by the device */
	private final ProtelisVM vm;
	private Position position;
	
	/**
	 * Standard constructor
	 */
	public SimpleDevice(IProgram program, int uid, Position position) {
		super(new CachingNetworkManager());
		this.uid = new IntegerUID(uid);
		this.position = position;
		
		// Finish making the new device and add it to our collection
		vm = new ProtelisVM(program, this);
	}
	
	/** 
	 * Internal-only lightweight constructor to support "instance"
	 */
	private SimpleDevice(IntegerUID uid) {
		super(new CachingNetworkManager());
		this.uid = uid;
		vm = null;
	}
	
	/** 
	 * Accessor for virtual machine, to allow external execution triggering 
	 */
	public ProtelisVM getVM() {
		return vm;
	}
	/** 
	 * Test actuator that dumps a string message to the output
	 */
	public void announce(String message) {
		SimpleVisualizedSimulation.out.println(message);
	}
	
	private static final double EARTH_RADIUS = 6.371e6;
	/**
	 * Move in a direction specified by the 3-tuple vector in meters
	 * Uses a kludge vector in which +X = East, +Y = North
	 * @param vector
	 */
	public void move(Tuple vector) {
		double radius = EARTH_RADIUS + position.elevation;
		double degreesPerMeter = 360 / (2 * Math.PI * radius);
		double newLon = position.longitude.degrees + degreesPerMeter * (Double)vector.get(0);
		double newLat = position.latitude.degrees + degreesPerMeter * (Double)vector.get(1);
		double newElevation = position.elevation + (Double)vector.get(2);
		position = Position.fromDegrees(newLat, newLon, newElevation);
	}

	public Position getPosition() {
		return position;
	}

	/** 
	 * Expose the network manager, to allow external simulation of network
	 * For real devices, the NetworkManager usually runs autonomously in its own thread(s)
     */
	public CachingNetworkManager accessNetworkManager() {
		return (CachingNetworkManager)super.getNetworkManager();
	}
	
	@Override
	public DeviceUID getDeviceUID() {
		return uid;
	}

	@Override
	public Number getCurrentTime() {
		return System.currentTimeMillis();
	}

	/** Cache storage of environment information */
	private Map<FasterString, Object> environment = new HashMap<>();
	
	/**
	 * Take a snapshot of the current collection of environment variables,
	 * to be 
	 */
	@Override
	protected Map<FasterString, Object> currentEnvironment() {
		return environment;
	}

	/**
	 * Take a snapshot of the current collection of environment variables
	 */
	@Override
	protected void setEnvironment(Map<FasterString, Object> newEnvironment) {
		environment = newEnvironment;
	}

	@Override
	protected AbstractExecutionContext instance() {
		return new SimpleDevice(uid);
	}

	/** 
	 * Note: this should be going away in the future, to be replaced by sensor fields
	 */
	@Override
	public double distanceTo(DeviceUID target) {
		// No real distance information, just self vs. other
		if(target.equals(uid)) { return 0; } else { return 1; }
	}

	/** 
	 * Note: this should be going away in the future, to be replaced by sensor fields
	 */
	@Override
	public IPosition getDevicePosition() {
		return null; // Devices don't know their own positions
	}

	/** 
	 * Note: this should be going away in the future, to be replaced by standard Java random
	 */
	@Override
	public double nextRandomDouble() {
		return Math.random();
	}
}
