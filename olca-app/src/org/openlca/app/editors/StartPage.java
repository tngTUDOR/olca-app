package org.openlca.app.editors;

import java.util.HashMap;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.EclipseCommandLine;
import org.openlca.app.util.UI;
import org.openlca.util.OS;

import com.google.gson.Gson;

public class StartPage extends SimpleFormEditor {

	public static String TYPE = "olca.StartPage";

	public static void open() {
		Editors.open(new SimpleEditorInput(TYPE, UUID.randomUUID().toString(), M.Welcome), TYPE);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		public Page() {
			super(StartPage.this, "olca.StartPage.Page", M.Welcome);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = mform.getForm();
			Composite comp = form.getBody();
			comp.setLayout(new FillLayout());
			Browser browser = new Browser(comp, SWT.NONE);
			browser.setJavascriptEnabled(true);
			UI.bindFunction(browser, "onOpenLink", (args) -> {
				if (args == null || args.length == 0)
					return null;
				Object s = args[0];
				if (!(s instanceof String))
					return null;
				Desktop.browse(s.toString());
				return null;
			});

			UI.onLoaded(browser, HtmlFolder.getUrl("home.html"), () -> {
				HashMap<String, String> props = new HashMap<>();
				props.put("version", getVersion());
				String lang = EclipseCommandLine.getArg("nl");
				props.put("lang", lang == null ? "en" : lang);
				String config = new Gson().toJson(props);
				browser.execute("setData(" + config + ")");

			});
		}

		private String getVersion() {
			String v = App.getVersion() + " (" + OS.get();
			String osarch = System.getProperty("os.arch");
			if (osarch != null)
				switch (osarch) {
				case "amd64":
					v += " " + "64 bit";
					break;
				case "x86":
				case "i386":
					v += " " + "32 bit";
				}
			return v += ")";
		}
	}
}
