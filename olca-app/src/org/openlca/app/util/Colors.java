package org.openlca.app.util;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Managed SWT colors: the colors are created on demand and disposed when the
 * application is closed.
 */
public class Colors {

	private static HashMap<RGB, Color> createdColors = new HashMap<>();

	private static Display display;

	static {
		display = PlatformUI.getWorkbench().getDisplay();
		PlatformUI.getWorkbench().addWorkbenchListener(new ShutDown());
	}

	public static Color errorColor() {
		RGB rgb = new RGB(255, 180, 180);
		return get(rgb);
	}

	public static Color get(RGB rgb) {
		Color color = createdColors.get(rgb);
		if (color == null || color.isDisposed()) {
			color = new Color(display, rgb);
			createdColors.put(rgb, color);
		}
		return color;
	}

	public static Color fromHex(String hex) {
		if (hex == null)
			return white();
		String s = hex.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		if (s.length() < 6)
			return white();

		String rh = s.substring(0, 2);
		String gh = s.substring(2, 4);
		String bh = s.substring(4, 6);

		try {
			int r = Integer.parseInt(rh, 16);
			int g = Integer.parseInt(gh, 16);
			int b = Integer.parseInt(bh, 16);
			return get(r, g, b);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Colors.class);
			log.error("failed to parse hex color " + hex, e);
			return black();
		}
	}

	public static Color get(int r, int g, int b) {
		RGB rgb = new RGB(r, g, b);
		return get(rgb);
	}

	public static Color white() {
		return systemColor(SWT.COLOR_WHITE);
	}

	public static Color darkGray() {
		return systemColor(SWT.COLOR_DARK_GRAY);
	}

	public static Color linkBlue() {
		return get(25, 76, 127);
	}

	public static Color gray() {
		return get(128, 128, 128);
	}

	public static Color black() {
		return systemColor(SWT.COLOR_BLACK);
	}

	public static Color systemColor(int swtConstant) {
		return display.getSystemColor(swtConstant);
	}

	private static class ShutDown implements IWorkbenchListener {

		@Override
		public boolean preShutdown(IWorkbench workbench, boolean forced) {
			return true;
		}

		@Override
		public void postShutdown(IWorkbench workbench) {
			for (Color color : createdColors.values()) {
				if (!color.isDisposed())
					color.dispose();
			}
		}
	}
}
