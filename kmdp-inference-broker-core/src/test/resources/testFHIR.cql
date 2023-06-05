library helloworld

using FHIR version '1.8'

include FHIRHelpers version '1.8' called FHIRHelpers

context Patient

define "theObs":
    Last( [Observation] )

define "theVal":
    theObs.value