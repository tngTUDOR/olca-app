package org.openlca.app.editors.graphical.actions;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.IOPaneEditPart;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;


public class AddExchangeAction extends SelectionAction {

	private final boolean forInput;
	private final Request request;

	public AddExchangeAction(GraphEditor part, boolean forInput) {
		super(part);
		this.forInput = forInput;
		request = new Request(forInput
			? REQ_ADD_INPUT_EXCHANGE
			: REQ_ADD_OUTPUT_EXCHANGE);
		setId(forInput
			?	GraphActionIds.ADD_INPUT_EXCHANGE
			: GraphActionIds.ADD_OUTPUT_EXCHANGE);
		setText(forInput ? M.AddInputFlow : M.AddOutputFlow);
		setImageDescriptor(Images.descriptor(ModelType.FLOW));
	}

	@Override
	protected boolean calculateEnabled() {
		var command = getCommand();
		if (command == null)
			return false;
		return command.canExecute();
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	private Command getCommand() {
		if (getSelectedObjects().isEmpty())
			return null;

		CompoundCommand cc = new CompoundCommand();
		cc.setDebugLabel("Add " + (forInput ? "input" : "output") + " exchange");

		var parts = getSelectedObjects();
		for (Object o : parts) {
			var pane = o instanceof IOPaneEditPart paneEditPart
				? paneEditPart.getModel()
				: o instanceof ExchangeEditPart exchangeEditPart
				? exchangeEditPart.getModel().getIOPane()
				: null;
			if (pane == null || pane.isForInputs() != forInput)
				return null;
			var viewer = getWorkbenchPart().getAdapter(GraphicalViewer.class);
			var registry = viewer.getEditPartRegistry();
			var editPart = registry.get(pane);

			if (editPart instanceof IOPaneEditPart paneEditPart)
				cc.add(paneEditPart.getCommand(request));
		}
		return cc.unwrap();
	}

}
