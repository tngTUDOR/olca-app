/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.editors.AnalyzeEditorInput;
import org.openlca.app.results.InventoryResultPage;
import org.openlca.app.results.InventoryResultProvider;
import org.openlca.core.database.Cache;
import org.openlca.core.editors.analyze.sankey.SankeyDiagram;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View for the analysis results of a product system.
 */
public class AnalyzeEditor extends FormEditor {

	public static final String ID = "editors.analyze";

	private Logger log = LoggerFactory.getLogger(getClass());

	private SankeyDiagram diagram;
	private int diagramIndex;
	private CalculationSetup setup;
	private AnalysisResult result;

	public CalculationSetup getSetup() {
		return setup;
	}

	public AnalysisResult getResult() {
		return result;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		AnalyzeEditorInput editorInput = (AnalyzeEditorInput) input;
		String resultKey = editorInput.getResultKey();
		String setupKey = editorInput.getSetupKey();
		result = App.getCache().remove(resultKey, AnalysisResult.class);
		setup = App.getCache().remove(setupKey, CalculationSetup.class);
		ProductSystem system = setup.getProductSystem();
		String name = Messages.ResultOf + " " + system.getName();
		setPartName(name);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new AnalyzeInfoPage(this, result, setup));

			addPage(new InventoryResultPage(this, new InventoryAdapter()));
			if (result.hasImpactResults())
				addPage(new LCIATotalPage(this, result));
			addPage(new ProcessContributionPage(this, result));
			addPage(new ProcessResultPage(this, result));
			if (result.hasImpactResults())
				addPage(new FlowImpactPage(this, result));
			addPage(new ContributionTreePage(this, result));
			addPage(new GroupPage(this, result));
			addPage(new LocationContributionPage(this, result));
			// if (FeatureFlag.SUNBURST_CHART.isEnabled())
			// addPage(new SunBurstView(this, result));
			// if (FeatureFlag.LOCALISED_LCIA.isEnabled()
			// && result.hasImpactResults())
			// addPage(new LocalisedImpactPage(this, result));
			diagram = new SankeyDiagram(setup, result);
			diagramIndex = addPage(diagram, getEditorInput());
			setPageText(diagramIndex, "Sankey diagram");
		} catch (final PartInitException e) {
			log.error("Add pages failed", e);
		}
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {

	}

	public SankeyDiagram getDiagram() {
		return diagram;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public Object getSelectedPage() {
		Object page = super.getSelectedPage();
		if (page == null && getActivePage() == diagramIndex) {
			page = diagram;
		}
		return page;
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void close(boolean save) {
		super.close(save);
	}

	private class InventoryAdapter implements InventoryResultProvider {

		@Override
		public Collection<FlowDescriptor> getFlows(Cache cache) {
			return result.getFlowResults().getFlows(cache);
		}

		@Override
		public double getAmount(FlowDescriptor flow) {
			return result.getFlowResults().getTotalResult(flow);
		}

		@Override
		public boolean isInput(FlowDescriptor flow) {
			return result.getFlowIndex().isInput(flow.getId());
		}

	}

}
