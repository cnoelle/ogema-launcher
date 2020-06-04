package org.ogema.launcher.resolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.ogema.launcher.OgemaLauncher;
import static org.ogema.launcher.resolver.MavenResolver.DEF_RELEASE_POLICY;
import static org.ogema.launcher.resolver.MavenResolver.DEF_SNAPSHOT_POLICY;

/**
 *
 * @author jlapp
 */
class LauncherRepositoryProperties {

    private final String _repositoryConfig;
    private final Properties launcherRepositoryProperties = new Properties();
    
    /**
     * Boolean property: ignore global and user Maven settings.xml files.
     */
    public static final String IGNORE_SETTINGS = "ignoreSettings";

    LauncherRepositoryProperties(String propertiesFile) {
        this._repositoryConfig = propertiesFile;
        try {
            loadProperties();
        } catch (IOException ioex) {
            OgemaLauncher.LOGGER.log(Level.SEVERE, "could not read launcher repositories configuration", ioex);
        }
    }

    Properties getProperties() {
        return launcherRepositoryProperties;
    }
    
    public boolean isIgnoreSettings() {
        return Boolean.valueOf(getProperties().getProperty(IGNORE_SETTINGS));
    }

    private void loadProperties() throws IOException {
        if (new File(_repositoryConfig).exists()) {
            try (InputStream is = new FileInputStream(new File(_repositoryConfig))) {
                launcherRepositoryProperties.load(is);
            }
            OgemaLauncher.LOGGER.log(Level.FINE, String.format("configuring maven repositories from file %s", _repositoryConfig));
        } else {
            try (InputStream is = getClass().getResourceAsStream("/org/ogema/launcher/props/repositories.properties")) {
                launcherRepositoryProperties.load(is);
            }
            OgemaLauncher.LOGGER.log(Level.FINE, "configuring maven repositories from internal configuration file");
        }
    }

    /**
     * @param defaultFile file to return if no file is available from configuration
     * @return directory to use as local respository
     */
    File getLocalRepository(File defaultFile) {
        if (launcherRepositoryProperties.containsKey("localRepository")) {
            String localRepPath = launcherRepositoryProperties.getProperty("localRepository");
            File localRepo = new File(localRepPath);
            if (localRepo.exists()) {
                if (!localRepo.canWrite()) {
                    OgemaLauncher.LOGGER.log(Level.WARNING,
                            "location configured as local repository is not writable: {0}",
                            localRepo);
                } else {
                    if (!localRepo.isDirectory()) {
                        OgemaLauncher.LOGGER.log(Level.WARNING,
                                "location configured as local repository is not a directory: {0}",
                                localRepo);
                    } else {
                        OgemaLauncher.LOGGER.log(Level.FINE,
                                "local repository location set to {0}",
                                localRepo);
                        return localRepo;
                    }
                }
            } else {
                if (!localRepo.mkdirs()) {
                    OgemaLauncher.LOGGER.log(Level.WARNING,
                            "could not create location configured as local repository: {0}",
                            localRepo);
                } else {
                    OgemaLauncher.LOGGER.log(Level.FINE,
                            "local repository location set to {0}",
                            localRepo);
                    return localRepo;
                }
            }
        }
        return defaultFile;
    }

    void initDefaultRepositories(Map<String, RemoteRepository> prototypes) {
        try {
            Properties p = launcherRepositoryProperties;

            String[] ids = p.get("ids").toString().split(",\\s*");
            for (String id : ids) {
                String url = p.getProperty(id);

                RepositoryPolicy snapshotPolicy = DEF_SNAPSHOT_POLICY;
                String snapshotPolicyParams = (String) p.getProperty(id + ".snapshot-policy");
                if (snapshotPolicyParams != null) {
                    snapshotPolicy = parsePolicy(snapshotPolicyParams);
                }

                RepositoryPolicy releasePolicy = DEF_RELEASE_POLICY;
                String releasePolicyParams = (String) p.getProperty(id + ".release-policy");
                if (releasePolicyParams != null) {
                    releasePolicy = parsePolicy(releasePolicyParams);
                }
                String user = (String) p.getProperty(id + ".user");
                String password = (String) p.getProperty(id + ".password");
                Authentication auth = null;
                if (user != null && password != null) {
                    OgemaLauncher.LOGGER.log(Level.FINE, "adding authentication for {0}", url);
                    auth = new AuthenticationBuilder().addUsername(user).addPassword(password).build();
                }
                RemoteRepository repo = new RemoteRepository.Builder(id, "default", url)
                        .setReleasePolicy(releasePolicy).setSnapshotPolicy(snapshotPolicy)
                        .setAuthentication(auth).build();
                prototypes.put(url, repo);
            }

            if (OgemaLauncher.LOGGER.isLoggable(Level.FINE)) {
                OgemaLauncher.LOGGER.log(Level.FINE, String.format("%d remote repositories:", prototypes.size()));
                for (Map.Entry<String, RemoteRepository> entry : prototypes.entrySet()) {
                    OgemaLauncher.LOGGER.log(Level.FINE, entry.getValue().toString());
                }
            }
        } catch (Exception ex) {
            OgemaLauncher.LOGGER.log(Level.SEVERE,
                    String.format("error parsing repository configuration (%s?): %s", _repositoryConfig, ex.getMessage()), ex);
        }
    }

    private static RepositoryPolicy parsePolicy(String propertyValue) {
        String[] params = propertyValue.split(",\\s*");
        boolean enabled = params[0].equalsIgnoreCase("enabled");
        String updatePolicy = params[1];
        String checksumPolicy = params.length > 2 ? params[2] : null;
        return new RepositoryPolicy(enabled, updatePolicy, checksumPolicy);
    }

}
