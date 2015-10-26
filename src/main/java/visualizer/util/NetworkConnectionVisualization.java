package visualizer.util;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.*;

import javax.media.opengl.*;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom renderable for drawing a device's network links
 * 
 * Derived on the cube example in the WorldWind examples package by pabercrombie.
 * The cube example is Copyright NASA, under the licensing of WorldWind
 */
public class NetworkConnectionVisualization implements OrderedRenderable
{
    /** Geographic position of the device. */
    protected Position position;
    /** Geographic positions of neighbors */
    protected Iterable<Position> neighbors;
    
    /** Support object to help with pick resolution. */
    protected PickSupport pickSupport = new PickSupport();

    // Determined each frame
    protected long frameTimestamp = -1L;
    /** Cartesian position of the device, computed from {@link #position}. */
    protected Vec4 placePoint;
    /** Cartesian positions of neighbors, computed from {@link #neighbors} */
    protected Set<Vec4> nbrPoints = new HashSet<>();
    /** Distance from the eye point to the device. */
    protected double eyeDistance;
    protected Extent extent;

    /**
     * Create a visualization of the network connections of one device.  
     * Note that when there are bidirectional connections, these can be doubly drawn
     * e.g., from A-->B and B-->A
     * @param position	Position of the device whose connections are being visualized
     * @param neighbors	Position of the devices it communicates with
     */
    public NetworkConnectionVisualization(Position position, Iterable<Position> neighbors) {
        this.position = position;
        this.neighbors = neighbors;
    }
    
    /** 
     * Update the position of a device
     * @param position	new position
     */
    public void setPosition(Position position) {
    	this.position = position;
    }
    
    /** 
     * Update the position of a device's neighbors
     * @param position	collection of new positions
     */
    public void setNeighbors(Iterable<Position> neighbors) {
    	this.neighbors = neighbors;
    }

    @Override
    public void render(DrawContext dc) {
        // Render is called three times:
        // 1) During picking. The network is drawn in a single color.
        // 2) As a normal renderable. The network is added to the ordered renderable queue.
        // 3) As an OrderedRenderable. The network is drawn.

        if (this.extent != null) {
        	// Ignore if not visible
            if (!this.intersectsFrustum(dc)) return;
            // If the shape is less that a pixel in size, don't render it.
            if (dc.isSmall(this.extent, 1)) return;
        }

        if (dc.isOrderedRenderingMode()) {
            this.drawOrderedRenderable(dc, this.pickSupport);
        } else {
            this.makeOrderedRenderable(dc);
        }
    }

    /**
     * Determines whether the network intersects the view frustum.
     * @param dc the current draw context.
     * @return true if this network intersects the frustum, otherwise false.
     */
    protected boolean intersectsFrustum(DrawContext dc) {
        if (this.extent == null)
            return true; // don't know the visibility, shape hasn't been computed yet

        if (dc.isPickingMode())
            return dc.getPickFrustums().intersectsAny(this.extent);

        return dc.getView().getFrustumInModelCoordinates().intersects(this.extent);
    }

    @Override
    public double getDistanceFromEye() {
        return this.eyeDistance;
    }

    @Override
    public void pick(DrawContext dc, Point pickPoint) {
        // Use same code for rendering and picking.
        this.render(dc);
    }

    /**
     * Setup drawing state in preparation for drawing the network. State changed by this method must be restored in
     * endDrawing.
     * @param dc Active draw context.
     */
    protected void beginDrawing(DrawContext dc) {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        int attrMask = GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT;

        gl.glPushAttrib(attrMask);

//        if (!dc.isPickingMode()) {
//            dc.beginStandardLighting();
//            gl.glEnable(GL.GL_BLEND);
//            OGLUtil.applyBlending(gl, false);
//        }
    }

    /**
     * Restore drawing state changed in beginDrawing to the default.
     * @param dc Active draw context.
     */
    protected void endDrawing(DrawContext dc) {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

//        if (!dc.isPickingMode()) {
//            dc.endStandardLighting();
//        }

        gl.glPopAttrib();
    }

    /**
     * Compute per-frame attributes, and add the ordered renderable to the ordered renderable list.
     * @param dc Current draw context.
     */
    protected void makeOrderedRenderable(DrawContext dc) {
        // This method is called twice each frame: once during picking and once during rendering. We only need to
        // compute the placePoint and eye distance once per frame, so check the frame timestamp to see if this is a
        // new frame.
        if (dc.getFrameTimeStamp() != this.frameTimestamp) {
            // Convert the device's geographic position to a position in Cartesian coordinates.
            this.placePoint = dc.getGlobe().computePointFromPosition(this.position);

            // Compute the distance from the eye to the device's position.
            this.eyeDistance = dc.getView().getEyePoint().distanceTo3(this.placePoint);

            // Compute a sphere that encloses the network. We'll use this sphere for intersection calculations to determine
            // if the network is actually visible.
            double maxDist = 1; // minimum must be better than zero
            nbrPoints.clear();
            for(Position nbr : neighbors) {
            	Vec4 nbrPoint = dc.getGlobe().computePointFromPosition(nbr);
            	nbrPoints.add(nbrPoint);
            	double nbrDist = nbrPoint.distanceTo3(placePoint);
            	maxDist = Math.max(maxDist,nbrDist);
            }
            this.extent = new Sphere(this.placePoint, maxDist);

            this.frameTimestamp = dc.getFrameTimeStamp();
        }

        // Add the device to the ordered renderable list. The SceneController sorts the ordered renderables by eye
        // distance, and then renders them back to front. render will be called again in ordered rendering mode, and at
        // that point we will actually draw the network.
        dc.addOrderedRenderable(this);
    }

    /**
     * Set up drawing state, and draw the network. This method is called when the 
     * network is rendered in ordered rendering mode.
     *
     * @param dc Current draw context.
     */
    protected void drawOrderedRenderable(DrawContext dc, PickSupport pickCandidates) {
        this.beginDrawing(dc);
        try {
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
            if (dc.isPickingMode()) {
                Color pickColor = dc.getUniquePickColor();
                pickCandidates.addPickableObject(pickColor.getRGB(), this, this.position);
                gl.glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());
            } else {
            	// magenta links
            	gl.glColor3f(1.0f, 0.0f, 1.0f);
            }

            this.drawEdges(dc);
        } finally {
            this.endDrawing(dc);
        }
    }

    /**
     * Actually draw the set of edges in the current (configured) drawing context
     * @param dc Current draw context.
     */
    protected void drawEdges(DrawContext dc) {
        // Note: draw the network in OpenGL immediate mode for simplicity. Real applications may want
    	// to use vertex arrays or vertex buffer objects to achieve better performance.
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glBegin(GL2.GL_LINES);
        try {
        	for(Vec4 nbr : nbrPoints) {
        		gl.glVertex3d(placePoint.x,placePoint.y,placePoint.z);
        		gl.glVertex3d(nbr.x,nbr.y,nbr.z);
        	}
        } finally {
            gl.glEnd();
        }
    }
}
