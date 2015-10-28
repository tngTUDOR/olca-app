package org.openlca.app.cloud.ui.compare;

import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.AbstractViewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ModelTree extends AbstractViewer<JsonNode, TreeViewer> {

	private boolean local;
	private ModelTree counterpart;

	ModelTree(Composite parent, boolean local) {
		super(parent);
		this.local = local;
	}

	void setCounterpart(ModelTree counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.NO_FOCUS
				| SWT.HIDE_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.getTree().getVerticalBar()
				.addSelectionListener(new ScrollListener());
		viewer.addSelectionChangedListener(new SelectionChangedListener());
		viewer.addTreeListener(new ExpansionListener());
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		if (local)
			tree.getVerticalBar().setVisible(false);
		return viewer;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	private void updateOtherScrollBar() {
		ScrollBar bar = this.getViewer().getTree().getVerticalBar();
		ScrollBar otherBar = counterpart.getViewer().getTree().getVerticalBar();
		otherBar.setSelection(bar.getSelection());
		if (local)
			bar.setVisible(false);
		else
			otherBar.setVisible(false);
	}

	public List<JsonNode> getSelection() {
		return Viewers.getAllSelected(getViewer());
	}

	private class ScrollListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent e) {
			updateOtherScrollBar();
		}

	}

	private class ExpansionListener implements ITreeViewerListener {

		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			setExpanded(event.getElement(), true);
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
			setExpanded(event.getElement(), false);
		}

		private void setExpanded(Object element, boolean value) {
			if (!(element instanceof JsonNode))
				return;
			JsonNode node = (JsonNode) element;
			counterpart.getViewer().setExpandedState(node, value);
			updateOtherScrollBar();
		}

	}

	private class SelectionChangedListener implements ISelectionChangedListener {

		private boolean pauseListening;

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (pauseListening)
				return;
			pauseListening = true;
			counterpart.getViewer().setSelection(event.getSelection());
			pauseListening = false;
		}

	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (!(inputElement instanceof Object[]))
				return null;
			Object[] array = (Object[]) inputElement;
			if (array.length == 0)
				return null;
			if (!(array[0] instanceof JsonNode))
				return null;
			return ((JsonNode) array[0]).children.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof JsonNode))
				return null;
			JsonNode node = (JsonNode) parentElement;
			return node.children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			JsonNode node = (JsonNode) element;
			return node.parent;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof JsonNode))
				return false;
			JsonNode node = (JsonNode) element;
			return !node.children.isEmpty();
		}

	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements IColorProvider {

		@Override
		public String getText(Object obj) {
			if (!(obj instanceof JsonNode))
				return null;
			JsonNode node = (JsonNode) obj;
			JsonElement element = local ? node.getLocalElement() : node
					.getRemoteElement();
			if (element == null)
				if (isEmptyArrayElement(node))
					return null;
				else if (isEmptyArrayElement(node.parent))
					return null;
				else
					return node.key + ": null";
			if (element.isJsonNull())
				return node.key + ": null";
			if (element.isJsonArray())
				if (element.getAsJsonArray().size() == 0)
					return node.key + ": null";
				else
					return node.key;
			if (element.isJsonObject()) {
				JsonObject object = element.getAsJsonObject();
				if (!JsonUtil.isReference(object))
					return node.key;
				return node.key + ": " + object.get("name").getAsString();
			}
			return node.key + ": " + element.getAsString();
		}

		private boolean isEmptyArrayElement(JsonNode node) {
			JsonElement element = local ? node.getLocalElement() : node
					.getRemoteElement();
			if (element != null)
				return false;
			if (node.parent.getElement() != null)
				return node.parent.getElement().isJsonArray();
			return false;
		}

		@Override
		public Color getForeground(Object object) {
			return null;
		}

		@Override
		public Color getBackground(Object object) {
			if (!(object instanceof JsonNode))
				return null;
			JsonNode node = (JsonNode) object;
			if (node.hasEqualValues())
				return null;
			return Colors.getColor(255, 255, 128);
		}

		@Override
		public Image getImage(Object object) {
			return null;
		}

	}

}