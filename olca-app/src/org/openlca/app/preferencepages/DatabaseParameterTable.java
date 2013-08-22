package org.openlca.app.preferencepages;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.Messages;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.Tables;
import org.openlca.app.util.Viewers;
import org.openlca.core.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DatabaseParameterTable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private TableViewer viewer;
	private Table table;

	private final String NAME = Messages.Name;
	private final String DESCRIPTION = Messages.Description;
	private final String NUMERIC_VALUE = Messages.Amount;
	private final String[] PROPERTIES = new String[] { NAME, NUMERIC_VALUE,
			DESCRIPTION };

	public DatabaseParameterTable(Composite parent) {
		createTableViewer(parent);
		createEditors();
	}

	private void createTableViewer(Composite parent) {
		viewer = Tables.createViewer(parent, PROPERTIES);
		viewer.setLabelProvider(new ParameterLabel());
		table = viewer.getTable();
		table.setEnabled(false);
		Tables.bindColumnWidths(table, 0.4, 0.3, 0.3);
	}

	private void createEditors() {
		CellEditor[] editors = new CellEditor[PROPERTIES.length];
		for (int i = 0; i < editors.length; i++)
			editors[i] = new TextCellEditor(table);
		viewer.setCellModifier(new ParameterModifier());
		viewer.setCellEditors(editors);
	}

	public Parameter getSelected() {
		return Viewers.getFirstSelected(viewer);
	}

	public void setInput(List<Parameter> parameters) {
		viewer.setInput(parameters);
	}

	public void setActions(Action... actions) {
		MenuManager menu = new MenuManager();
		for (Action action : actions)
			menu.add(action);
		table.setMenu(menu.createContextMenu(table));
	}

	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
	}

	private class ParameterLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Parameter))
				return null;
			Parameter parameter = (Parameter) element;
			switch (columnIndex) {
			case 0:
				return parameter.getName();
			case 1:
				return Double.toString(parameter.getValue());
			case 2:
				return parameter.getDescription();
			default:
				return null;
			}
		}
	}

	private class ParameterModifier implements ICellModifier {

		@Override
		public boolean canModify(Object element, String property) {
			return true;
		}

		@Override
		public Object getValue(Object element, String property) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}
			if (!(element instanceof Parameter) || property == null)
				return null;
			Parameter parameter = (Parameter) element;
			if (property.equals(NAME))
				return parameter.getName();
			else if (property.equals(NUMERIC_VALUE))
				return Double.toString(parameter.getValue());
			else if (property.equals(DESCRIPTION))
				return parameter.getDescription();
			else
				return null;
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}
			if (!(element instanceof Parameter))
				return;
			Parameter parameter = (Parameter) element;
			log.trace("modify parameter {}", parameter);
			log.trace("modify property {} with value {}", property, value);
			setValue(property, (String) value, parameter);
			viewer.refresh();
		}

		private void setValue(String property, String value, Parameter parameter) {
			if (property.equals(NAME)) {
				setParameterName(parameter, value);
			} else if (property.equals(NUMERIC_VALUE)) {
				setParameterValue(parameter, value);
			} else if (property.equals(DESCRIPTION)) {
				log.trace("set description to {}", value);
				parameter.setDescription(value);
			}
		}

		private void setParameterName(Parameter parameter, String value) {
			log.trace("set name to {}", value);
			if (Parameter.isValidName(value)) {
				parameter.setName(value);
			} else {
				Dialog.showError(table.getShell(), "Invalid parameter name: "
						+ value);
			}
		}

		private void setParameterValue(Parameter parameter, String value) {
			log.trace("set value to {}", value);
			try {
				double val = Double.parseDouble(value);
				parameter.setValue(val);
			} catch (Exception e) {
				Dialog.showError(table.getShell(), value
						+ " is not a valid number.");
			}
		}

	}
}
