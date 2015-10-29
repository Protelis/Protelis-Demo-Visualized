/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package visualizer;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.symbology.BasicTacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.SymbologyConstants;
import gov.nasa.worldwind.symbology.TacticalSymbol;
import gov.nasa.worldwind.symbology.TacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.milstd2525.MilStd2525TacticalSymbol;
import gov.nasa.worldwind.symbology.milstd2525.SymbolCode;
import gov.nasa.worldwind.util.StatisticsPanel;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.WWUtil;

import visualizer.util.ClickAndGoSelectListener;
import visualizer.util.HighlightController;
import visualizer.util.LayerPanel;
import visualizer.util.ToolTipController;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Provides a base application framework for simple WorldWind examples. Examine other examples in this package to see
 * how it's used.
 *
 * @version $Id: ApplicationTemplate.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WorldWindVisualization {
	static {
		// Set location of config document: note - this may be overridden by a similar statement
		// in any other class that previously invoked a WorldWind routine
		System.setProperty("gov.nasa.worldwind.app.config.document", "config/protelisww.xml");

		// OS-specific graphics customization
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        } else if (Configuration.isWindowsOS()) {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

	/** Java window where the visualization and controls will appear */
    private final AppFrame frame;
    /** Set of layers specified dynamically by the user rather than in the configuration file */
	private Map<String,RenderableLayer> extraLayers = new HashMap<>();
    
	/**
	 * Create a new visualization window.
	 * Note that by default the application is bound to close when the window closes
	 * @param windowName Name for the window
	 */
    public WorldWindVisualization(final String windowName) {
    	frame = start(windowName,AppFrame.class);
    }
    
    /**
     * Declare a set of layers that will be drawn in the specified order.
     * @param names  List of layer names in order
     */
    public void orderLayers(final String ... names) {
    	for (String n : names) {
    		ensureLayer(n);
    	}
    }
    
    /** 
     * Add a visualization element.
     * @param element	Visualization object to be added
     * @param layerName Layer to be midified; if the layer doesn't exist, it will be created
     */
    public void addVisualization(final Renderable element, final String layerName) {
    	RenderableLayer layer = ensureLayer(layerName);
    	layer.addRenderable(element);
    }
    
    /** 
     * Remove a visualization element.
     * @param element	Visualization object to be removed
     * @param layerName Layer to be modified; if the layer doesn't exist, it will be created
     */
    public void removeVisualization(final Renderable element, final String layerName) {
    	RenderableLayer layer = ensureLayer(layerName);
    	layer.removeRenderable(element);
    }
    
    /** 
     * Clear all visualizations from a layer.
     * @param layerName Layer to be midified; if the layer doesn't exist, it will be created
     */
    public void clearVisualization(final String layerName) {
    	RenderableLayer layer = ensureLayer(layerName);
    	layer.removeAllRenderables();
    }

    /**
     * Inform the visualizer that the simulation has updated, and needs to be re-drawn.
     */
    public void triggerRedraw() {
    	frame.getWwd().redraw();
    }
    
    /**
     * Make sure that a layer exists and is inserted
     */
    private RenderableLayer ensureLayer(final String layerName) {
    	if (!extraLayers.containsKey(layerName)) {
    		RenderableLayer newLayer = new RenderableLayer();
    		insertBeforeCompass(frame.getWwd(), newLayer);
    		extraLayers.put(layerName, newLayer);
    	}
    	return extraLayers.get(layerName);
    }
    
    /**
     * Insert a layer that will be drawn on top of all terrain imagery but below controls.
     * @param wwd	window where it will be drawn
     * @param layer	layer to be inserted
     */
    public static void insertBeforeCompass(final WorldWindow wwd, final Layer layer) {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers) {
            if (l instanceof CompassLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition, layer);
    }

//    private static void insertBeforePlacenames(WorldWindow wwd, Layer layer)
//    {
//        // Insert the layer into the layer list just before the placenames.
//        int compassPosition = 0;
//        LayerList layers = wwd.getModel().getLayers();
//        for (Layer l : layers)
//        {
//            if (l instanceof PlaceNameLayer)
//                compassPosition = layers.indexOf(l);
//        }
//        layers.add(compassPosition, layer);
//    }

//    private static void insertAfterPlacenames(WorldWindow wwd, Layer layer)
//    {
//        // Insert the layer into the layer list just after the placenames.
//        int compassPosition = 0;
//        LayerList layers = wwd.getModel().getLayers();
//        for (Layer l : layers)
//        {
//            if (l instanceof PlaceNameLayer)
//                compassPosition = layers.indexOf(l);
//        }
//        layers.add(compassPosition + 1, layer);
//    }

//    private static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName)
//    {
//        // Insert the layer into the layer list just before the target layer.
//        int targetPosition = 0;
//        LayerList layers = wwd.getModel().getLayers();
//        for (Layer l : layers)
//        {
//            if (l.getName().indexOf(targetName) != -1)
//            {
//                targetPosition = layers.indexOf(l);
//                break;
//            }
//        }
//        layers.add(targetPosition, layer);
//    }

    private static AppFrame start(final String windowName, final Class<AppFrame> appFrameClass) {
        if (Configuration.isMacOS() && windowName != null) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", windowName);
        }

        try {
            final AppFrame frame = new AppFrame(true, true, false);
            frame.setTitle(windowName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    frame.setVisible(true);
                }
            });

            return frame;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class AppPanel extends JPanel {
		private static final long serialVersionUID = 8394322655971367590L;
		private WorldWindow wwd;
	    private StatusBar statusBar;
//	    private ToolTipController toolTipController;
//	    private HighlightController highlightController;
	
	    public AppPanel(final Dimension canvasSize, final boolean includeStatusBar) {
	        super(new BorderLayout());
	
	        this.wwd = this.createWorldWindow();
	        ((Component) this.wwd).setPreferredSize(canvasSize);
	
	        // Create the default model as described in the current worldwind properties.
	        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
	        this.wwd.setModel(m);
	
	        // Setup a select listener for the worldmap click-and-go feature
	        this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));
	
	        this.add((Component) this.wwd, BorderLayout.CENTER);
	        if (includeStatusBar) {
	            this.statusBar = new StatusBar();
	            this.add(statusBar, BorderLayout.PAGE_END);
	            this.statusBar.setEventSource(wwd);
	        }
	
	        // Add controllers to manage highlighting and tool tips.
	        //this.toolTipController = 
	        		new ToolTipController(this.getWwd(), AVKey.DISPLAY_NAME, null);
	        //this.highlightController = 
	        		new HighlightController(this.getWwd(), SelectEvent.ROLLOVER);
	    }
	
	    protected WorldWindow createWorldWindow() {
	        return new WorldWindowGLCanvas();
	    }
	
	    public WorldWindow getWwd() {
	        return wwd;
	    }
	
//	    public StatusBar getStatusBar() {
//	        return statusBar;
//	    }
	}

	private static class AppFrame extends JFrame {
		private static final long serialVersionUID = 1902870695296019698L;
	
		private static final int DEFAULT_WIDTH = 800, DEFAULT_HEIGHT = 600;
		private Dimension canvasSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	
	    private AppPanel wwjPanel;
	    private LayerPanel layerPanel;
	    private StatisticsPanel statsPanel;
	
	    public AppFrame(final boolean includeStatusBar, final boolean includeLayerPanel, final boolean includeStatsPanel) {
	        this.initialize(includeStatusBar, includeLayerPanel, includeStatsPanel);
	    }
	
	    protected void initialize(final boolean includeStatusBar, final boolean includeLayerPanel, final boolean includeStatsPanel) {
	        // Create the WorldWindow.
	        this.wwjPanel = this.createAppPanel(this.canvasSize, includeStatusBar);
	        this.wwjPanel.setPreferredSize(canvasSize);
	
	        // Put the pieces together.
	        this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
	        if (includeLayerPanel) {
	            this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(), null);
	            this.getContentPane().add(this.layerPanel, BorderLayout.WEST);
	        }
	
	        if (includeStatsPanel || System.getProperty("gov.nasa.worldwind.showStatistics") != null) {
	        	final int defaultStatsWidth = 250;
	            this.statsPanel = new StatisticsPanel(this.wwjPanel.getWwd(), new Dimension(defaultStatsWidth, canvasSize.height));
	            this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
	        }
	
	        // Create and install the view controls layer and register a controller for it with the World Window.
	        ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
	        insertBeforeCompass(getWwd(), viewControlsLayer);
	        this.getWwd().addSelectListener(new ViewControlsSelectListener(this.getWwd(), viewControlsLayer));
	
	        // Register a rendering exception listener that's notified when exceptions occur during rendering.
	        this.wwjPanel.getWwd().addRenderingExceptionListener(new RenderingExceptionListener() {
	            public void exceptionThrown(final Throwable t) {
	                if (t instanceof WWAbsentRequirementException) {
	                    String message = "Computer does not meet minimum graphics requirements.\n";
	                    message += "Please install up-to-date graphics driver and try again.\n";
	                    message += "Reason: " + t.getMessage() + "\n";
	                    message += "This program will end when you press OK.";
	
	                    JOptionPane.showMessageDialog(AppFrame.this, message, "Unable to Start Program",
	                        JOptionPane.ERROR_MESSAGE);
	                    System.exit(-1);
	                }
	            }
	        });
	
	        // Search the layer list for layers that are also select listeners and register them with the World
	        // Window. This enables interactive layers to be included without specific knowledge of them here.
	        for (Layer layer : this.wwjPanel.getWwd().getModel().getLayers()) {
	            if (layer instanceof SelectListener) {
	                this.getWwd().addSelectListener((SelectListener) layer);
	            }
	        }
	
	        this.pack();
	
	        // Center the application on the screen.
	        WWUtil.alignComponent(null, this, AVKey.CENTER);
	        this.setResizable(true);
	    }
	
	    protected AppPanel createAppPanel(final Dimension size, final boolean statusBar) {
	        return new AppPanel(size, statusBar);
	    }
	
	    public WorldWindow getWwd() {
	        return this.wwjPanel.getWwd();
	    }
	
//	    public Dimension getCanvasSize() {
//	        return canvasSize;
//	    }
//	
//	    public AppPanel getWwjPanel() {
//	        return wwjPanel;
//	    }
//	
//	    public StatusBar getStatusBar() {
//	        return this.wwjPanel.getStatusBar();
//	    }
//	
//	    public LayerPanel getLayerPanel() {
//	        return layerPanel;
//	    }
//	
//	    public StatisticsPanel getStatsPanel() {
//	        return statsPanel;
//	    }
//	
//	    public void setToolTipController(final ToolTipController controller) {
//	        if (this.wwjPanel.toolTipController != null) {
//	            this.wwjPanel.toolTipController.dispose();
//	        }
//	        this.wwjPanel.toolTipController = controller;
//	    }
//	
//	    public void setHighlightController(final HighlightController controller) {
//	        if (this.wwjPanel.highlightController != null) {
//	            this.wwjPanel.highlightController.dispose();
//	        }
//	        this.wwjPanel.highlightController = controller;
//	    }
	}

	/**
     * Scratch test of flying 1000 random UAVs over Fresh pond in Cambridge, MA.
     * @param args Command-line arguments
     */
    public static void main(final String[] args) {
    	WorldWindVisualization vis = new WorldWindVisualization("World Wind Application");
    	
    	// Create a symbol for a civilian UAV
    	SymbolCode code = new SymbolCode();
    	code.setBattleDimension(SymbologyConstants.BATTLE_DIMENSION_AIR);
    	code.setOrderOfBattle(SymbologyConstants.ORDER_OF_BATTLE_CIVILIAN);
    	code.setStandardIdentity(SymbologyConstants.STANDARD_IDENTITY_FRIEND);
    	code.setStatus(SymbologyConstants.STATUS_PRESENT);
    	code.setScheme(SymbologyConstants.SCHEME_WARFIGHTING);
    	code.setCategory(SymbologyConstants.CATEGORY_TASKS);
    	code.setFunctionId("MFQ"); // Drone
    	
    	// Default parameters
		final double range = 0.1;
		final double graphicScale = 0.2;
		final double centerLat = 42.3898, centerLon = -71.1475, centerEl = 100;
    	// Put 100 into the map
    	for (int i = 1; i < 1000; i++) {
    		double offsetx = (Math.random() - 0.5) * range, offsety = (Math.random() - 0.5) * range;
    		Position position = Position.fromDegrees(centerLat + offsetx, centerLon + offsety, centerEl);
    		TacticalSymbol symbol = new MilStd2525TacticalSymbol(code.toString(), position);
    		TacticalSymbolAttributes attrs = new BasicTacticalSymbolAttributes();
    		attrs.setScale(graphicScale);
    		symbol.setAttributes(attrs);
    		symbol.setShowTextModifiers(false);
        	vis.addVisualization(symbol,"Symbols");
    	}
    }
}
