package org.openlca.app.editors.lcia;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

class ImpactMethodInfoPage extends ModelPage<ImpactMethod> {

	private TableViewer indicatorTable;
	private final ImpactMethodEditor editor;

	ImpactMethodInfoPage(ImpactMethodEditor editor) {
		super(editor, "ImpactMethodInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var info = new InfoSection(getEditor()).render(body, tk);
		var comp = info.composite();
		modelLink(comp, M.Source, "source");
		text(comp, M.Code, "code");

		createIndicatorTable(tk, body);
		body.setFocus();
		form.reflow(true);
	}

	private void createIndicatorTable(FormToolkit tk, Composite body) {
		var section = UI.section(body, tk, M.ImpactCategories);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		indicatorTable = Tables.createViewer(comp,
			M.Name, M.Description, M.ReferenceUnit, "");
		indicatorTable.setLabelProvider(new ImpactLabel());
		var method = editor.getModel();
		var impacts = method.impactCategories;
		impacts.sort((c1, c2) -> Strings.compare(c1.name, c2.name));
		indicatorTable.setInput(impacts);
		Tables.bindColumnWidths(indicatorTable, 0.5, 0.25, 0.22);

		if (isEditable()) {
			bindActions(indicatorTable, section);
		} else {
			Tables.onDoubleClick(indicatorTable, $ -> {
				if (Viewers.getFirstSelected(indicatorTable) instanceof ImpactCategory i) {
					App.open(i);
				}
			});
		}
	}

	private void bindActions(TableViewer table, Section section) {
		ModelTransfer.onDrop(table.getTable(), this::onAdd);
		var add = Actions.onAdd(
			() -> onAdd(ModelSelector.multiSelect(ModelType.IMPACT_CATEGORY)));
		var remove = Actions.onRemove(this::onRemove);
		var copy = TableClipboard.onCopySelected(table);
		var open = Actions.onOpen(() -> {
			if (Viewers.getFirstSelected(table) instanceof ImpactCategory i) {
				App.open(i);
			}
		});
		Actions.bind(table, add, remove, open, copy);
		CommentAction.bindTo(section, "impactCategories",
			editor.getComments(), add, remove);
		Tables.onDeletePressed(table, $ -> onRemove());
		Tables.onDoubleClick(table, $ -> {
			if (Viewers.getFirstSelected(table) instanceof ImpactCategory i) {
				App.open(i);
			} else {
				add.run();
			}
		});
	}

	private void onAdd(List<? extends Descriptor> ds) {
		if (ds.isEmpty())
			return;
		var method = editor.getModel();
		var dao = new ImpactCategoryDao(Database.get());
		var newImpacts = ds.stream()
			.filter(d -> d.type == ModelType.IMPACT_CATEGORY)
			.map(d -> dao.getForId(d.id))
			.filter(Objects::nonNull)
			.filter(impact -> !method.impactCategories.contains(impact))
			.toList();
		if (newImpacts.isEmpty())
			return;
		method.impactCategories.addAll(newImpacts);
		indicatorTable.setInput(method.impactCategories);
		fireCategoryChange();
	}

	private void onRemove() {
		var method = editor.getModel();
		List<ImpactCategory> impacts = Viewers.getAllSelected(indicatorTable);
		for (var impact : impacts) {
			method.impactCategories.remove(impact);
			for (var set : method.nwSets) {
				var factor = set.getFactor(impact);
				if (factor != null) {
					set.factors.remove(factor);
				}
			}
		}
		indicatorTable.setInput(method.impactCategories);
		fireCategoryChange();
	}

	private void fireCategoryChange() {
		editor.emitEvent(editor.IMPACT_CATEGORY_CHANGE);
		editor.setDirty(true);
	}

	private class ImpactLabel extends LabelProvider implements
		ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ImpactCategory impact))
				return null;
			return switch (col) {
				case 0 -> Images.get(impact);
				case 3 -> Images.get(editor.getComments(), CommentPaths.get(impact));
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactCategory impact))
				return null;
			return switch (col) {
				case 0 -> Labels.name(impact);
				case 1 -> impact.description;
				case 2 -> impact.referenceUnit;
				default -> null;
			};
		}
	}
}
