library TEE

using FHIR version '1.8'

codesystem sct : 'http://snomed.info/sct'
code tee: '266262004' from "sct"
concept teeC: {tee}

context Patient

//
define "HistoryOfArterialThromboembolism":
    FHIR.boolean { value : Exists( [ DiagnosticReport ] ) }