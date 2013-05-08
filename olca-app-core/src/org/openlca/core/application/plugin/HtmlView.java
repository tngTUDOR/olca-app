package org.openlca.core.application.plugin;

import static org.openlca.core.application.plugin.Resource.USAGE_VIEW_JS;

import java.io.File;
import java.io.IOException;

import org.openlca.ui.html.HtmlFolder;
import org.openlca.ui.html.HtmlResource;
import org.openlca.ui.html.IHtmlResource;

public enum HtmlView {

	USAGES_VIEW("usages_view.html", USAGE_VIEW_JS);

	private IHtmlResource resource;
	private Resource[] dependencies;

	private HtmlView(String fileName, Resource... dependencies) {
		this.resource = new HtmlResource(Activator.getDefault().getBundle(),
				"html" + File.separator + fileName, fileName);
		this.dependencies = dependencies;
	}

	public IHtmlResource getResource() {
		try {
			if (dependencies != null)
				for (Resource dependency : dependencies)
					HtmlFolder.register(dependency.getResource());
			HtmlFolder.register(resource);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resource;
	}

}