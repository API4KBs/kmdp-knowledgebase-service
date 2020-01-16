# kmdp-knowledgebase-service
##Release Instructions

Affected variables:
* project.parent.version
* project.version (SELF)

### Release Branch
1. Set root POM's version and parent.version to desired fixed version
  * The parent.version MUST match the ${kmdp.impl.version} variable in the BOM

### Nex Dev Branch
1. Set parent and project to the next desired version