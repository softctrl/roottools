# Easy access to common rooted tools for android developers 

clone of http://code.google.com/p/roottools/

--- 

I'm not intending to frequently update this or maintain it as a continual
mirror, if you're looking for the latest and greatest please see the 
[original source](http://code.google.com/p/roottools/)

Clone was taken using [svn2git](https://github.com/nirvdrum/svn2git) with a 
bunch of make/info/project file exclusions to strip it down to bare
bones:

#### create-clone.sh
	#!/bin/bash

    svn2git \
     "http://roottools.googlecode.com/svn/trunk/Developmental/RootTools_sdk3_generic/" \
     --rootistrunk \
     --exclude '.externalToolBuilders' \
     --verbose 
    
    git rm -r META-INF
    
    files_to_go=(
     ".classpath"
     ".project"
     "*.iml"
     "make*"
     "javadoc.xml"
     "Recommended_Merge_Method.cfr.txt"
    )
    
    for f in "${files_to_go[@]}"; do
	    echo git rm $f
	    git rm $f
    done

---

### Usage:

See [RootTools Wiki | Usage](http://code.google.com/p/roottools/wiki/Usage)
for details but in general:

many check methods are provided:

    if (RootTools.isBusyboxAvailable()) {
        // busybox exists, do something
    } else {
        // do something else
    }

and run-as-root methods:

    CommandCapture command = new CommandCapture(0, 
    	"echo this is a command", "echo this is another command"); 
    	
    RootTools.getShell(true).add(command).waitForFinish(); 