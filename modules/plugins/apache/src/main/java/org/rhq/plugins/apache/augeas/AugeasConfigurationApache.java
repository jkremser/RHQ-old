/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.apache.augeas;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.util.Glob;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.PluginDescriptorBasedAugeasConfiguration;

/**
 * Represents the configuration of Augeas needed to parse Apache.
 * In this class, we scan the supplied main configuration file for include directives
 * (recursively) and thus make up the full list of files the concrete Apache configuration
 * is comprised of.
 * <p>
 * Also, in case the serverRoot is overriden in the configuration files from the default
 * supplied in the configuration object in the constructor, it is updated accordingly.
 * 
 * @author Filip Drabek
 */
public class AugeasConfigurationApache extends PluginDescriptorBasedAugeasConfiguration {

    public static final String INCLUDE_DIRECTIVE = "Include";
    private static final String INCLUDE_FILES_PATTERN = "^[\t ]*Include[\t ]+(.*)$";
    private static final String SERVER_ROOT_PATTERN = "^[\t ]*ServerRoot[\t ]+[\"]?([^\"\n]*)[\"]?$";

    private final Pattern includePattern = Pattern.compile(INCLUDE_FILES_PATTERN);
    private final Pattern serverRootPattern = Pattern.compile(SERVER_ROOT_PATTERN);

    private String serverRootPath;
    private AugeasModuleConfig module;
    private List<File> allConfigFiles;

    public String getServerRootPath() {
        return serverRootPath;
    }

    public AugeasConfigurationApache(Configuration configuration) throws AugeasRhqException {
        super(configuration);

        serverRootPath = configuration.getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT, null);

        if (modules.isEmpty())
            throw new AugeasRhqException("There is not configuration for this resource.");
        try {
            module = modules.get(0);

            List<String> foundIncludes = new ArrayList<String>();
            loadIncludes(module.getIncludedGlobs().get(0), foundIncludes);

            allConfigFiles = getIncludeFiles(serverRootPath, foundIncludes);
        } catch (Exception e) {
            throw new AugeasRhqException(e.getMessage());
        }
    }

    public String getAugeasModuleName() {
        return module.getModuletName();
    }

    public List<String> getIncludes(File file) {
        List<String> includeFiles = new ArrayList<String>();

        return includeFiles;
    }

    public List<File> getAllConfigurationFiles() {
        return allConfigFiles;
    }

    private void loadIncludes(String expression, List<String> foundIncludes) {
        foundIncludes.add(expression);
        try {
            File file = new File(expression);

            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    Matcher m = includePattern.matcher(strLine);
                    if (m.matches()) {
                        String glob = m.group(1);

                        module.addIncludedGlob(glob);
                        loadIncludes(glob, foundIncludes);
                    }
                    Matcher serverRootMatcher = serverRootPattern.matcher(strLine);
                    if (serverRootMatcher.matches()) {
                        serverRootPath = serverRootMatcher.group(1);
                    }
                }
                br.close();
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void loadFiles() {
        File root = new File(serverRootPath);

        for (AugeasModuleConfig module : modules) {
            List<String> includeGlobs = module.getIncludedGlobs();

            if (includeGlobs.size() <= 0) {
                throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
            }

            ArrayList<File> files = new ArrayList<File>();

            for (String incl : includeGlobs) {
                if (incl.indexOf(File.separatorChar) == 0) {
                    files.add(new File(incl));
                } else
                    files.addAll(Glob.match(root, incl));
            }

            if (module.getExcludedGlobs() != null) {
                List<String> excludeGlobs = module.getExcludedGlobs();
                Glob.excludeAll(files, excludeGlobs);
            }

            for (File configFile : files) {
                if (!configFile.isAbsolute()) {
                    throw new IllegalStateException(
                        "Configuration files inclusion patterns contain a non-absolute file.");
                }
                if (!configFile.exists()) {
                    throw new IllegalStateException(
                        "Configuration files inclusion patterns refer to a non-existent file.");
                }
                if (configFile.isDirectory()) {
                    throw new IllegalStateException("Configuration files inclusion patterns refer to a directory.");
                }
                if (!module.getConfigFiles().contains(configFile.getAbsolutePath()))
                    module.addConfigFile(configFile.getAbsolutePath());
            }
        }
    }

    private static List<File> getIncludeFiles(String serverRoot, List<String> foundIncludes) {
        List<File> ret = new ArrayList<File>();
        File serverRootFile = new File(serverRoot);
        for (String path : foundIncludes) {
            File check = new File(path);
            if (check.isAbsolute()) {
                ret.add(check);
            } else {
                for (File f : Glob.match(serverRootFile, path)) {
                    ret.add(f);
                }
            }
        }

        return ret;
    }
}
