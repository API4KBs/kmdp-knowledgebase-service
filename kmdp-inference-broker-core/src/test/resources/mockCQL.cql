library helloworld

using FHIR version '1.8'

include FHIRHelpers version '1.8' called FHIRHelpers

codesystem PCO : 'http://ontology.mayo.edu/PCO#'
codesystem mock : 'http://foo.bar#'

code smokingStatus: 'cb84baf3f45b4ac2bd4d64eab2a4fbf0' from "PCO" display 'Current Smoking Status'

code smoker: 'smoker' from mock

context Patient

define private "SmokingObs":
    [ Observation : code in smokingStatus ]


// cb84baf3-f45b-4ac2-bd4d-64eab2a4fbf0
define "currentSmokingStatus":
    Last( "SmokingObs" )

// a8663b10-d01d-4f79-8c8e-42a864f409e1
define "currentSmokerType":
    "currentSmokingStatus".value.coding.single()
