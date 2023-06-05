library helloworld

using FHIR version '1.8'

include FHIRHelpers version '1.8' called FHIRHelpers

codesystem PCO : 'http://ontology.mayo.edu/PCO#'
codesystem mock : 'http://foo.bar#'

code smokingStatus: 'cb84baf3-f45b-4ac2-bd4d-64eab2a4fbf0' from "PCO" display 'Current Smoking Status'

code smoker: 'smoker' from mock
concept smokerC: {smoker} display 'Mock smoker'

context Patient

define function CodeEqual( c1 Code, c2 Code ):
    Equal(c1.code,c2.code) and Equal(c1.system,c2.system)


define "Answer": "Is a smoker" or false

// cb84baf3-f45b-4ac2-bd4d-64eab2a4fbf0
define "CurrentSmokingStatus":
    [ Observation : code in smokingStatus ]

// "a8663b10-d01d-4f79-8c8e-42a864f409e1"
define "CurrentSmokerType":
    Last( "CurrentSmokingStatus" ).value

define "Is a smoker":
    Exists( "CurrentSmokingStatus" O
        where CodeEqual( FHIRHelpers.ToCode( O.value.coding.single() ), smoker ) )
