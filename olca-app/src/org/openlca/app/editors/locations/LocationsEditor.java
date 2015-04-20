package org.openlca.app.editors.locations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.lcia_methods.ShapeFileUtils;
import org.openlca.app.events.EventHandler;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.geo.parameter.ParameterRepository;
import org.openlca.geo.parameter.ShapeFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationsEditor extends FormEditor implements IEditor {

	public static final String ID = "editors.locations";
	private Logger log = LoggerFactory.getLogger(getClass());
	private List<Location> locations;
	private Set<Location> added = new HashSet<>();
	private Set<Location> removed = new HashSet<>();
	private Set<Location> changed = new HashSet<>();
	private Set<Location> kmlChanged = new HashSet<>();
	private LocationDao dao;
	private boolean dirty;
	private List<EventHandler> savedHandlers = new ArrayList<>();

	/**
	 * Calls the given event handler AFTER the model in this editor was saved.
	 */
	public void onSaved(EventHandler handler) {
		savedHandlers.add(handler);
	}

	void locationAdded(Location location) {
		added.add(location);
		locations.add(0, location);
		setDirty(true);
	}

	void locationRemoved(Location location) {
		if (added.contains(location))
			added.remove(location);
		else
			removed.add(location);
		if (changed.contains(location))
			changed.remove(location);
		if (kmlChanged.contains(location))
			kmlChanged.remove(location);
		locations.remove(location);
		kmlChanged.add(location);
		setDirty(true);
	}

	void locationChanged(Location location) {
		if (added.contains(location))
			return;
		if (removed.contains(location))
			return;
		changed.add(location);
		setDirty(true);
	}

	void locationKmlChanged(Location location) {
		locationChanged(location);
		kmlChanged.add(location);
		setDirty(true);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new LocationsPage(this));
		} catch (PartInitException e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		dao = new LocationDao(Database.get());
		locations = dao.getAllWithoutKmz();
		Collections.sort(locations, new LocationComparator());
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		doSave();
	}

	public void doSave() {
		for (Location added : added)
			dao.insert(added);
		added.clear();
		for (Location removed : removed)
			dao.delete(removed);
		removed.clear();
		for (Location changed : changed) {
			// since we did not load kmz from the beginning we need to do now,
			// otherwise kmz would be overwritten with null on save
			if (changed.getKmz() == null)
				changed.setKmz(dao.getKmz(changed.getId()));
			dao.update(changed);
		}
		File shapeFileDirectory = DatabaseFolder.getShapeFileDirectory(Database
				.get());
		if (!shapeFileDirectory.exists())
			return;
		ImpactMethodDao methodDao = new ImpactMethodDao(Database.get());
		List<ImpactMethodDescriptor> descriptors = methodDao.getDescriptors();
		for (ImpactMethodDescriptor method : descriptors) {
			ShapeFileRepository repo = new ShapeFileRepository(
					DatabaseFolder.getShapeFileLocation(Database.get(),
							method.getRefId()));
			ParameterRepository pRepo = new ParameterRepository(repo);
			for (String shapeFile : ShapeFileUtils.getShapeFiles(method))
				for (Location kmlChanged : kmlChanged)
					pRepo.remove(kmlChanged.getId(), shapeFile);
		}
		changed.clear();
		updateModel();
		for (EventHandler handler : savedHandlers)
			handler.handleEvent();
		setDirty(false);
	}

	@Override
	public void setDirty(boolean b) {
		if (dirty == b)
			return;
		dirty = b;
		editorDirtyStateChanged();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	List<Location> getLocations() {
		return locations;
	}

	void updateModel() {
		added.clear();
		removed.clear();
		changed.clear();
		locations = dao.getAllWithoutKmz();
		Collections.sort(locations, new LocationComparator());
	}

	private class LocationComparator implements Comparator<Location> {

		@Override
		public int compare(Location arg0, Location arg1) {
			String name1 = getSafeName(arg0);
			String name2 = getSafeName(arg1);
			return name1.compareTo(name2);
		}

		private String getSafeName(Location location) {
			if (location == null)
				return "";
			if (location.getName() == null)
				return "";
			return location.getName().toLowerCase();
		}

	}
}
