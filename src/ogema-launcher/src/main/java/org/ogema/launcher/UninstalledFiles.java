package org.ogema.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;

/**
 *
 * @author jlapp
 */
public class UninstalledFiles {

    private final static String FILEPATH = "config/uninstalled";

    public BundleListener getListener() {
        return l;
    }

    private final BundleListener l = new BundleListener() {
        @Override
        public void bundleChanged(BundleEvent event) {
            if (event.getType() == BundleEvent.UNINSTALLED) {
                try {
                    addUninstalled(event.getBundle());
                } catch (IOException ex) {
                    OgemaLauncher.LOGGER.severe(
                            String.format("could not update uninstalled bundle list: %s", ex.getMessage()));
                }
            }
        }
    };

    private static synchronized void addUninstalled(Bundle b) throws IOException {
        try ( FileWriter out = new FileWriter(FILEPATH, true)) {
            String bsnversion = b.getSymbolicName() + ":" + b.getVersion();
            out.append(bsnversion).append("\n");
        }
    }

    public static void removeUninstalled(Map<String, List<BundleInfo>> bundlesToInstall) {
        File f = new File(FILEPATH);
        if (!f.canRead()) {
            return;
        }
        try ( FileReader fin = new FileReader(f);  BufferedReader in = new BufferedReader(fin)) {
            String line;
            List<BundleInfo> uninstalled = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                String[] a = line.split(":", 2);
                uninstalled.add(new BundleInfo(a[0], Version.parseVersion(a[1])));
            }
            for (List<BundleInfo> bis : bundlesToInstall.values()) {
                Iterator<BundleInfo> it = bis.iterator();
                while (it.hasNext()) {
                    BundleInfo bi = it.next();
                    for (BundleInfo u : uninstalled) {
                        if (u.getSymbolicName().equals(bi.getSymbolicName())
                                && u.getVersion().equals(bi.getVersion())) {
                            OgemaLauncher.LOGGER.fine(
                                    String.format("not installing previously removed bundle %s:%s",
                                            bi.getSymbolicName(), bi.getVersion()));
                            it.remove();
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            OgemaLauncher.LOGGER.severe(
                    String.format("error while processing uninstalled bundle info: %s",
                            ex));
        }
    }

}
