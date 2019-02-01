/*
 * VcsVersion - an ant task for fetching revision, branch and dates of current view
 * Copyright 2013-2019 MeBigFatGuy.com
 * Copyright 2013-2019 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.vcsversion;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class VcsVersionTask extends Task {

    private enum VcsType {
        SVN("svn", "subversion"), GIT("git"), HG("hg", "mercurial"), BAZAAR("bzr", "bazaar"), BITKEEPER("bk", "bitkeeper");

        private final List<String> aliases;

        VcsType(String... vcsAlias) {
            aliases = Arrays.asList(vcsAlias);
        }

        public static VcsType decode(String vcsAlias) {
            vcsAlias = vcsAlias.toLowerCase();
            for (VcsType vcs : VcsType.values()) {
                if (vcs.aliases.indexOf(vcsAlias) >= 0) {
                    return vcs;
                }
            }

            return null;
        }
    };

    private String vcs;
    private String revisionProp;
    private String branchProp;
    private String dateProp;
    private String urlProp;

    public void setVcs(String versionControlSystem) {
        vcs = versionControlSystem;
    }

    public void setRevisionProperty(String revisionProperty) {
        revisionProp = revisionProperty;
    }

    public void setBranchProperty(String branchProperty) {
        branchProp = branchProperty;
    }

    public void setDateProperty(String dateProperty) {
        dateProp = dateProperty;
    }

    public void setUrlProperty(String urlProperty) {
        urlProp = urlProperty;
    }

    @Override
    public void execute() {
        if (vcs == null) {
            throw new BuildException("Failed to provide ant property 'vcs'");
        }

        VcsType type = VcsType.decode(vcs);

        switch (type) {

        case SVN:
            getSVNInfo();
            break;

        case GIT:
            getGITInfo();
            break;

        case HG:
            getHGInfo();
            break;

        case BAZAAR:
            getBazaarInfo();
            break;

        case BITKEEPER:
            getBitKeeperInfo();
            break;

        default:
            throw new BuildException("Unknown vcs type: " + vcs);
        }
    }

    private void getSVNInfo() {

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null) {
                Pattern commitPattern = Pattern.compile("([^\\s]*)\\s+\\|.*", Pattern.CASE_INSENSITIVE);
                vcsProps.put(commitPattern, revisionProp);
            }
            if (dateProp != null) {
                Pattern datePattern = Pattern.compile("[^\\|]+\\|[^\\|]+\\|\\s*([\\|]*).*", Pattern.CASE_INSENSITIVE);
                vcsProps.put(datePattern, dateProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "svn", "log", "-l", "1");
            }

            vcsProps.clear();
            if (branchProp != null) {
                Pattern branchPattern = Pattern.compile("url:?.*/(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(branchPattern, branchProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "svn", "info");
            }

            vcsProps.clear();
            if (urlProp != null) {
                Pattern urlPattern = Pattern.compile("URL:\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(urlPattern, urlProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "svn", "info", ".");
            }

        } catch (Exception e) {
            throw new BuildException("Failed getting svn log info", e);
        }
    }

    private void getGITInfo() {

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null) {
                Pattern commitPattern = Pattern.compile("commit:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(commitPattern, revisionProp);
            }
            if (dateProp != null) {
                Pattern datePattern = Pattern.compile("date:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(datePattern, dateProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "git", "log", "-n", "1");
            }

            vcsProps.clear();
            if (branchProp != null) {
                Pattern branchPattern = Pattern.compile("\\*\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(branchPattern, branchProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "git", "branch");
            }

            vcsProps.clear();
            if (urlProp != null) {
                Pattern urlPattern = Pattern.compile("(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(urlPattern, urlProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "git", "config", "--get", "remote.origin.url");
            }

        } catch (Exception e) {
            throw new BuildException("Failed getting git log info", e);
        }
    }

    private void getHGInfo() {

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null) {
                Pattern changesetPattern = Pattern.compile("changeset:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(changesetPattern, revisionProp);
            }
            if (dateProp != null) {
                Pattern datePattern = Pattern.compile("date:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(datePattern, dateProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "hg", "log", "-l", "1");
            }

            vcsProps.clear();
            if (branchProp != null) {
                Pattern branchPattern = Pattern.compile("(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(branchPattern, branchProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "hg", "branch");
            }

            vcsProps.clear();
            if (urlProp != null) {
                Pattern urlPattern = Pattern.compile("(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(urlPattern, urlProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "hg", "paths", "default");
            }

        } catch (Exception e) {
            throw new BuildException("Failed getting hg log info", e);
        }
    }

    private void getBazaarInfo() {

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null) {
                Pattern revnoPattern = Pattern.compile("revno:\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(revnoPattern, revisionProp);
            }
            if (dateProp != null) {
                Pattern timestampPattern = Pattern.compile("timestamp:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(timestampPattern, dateProp);
            }
            if (branchProp != null) {
                Pattern branchPattern = Pattern.compile("branch nick:\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(branchPattern, branchProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "bzr", "log", "-r-1");
            }

            vcsProps.clear();
            if (urlProp != null) {
                Pattern urlPattern = Pattern.compile("\\s*parent branch:\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(urlPattern, urlProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "bzr", "info", "-v");
            }

        } catch (Exception e) {
            throw new BuildException("Failed getting hg log info", e);
        }
    }

    private void getBitKeeperInfo() {

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null) {
                Pattern revnoPattern = Pattern.compile(".*\\|ChangeSet\\|.*\\|(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(revnoPattern, revisionProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "bk", "changes", "-1", "-nd:KEY:");
            }

            vcsProps.clear();
            if (dateProp != null) {
                Pattern timestampPattern = Pattern.compile("ChangeSet@[^,]*,([^,]*),.*", Pattern.CASE_INSENSITIVE);
                vcsProps.put(timestampPattern, dateProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "bk", "changes", "-1");
            }

            if (branchProp != null) {
                getProject().setProperty(branchProp, "");
            }

            vcsProps.clear();
            if (urlProp != null) {
                Pattern urlPattern = Pattern.compile("\\s*Push/pull parent:\\s*(.*)", Pattern.CASE_INSENSITIVE);
                vcsProps.put(urlPattern, urlProp);
            }

            if (!vcsProps.isEmpty()) {
                fetchInfo(vcsProps, "bk", "parent");
            }

        } catch (Exception e) {
            throw new BuildException("Failed getting hg log info", e);
        }
    }

    private void fetchInfo(Map<Pattern, String> vcsProps, String... commandLine) throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder(commandLine);
        builder.directory(getProject().getBaseDir());
        Process p = builder.start();
        p.waitFor();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String line = br.readLine();
            while (line != null) {
                for (Map.Entry<Pattern, String> entry : vcsProps.entrySet()) {
                    Matcher m = entry.getKey().matcher(line);
                    if (m.matches()) {
                        getProject().setProperty(entry.getValue(), m.group(1).trim());
                    }
                }

                line = br.readLine();
            }
        } finally {
            closeQuietly(br);
        }
    }

    private static void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
        }
    }
}
