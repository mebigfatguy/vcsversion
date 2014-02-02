vcsversion
==========

An ant task to provide revision, date and branch properties of the project for use in a build.xml.
Currently supports Git, Hg, Svn and Bazaar.

Use as follows:

        <taskdef name="vcsversion" classname="com.mebigfatguy.vcsversion.VcsVersionTask" classpath="${lib.dir}/vcsversion.jar"/>
        <vcsversion vcs="git" revisionProperty="_rev_" dateProperty="_date_" branchProperty="_branch_"/>
        
        or you can use the namespace 
        
        xmlns:vcs="antlib:com.mebigfatguy.vcsversion
        
        to include the 'vcsversion' task automatically.
        
        
Then you can use your properties anywhere in the build xml, such as

        <manifest>
            <attribute name="Revision" value="${_rev_}"/>
            <attribute name="Date" value="${_date_}"/>
            <attribute name="Branch" value="${_branch_}"/>
        </manifest>
        
You do not need to supply all attributes, only the ones you want.

