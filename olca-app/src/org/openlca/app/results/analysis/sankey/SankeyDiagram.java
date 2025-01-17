package org.openlca.app.results.analysis.sankey;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.openlca.app.App;
import org.openlca.app.results.analysis.sankey.actions.SankeyMenu;
import org.openlca.app.results.analysis.sankey.model.Link;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;

public class SankeyDiagram extends GraphicalEditor implements PropertyChangeListener {

	public static final String ID = "editor.ProductSystemSankeyDiagram";
	public final DQResult dqResult;
	public final ProcessLinkSearchMap linkSearchMap;
	public final FullResult result;
	public ProductSystemNode node;
	public double zoom = 1;
	private boolean routed = false;
	private SankeyResult sankeyResult;
	private Map<ProcessLink, Link> createdLinks = new HashMap<>();
	private Map<Long, ProcessNode> createdProcesses = new HashMap<>();
	private ProductSystem productSystem;

	public SankeyDiagram(FullResult result, DQResult dqResult, CalculationSetup setup) {
		this.dqResult = dqResult;
		setEditDomain(new DefaultEditDomain(this));
		this.result = result;
		productSystem = setup.productSystem;
		linkSearchMap = new ProcessLinkSearchMap(productSystem.processLinks);
		sankeyResult = new SankeyResult(productSystem, result);
		if (productSystem != null) {
			setPartName(productSystem.name);
		}
	}

	private void createConnections(long startProcessId) {
		Set<Long> processed = new HashSet<>();
		Stack<Long> processes = new Stack<>();
		processes.add(startProcessId);
		while (!processes.isEmpty()) {
			long nextId = processes.pop();
			processed.add(nextId);
			for (ProcessLink processLink : linkSearchMap.getIncomingLinks(nextId)) {
				ProcessNode sourceNode = createdProcesses.get(processLink.providerId);
				ProcessNode targetNode = createdProcesses.get(processLink.processId);
				if (sourceNode == null || targetNode == null)
					continue;
				if (createdLinks.containsKey(processLink))
					continue;
				double ratio = sankeyResult.getLinkContribution(processLink);
				Link link = new Link(sourceNode, targetNode, processLink, ratio);
				createdLinks.put(processLink, link);
				if (processed.contains(sourceNode.process.id))
					continue;
				processes.add(sourceNode.process.id);
			}
		}
	}

	private ProcessNode createNode(CategorizedDescriptor process) {
		ProcessNode node = new ProcessNode(process);
		long processId = process.id;
		node.directContribution = sankeyResult.getDirectContribution(processId);
		node.directResult = sankeyResult.getDirectResult(processId);
		node.upstreamContribution = sankeyResult.getUpstreamContribution(processId);
		node.upstreamResult = sankeyResult.getUpstreamResult(processId);
		createdProcesses.put(process.id, node);
		return node;
	}

	private void updateConnections() {
		createConnections(productSystem.referenceProcess.id);
		for (final Link link : createdLinks.values()) {
			link.link();
		}
	}

	private void updateModel(double cutoff) {
		Map<Long, CategorizedDescriptor> processes = new HashMap<>();
		for (CategorizedDescriptor d : result.getProcesses())
			processes.put(d.id, d);
		if (cutoff == 0) {
			for (Long processId : productSystem.processes) {
				CategorizedDescriptor d = processes.get(processId);
				if (d != null) {
					node.addChild(createNode(d));
				}
			}
		} else {
			long refProcess = productSystem.referenceProcess.id;
			Set<Long> processesToDraw = SankeyProcessList.calculate(
					sankeyResult, refProcess, cutoff, linkSearchMap);
			for (final Long processId : processesToDraw) {
				CategorizedDescriptor process = processes.get(processId);
				if (process != null) {
					node.addChild(createNode(process));
				}
			}
		}
	}

	@Override
	protected void configureGraphicalViewer() {
		ArrayList<String> zoomContributions;
		// configure viewer
		super.configureGraphicalViewer();

		MenuManager menu = SankeyMenu.create(this);
		getGraphicalViewer().setContextMenu(menu);

		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new SankeyEditPartFactory());
		ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
		viewer.setRootEditPart(rootEditPart);

		ZoomManager zoomManager = rootEditPart.getZoomManager();

		// append zoom actions to action registry
		getActionRegistry().registerAction(new ZoomInAction(zoomManager));
		getActionRegistry().registerAction(new ZoomOutAction(zoomManager));

		zoomContributions = new ArrayList<>();
		zoomContributions.add(ZoomManager.FIT_ALL);
		zoomContributions.add(ZoomManager.FIT_HEIGHT);
		zoomContributions.add(ZoomManager.FIT_WIDTH);
		zoomManager.setZoomLevelContributions(zoomContributions);

		// create key handler
		KeyHandler keyHandler = new KeyHandler();
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
		viewer.setKeyHandler(keyHandler);

		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);
	}

	@Override
	protected void initializeGraphicalViewer() {
		// create new root edit part with switched layers (connection layer
		// under the process layer)
		getGraphicalViewer().setRootEditPart(new ScalableRootEditPart() {

			@Override
			protected LayeredPane createPrintableLayers() {
				LayeredPane pane = new LayeredPane();
				Layer layer = new ConnectionLayer();
				layer.setPreferredSize(new Dimension(5, 5));
				pane.add(layer, CONNECTION_LAYER);
				layer = new Layer();
				layer.setOpaque(false);
				layer.setLayoutManager(new StackLayout());
				pane.add(layer, PRIMARY_LAYER);
				return pane;
			}

		});
		// zoom listener
		((ScalableRootEditPart) getGraphicalViewer().getRootEditPart())
				.getZoomManager().addZoomListener(new ZoomListener() {
					@Override
					public void zoomChanged(final double arg0) {
						zoom = arg0;
					}
				});
		double[] zoomLevels = new double[] { 0.005, 0.01, 0.02, 0.0375, 0.075,
				0.125, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0,
				10.0, 20.0, 40.0, 80.0, 150.0, 300.0, 500.0, 1000.0 };
		((ScalableRootEditPart) getGraphicalViewer().getRootEditPart())
				.getZoomManager().setZoomLevels(zoomLevels);
		getGraphicalViewer().getEditDomain().setActiveTool(new PanningSelectionTool());
		initContent();
	}

	// TODO: avoid double calculation here
	private void initContent() {
		Object defaultSelection = getDefaultSelection();
		if (defaultSelection == null) {
			getGraphicalViewer().setContents(
					new ProductSystemNode(productSystem, this, null, 0.1));
			return;
		}
		sankeyResult.calculate(defaultSelection);
		double cutoff = sankeyResult.findCutoff(30);
		update(defaultSelection, cutoff);
	}

	public Object getDefaultSelection() {
		if (result == null)
			return null;
		if (result.hasImpactResults()) {
			Set<ImpactCategoryDescriptor> categories = result
					.getImpacts();
			if (!categories.isEmpty())
				return categories.iterator().next();
		}
		Set<FlowDescriptor> flows = result.getFlows();
		if (!flows.isEmpty())
			return flows.iterator().next();
		return null;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	public double getProductSystemResult() {
		return sankeyResult.getUpstreamResult(productSystem.referenceProcess.id);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("firstTimeInitialized")) {
			createdLinks.clear();
			updateConnections();
		}
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	public void update(Object selection, double cutoff) {
		if (selection == null || cutoff < 0d || cutoff > 1d)
			return;
		App.run("Calculate sankey results", () -> sankeyResult.calculate(selection), () -> {
			node = new ProductSystemNode(productSystem, this, selection, cutoff);
			createdProcesses.clear();
			createdLinks.clear();
			updateModel(cutoff);
			getGraphicalViewer().deselectAll();
			getGraphicalViewer().setContents(node);
			node.setRouted(routed);
		});
	}

	public boolean isRouted() {
		return routed;
	}

	public void switchRouting() {
		routed = !routed;
		if (node != null) {
			node.setRouted(routed);
		}
	}

}
