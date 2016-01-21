package org.openlca.app.editors.projects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;

class ProjectInfoPage extends ModelPage<Project> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	public ProjectInfoPage(ProjectEditor editor) {
		super(editor, "ProjectInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createGoalAndScopeSection(body);
		createTimeInfoSection(body);
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(Messages.Project + ": " + getModel().getName());
	}

	private void createGoalAndScopeSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.GoalAndScope);

		createMultiText(Messages.Goal, "goal", composite);
		createMultiText(Messages.FunctionalUnit, "functionalUnit", composite);
	}

	private void createTimeInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.TimeAndAuthor);

		createReadOnly(Messages.CreationDate, "creationDate", composite);
		createReadOnly(Messages.LastModificationDate, "lastModificationDate",
				composite);
		createDropComponent(Messages.Author, "author", ModelType.ACTOR,
				composite);
	}

}
