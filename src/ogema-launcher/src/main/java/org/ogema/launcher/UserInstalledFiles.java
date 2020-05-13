package org.ogema.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 *
 * @author jlapp
 */
public class UserInstalledFiles {

    private final static String FILEPATH = "config/userbundles";

    public BundleListener getListener() {
        return l;
    }

    private final BundleListener l = new BundleListener() {
        @Override
        public void bundleChanged(BundleEvent event) {
            if (event.getType() == BundleEvent.INSTALLED) {
                try {
                    addInstalled(event.getBundle());
                } catch (IOException ex) {
                    OgemaLauncher.LOGGER.severe(
                            String.format("could not update manually installed bundles list: %s", ex.getMessage()));
                }
            }
        }
    };

    private static synchronized void addInstalled(Bundle b) throws IOException {
        try (FileWriter out = new FileWriter(FILEPATH, true)) {
            String bundleInfo = String.format("%d:%s:%s%n",
                    b.getBundleId(), b.getSymbolicName(), b.getVersion());
            out.append(bundleInfo);
        }
    }

    public static List<Bundle> removeUserInstalled(List<Bundle> bundlesToUninstall) {
        File f = new File(FILEPATH);
        bundlesToUninstall = new ArrayList<>(bundlesToUninstall);
        if (!f.canRead()) {
            return bundlesToUninstall;
        }
        try (FileReader fin = new FileReader(f); BufferedReader in = new BufferedReader(fin)) {
            String line;
            HashSet<Long> userBundleIds = new HashSet<>();
            while ((line = in.readLine()) != null) {
                String[] a = line.split(":", 2);
                userBundleIds.add(Long.parseLong(a[0]));
            }
            Iterator<Bundle> it = bundlesToUninstall.iterator();
            while (it.hasNext()) {
                Bundle b = it.next();
                if (userBundleIds.contains(b.getBundleId())) {
                    OgemaLauncher.LOGGER.fine(
                            String.format("will not remove manually installed bundle [%d] %s:%s",
                                    b.getBundleId(), b.getSymbolicName(), b.getVersion()));
                    it.remove();
                }
            }
        } catch (IOException | RuntimeException ex) {
            OgemaLauncher.LOGGER.log(Level.SEVERE,
                    "error while processing uninstalled bundle info", ex);
        }
        return bundlesToUninstall;
    }

}
