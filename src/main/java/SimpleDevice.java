import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.Globe;
import java8.util.function.Function;

import org.protelis.lang.datatype.DeviceUID;
import org.protelis.lang.datatype.Field;
import org.protelis.lang.datatype.Tuple;
import org.protelis.lang.datatype.impl.ArrayTupleImpl;
import org.protelis.vm.ProtelisProgram;
import org.protelis.vm.ProtelisVM;
import org.protelis.vm.impl.AbstractExecutionContext;
import org.protelis.vm.impl.SimpleExecutionEnvironment;

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
	public SimpleDevice(ProtelisProgram program, int uid, Position position) {
		super(new SimpleExecutionEnvironment(), new CachingNetworkManager());
		this.uid = new IntegerUID(uid);
		this.position = position;
		
		// Finish making the new device and add it to our collection
		vm = new ProtelisVM(program, this);
	}
	
	/** 
	 * Internal-only lightweight constructor to support "instance"
	 */
	private SimpleDevice(IntegerUID uid) {
		super(new SimpleExecutionEnvironment(), new CachingNetworkManager());
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
	
	private static final Globe EARTH = new Earth();
	/**
	 * Move in a direction specified by the 3-tuple vector in meters
	 * Uses a kludge vector in which +X = East, +Y = North
	 * This will not work correctly in polar regions.
	 * @param vector
	 */
	public void move(Tuple vector) {
		double radius = Earth.WGS84_EQUATORIAL_RADIUS + position.elevation;
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

	/** @return Field of distances to neighbors */
	public Field nbrRange() {
		Vec4 v = EARTH.computePointFromPosition(getPosition());
		return buildField(new Function<Object,Double>() {
			public Double apply(final Object otherNode) {
				Vec4 vOther = EARTH.computePointFromPosition(((SimpleDevice)otherNode).getPosition());
				return v.distanceTo3(vOther);
			}
		}, this);
	}
	
	/** @return Field of vectors to neighbors */
	public Field nbrVector() {
		return buildField(new Function<Object,Tuple>() {
			public Tuple apply(final Object otherNode) {
				Position pDelta = ((SimpleDevice)otherNode).getPosition().subtract(getPosition());
				// TODO: note that this conversion is an approximation that will not hold near the poles
				double dN = pDelta.getLatitude().getRadians() * Earth.WGS84_POLAR_RADIUS;
				double dE = pDelta.getLongitude().getRadians() * Earth.WGS84_EQUATORIAL_RADIUS;
				return new ArrayTupleImpl(dN, dE, pDelta.getAltitude());
			}
		}, this);
	}
	
	@Override
	public Number getCurrentTime() {
		return System.currentTimeMillis();
	}

	@Override
	protected AbstractExecutionContext instance() {
		return new SimpleDevice(uid);
	}

	/** 
	 * Note: this should be going away in the future, to be replaced by standard Java random
	 */
	@Override
	public double nextRandomDouble() {
		return Math.random();
	}
}
