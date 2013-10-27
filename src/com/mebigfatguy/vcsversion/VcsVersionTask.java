package com.mebigfatguy.vcsversion;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class VcsVersionTask extends Task {

    private enum VcsType {CVS, SVN, GIT, HG};
    
    private String vcs;
    private String revisionProp;
    private String branchProp;
    private String dateProp;
    
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
    
    
    public void execute() {
        if (vcs == null) {
            throw new BuildException("Failed to provide ant property 'vcs'");
        }
        
        VcsType type = VcsType.valueOf(vcs.toUpperCase());
        
        switch (type) {
            case CVS:
                getCVSInfo();
                break;
                
            case SVN:
                getSVNInfo();
                break;
                
            case GIT:
                getGITInfo();
                break;
                
            case HG:
                getHGInfo();
                break;
                
            default:
                throw new BuildException("Unknown vcs type: " + vcs);
        }        
    }

    private void getCVSInfo() {
 
    }
    
    private void getSVNInfo() {

    }
    
    private void getGITInfo() {

        Pattern gitCommitPattern = Pattern.compile("commit:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("date:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern branchPattern = Pattern.compile("\\*\\s*(.*)", Pattern.CASE_INSENSITIVE);

        BufferedReader br = null;
        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null)
                vcsProps.put(gitCommitPattern, revisionProp);
            if (dateProp != null)
                vcsProps.put(datePattern, dateProp);
            
            fetchInfo(vcsProps, "git", "-n", "1");
            
            vcsProps.clear();
            vcsProps.put(branchPattern, branchProp);
            
            fetchInfo(vcsProps, "git", "branch");
            
        } catch (Exception e) {
            throw new BuildException("Failed getting git log info", e);
        } finally {
            closeQuietly(br);
        }
    }

    private void getHGInfo() {

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
                        getProject().setProperty(entry.getValue(), m.group(1));
                    }
                }

                line = br.readLine();
            }
        } finally {
            closeQuietly(br);
        }
    }
    
    private void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) { 
        }
    }
}
