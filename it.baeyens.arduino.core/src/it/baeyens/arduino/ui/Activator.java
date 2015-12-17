package it.baeyens.arduino.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import cc.arduino.packages.discoverers.NetworkDiscovery;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.listeners.ConfigurationChangeListener;
import it.baeyens.arduino.managers.ArduinoManager;

/**
 * generated code
 * 
 * @author Jan Baeyens
 * 
 */
public class Activator implements BundleActivator {
    public static NetworkDiscovery bonjourDiscovery;
    public URL pluginStartInitiator = null; // Initiator to start the plugin
    public Object mstatus; // status of the plugin
    protected String flagStart = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s";
    protected char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e', 'c', 'l', 'i', 'p', 's',
	    'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'p', 'l', 'u', 'g', 'i', 'n', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l',
	    '?', 's', '=' };

    @Override
    public void start(BundleContext context) throws Exception {
	// Make sure some important variables are being initialized
	String LibPaths[] = ArduinoInstancePreferences.getPrivateLibraryPaths();
	ArduinoInstancePreferences.setPrivateLibraryPaths(LibPaths);

	LibPaths = ArduinoInstancePreferences.getPrivateHardwarePaths();
	ArduinoInstancePreferences.setPrivateHardwarePaths(LibPaths);

	Job job = new Job("pluginCoreStartInitiator") {
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(Activator.this.flagStart, 0) + 1;
		    myScope.putInt(Activator.this.flagStart, curFsiStatus);
		    Activator.this.pluginStartInitiator = new URL(new String(Activator.this.uri) + Integer.toString(curFsiStatus));
		    Activator.this.mstatus = Activator.this.pluginStartInitiator.getContent();
		} catch (Exception e) {
		    // if this happens there is no real harm or functionality
		    // lost
		}
		CoreModel singCoreModel = CoreModel.getDefault();
		singCoreModel.addCProjectDescriptionListener(new ConfigurationChangeListener(), CProjectDescriptionEvent.ABOUT_TO_APPLY);
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule();

	Job installJob = new Job("Arduino installer job") {

	    @SuppressWarnings("synthetic-access")
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		makeOurOwnCustomBoards_txt();
		ArduinoManager.startup_Pluging();
		bonjourDiscovery = new NetworkDiscovery();
		bonjourDiscovery.start();
		return Status.OK_STATUS;

	    }

	};
	installJob.setPriority(Job.BUILD);
	installJob.schedule();

	return;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
     */
    @Override
    public void stop(BundleContext context) throws Exception {
	// plugin = null;
	// super.stop(context);
    }

    public static final String PLUGIN_ID = "it.baeyens.arduino.core"; //$NON-NLS-1$

    public static void log(Exception e) {
	// TODO Auto-generated method stub

    }

    public static String getId() {
	// TODO Auto-generated method stub
	return PLUGIN_ID;
    }

    /**
     * To be capable of overwriting the boards.txt and platform.txt file settings the plugin contains its own settings. The settings are arduino IDE
     * version specific and it seems to be relatively difficult to read a boards.txt located in the plugin itself (so outside of the workspace)
     * Therefore I copy the file during plugin configuration to the workspace root. The file is arduino IDE specific. If no specific file is found the
     * default is used. There are actually 4 txt files. 2 are for pre-processing 2 are for post processing. each time 1 board.txt an platform.txt I
     * probably do not need all of them but as I'm setting up this framework it seems best to add all possible combinations.
     * 
     */
    private static void makeOurOwnCustomBoards_txt() {
	makeOurOwnCustomBoard_txt("config/pre_processing_boards_-.txt", ConfigurationPreferences.getPreProcessingBoardsFile(), false); //$NON-NLS-1$
	makeOurOwnCustomBoard_txt("config/post_processing_boards_-.txt", ConfigurationPreferences.getPostProcessingBoardsFile(), false); //$NON-NLS-1$
	makeOurOwnCustomBoard_txt("config/pre_processing_platform_-.txt", ConfigurationPreferences.getPreProcessingPlatformFile(), false); //$NON-NLS-1$
	makeOurOwnCustomBoard_txt("config/post_processing_platform_-.txt", ConfigurationPreferences.getPostProcessingPlatformFile(), false); //$NON-NLS-1$
    }

    /**
     * This method creates a file in the root of the workspace based on a file delivered with the plugin The file can be arduino IDE version specific.
     * If no specific version is found the default is used. Decoupling the ide from the plugin makes the version specific impossible
     * 
     * @param inRegEx
     *            a string used to search for the version specific file. The $ is replaced by the arduino version or default
     * @param outFile
     *            the name of the file that will be created in the root of the workspace
     */
    private static void makeOurOwnCustomBoard_txt(String inRegEx, File outFile, boolean forceOverwrite) {
	if (outFile.exists() && !forceOverwrite) {
	    return;
	}
	// String VersionSpecificFile = inRegEx.replaceFirst("-", mArduinoIdeVersion.getStringValue());
	String DefaultFile = inRegEx.replaceFirst("-", "default"); //$NON-NLS-1$ //$NON-NLS-2$
	/*
	 * Finding the file in the plugin as described here :http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
	 */

	byte[] buffer = new byte[4096]; // To hold file contents
	int bytes_read; // How many bytes in buffer

	try (FileOutputStream to = new FileOutputStream(outFile.toString());) {
	    try {
		// URL specificUrl = new URL("platform:/plugin/it.baeyens.arduino.core/" + VersionSpecificFile);
		URL defaultUrl = new URL("platform:/plugin/it.baeyens.arduino.core/" + DefaultFile); //$NON-NLS-1$

		// try (InputStream inputStreamSpecific = specificUrl.openConnection().getInputStream();) {
		// while ((bytes_read = inputStreamSpecific.read(buffer)) != -1) {
		// to.write(buffer, 0, bytes_read); // write
		// }
		// } catch (IOException e) {
		try (InputStream inputStreamDefault = defaultUrl.openConnection().getInputStream();) {
		    while ((bytes_read = inputStreamDefault.read(buffer)) != -1) {
			to.write(buffer, 0, bytes_read); // write
		    }
		} catch (IOException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		    return;
		}
		// return;
		// }
	    } catch (MalformedURLException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	} catch (IOException e2) {
	    // TODO Auto-generated catch block
	    e2.printStackTrace();
	} // Create output stream

    }

}