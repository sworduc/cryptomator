package org.cryptomator.ui.launcher;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.jni.JniException;
import org.cryptomator.jni.MacApplicationUiState;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Desktop;
import java.awt.SystemTray;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;
import java.awt.desktop.SystemEventListener;
import java.util.Optional;

@Singleton
public class UiLauncher {

	private static final Logger LOG = LoggerFactory.getLogger(UiLauncher.class);

	private final Settings settings;
	private final TrayMenuComponent.Builder trayComponent;
	private final FxApplicationStarter fxApplicationStarter;
	private final AppLaunchEventHandler launchEventHandler;
	private final Optional<MacFunctions> macFunctions;

	@Inject
	public UiLauncher(Settings settings, TrayMenuComponent.Builder trayComponent, FxApplicationStarter fxApplicationStarter, AppLaunchEventHandler launchEventHandler, Optional<MacFunctions> macFunctions) {
		this.settings = settings;
		this.trayComponent = trayComponent;
		this.fxApplicationStarter = fxApplicationStarter;
		this.launchEventHandler = launchEventHandler;
		this.macFunctions = macFunctions;
	}

	public void launch() {
		final boolean hasTrayIcon;
		if (SystemTray.isSupported()) {
			trayComponent.build().addIconToSystemTray();
			hasTrayIcon = true;
		} else {
			hasTrayIcon = false;
		}

		// show window on start?
		if (hasTrayIcon && settings.startHidden().get()) {
			LOG.debug("Hiding application...");
			macFunctions.map(MacFunctions::uiState).ifPresent(JniException.ignore(MacApplicationUiState::transformToAgentApplication));
		} else {
			showMainWindowAsync(hasTrayIcon);
		}

		// register app reopen listener
		Desktop.getDesktop().addAppEventListener((AppReopenedListener) e -> showMainWindowAsync(hasTrayIcon));

		launchEventHandler.startHandlingLaunchEvents(hasTrayIcon);
	}

	private void showMainWindowAsync(boolean hasTrayIcon) {
		fxApplicationStarter.get(hasTrayIcon).thenAccept(FxApplication::showMainWindow);
	}

}
