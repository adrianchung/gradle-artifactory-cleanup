Artifactory Cleanup
===================

Example of setting up a gradle plugin that cleans up artifactory artifacts that are older 
than 30 days. I needed this since the artifactory plugins require an upgrade to the pro 
version. 

This particular plugin was coded for my use case where I wanted to delete only leaf nodes
that contain all files and don't have the 'release' keyword. 

`gradle cleanArtifactory`
