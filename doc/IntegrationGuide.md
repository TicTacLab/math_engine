# Math Engine Integration Guide

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in RFC 2119.

## Introduction

The purpose of Math Engine (ME) is calculating of math models written in MS Excel format. Guidelines for Excel file preparation are described in the "Guidelines.xls" file. Math Engine interacts with excel math models through IN/OUT-sheets by passing IN-params and receiving OUT-values.

## Definitions

*IN-params* – parameters that are passed from client to model through IN-sheet on model excel file.

*OUT-values* – result of math model evaluation with applied IN-params

*IN-sheet* – sheet in model excel file used to accept input parameters from Math Engine. Must contain id and value columns. Also may contain any number of additional extra columns.

*OUT-sheet* – sheet in model excel file used to receive calculation result from model in Math Engine.

This sheet directly serialized to JSON.

*Examples*

There is examples in the description of every REST method. This example uses curl command to make requests to the server. If you are running Linux or OSX then this command must be preinstalled on your system, otherwise use your package manager to install it. If you are using Windows you can install it from http://curl.haxx.se/download.html and run it in the command line.

## Math Engine usage

ME usage consists of several steps:

1. Creating event instance by unique event-id.
2. Filling IN-params and getting model's OUT-values. This step repeats until the end of event.
3. Destroying event.

Let’s talk about each step in details.

### Creating event by unique event-id

For calculation speed up ME allocates computational resources for every event. Every event in MEuses two identificators: event-id and model-id.

Resources are allocated "lazily", after first call of method CALCULATE or IN-PARAMS. So you do not need explicitly create any event. Because of this, first request is processed longer than subsequent.

Also resources will be allocated again if method CALCULATE or IN-PARAMS called after RELEASE with the same model-id and event-id.

### Filling IN-params and getting model's OUT-values

For filling IN-sheet and getting evaluated OUT-values you should use method CALCULATE. This method takes IN-params and returns OUT-params. Important IN-sheet should have at least two columns: id and value. IN-sheet can also contain any number of additional columns. API allows to get IN-sheet data as JSON using IN-PARAMS method. Requests to single event are executed sequentially, ME will lock event until request returns. Calling methods on locked event will result in 423 "Calculation in progress" error. Destroying event When interaction with ME on particular event is ended, client should release computation resources by calling method RELEASE. Also resources will be released automatically after some period of time which is set by ME administrator, usually 15 minutes.

## REST API

### General

Any request may return 500 errors if internal error occurred. All responses will have Content-Type: "application/json". Response 204 has no body as specified by HTTP protocol.

*Response structure*

```
{
"status": 200,
"data": {} // object or array of objects
}
Error structure
{
"status": 423,
"errors": [
{
"code": "CIP",
"message" "Calculation in progress"
}]
}
```

### Method: IN-PARAMS
### GET /api/models/:model-id/:event-id/in-params

Returns IN-params for specified model. event-id and model-id couple should be unique.

*Parameters*

* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

*Responses*

* 200 if IN-params found
* 404 "MNF" if model not found

*Request example*

```curl http://math.engine/api/models/120/72c361803a5b116e0682581fe958fdee/in-params```

```
Response 200 data example
{
"status":200,
"data":[
{
"code":"P_SCORE_A_PART_1",
"value":0.0,
"name":"Score A Part 1",
"type":"Parameter",
"id":1
},
{
"code":"P_SCORE_B_PART_1",
"value":0.0,
"name":"Score B Part 1",
"type":"Parameter",
"id":2
},
{
"code":"P_SCORE_A_PART_2",
"value":0.0,
"name":"Score A Part 2",
"type":"Parameter",
"id":3
},
{
"code":"P_SCORE_B_PART_2","value":0.0,
"name":"Score B Part 2",
"type":"Parameter",
"id":4
},
{
"code":"P_SCORE_A_PART_3",
"value":0.0,
"name":"Score A Part 3",
"type":"Parameter",
"id":5
},
{
"code":"P_SCORE_B_PART_3",
"value":0.0,
"name":"Score B Part 3",
"type":"Parameter",
"id":6
}
]
}
```

### Method: CALCULATE
### POST /api/models/:model-id/:event-id/calculate

Set IN-sheet using IN-parms sent in the body of this request, evaluates OUT-sheet, and returns OUT-values in the body of response.
**!Important IN-params should have only strings in the values of parameters.**

*Parameters*

* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

*Request body*

```
{
"params" : [
{
"id" : "9", // string!
"value" : "150.0" // string!
}, {
"id" : "3",
"value" : "0.0"
}
]
}
[
{
"id" : "9", // string"value" : "150.0" // string
},
{
"id" : "3",
"value" : "0.0"
}
]

```
request body is a json object and should contain ALL models in-parameters for correct computation.

*Responses*

* 200 if calculation success
* 400 "MFP" if parameters are invalid
* 404 "MNF" if model not found
* 423 "CIP" if engine is busy, event is locked and computation in progress

### Cell errors

Any field of outcome in response may contain error string message if error occured while calculation. Per cell errors are not considered as response error, only as per cell fail, so response  with cell errors will be 200 OK.

#### Possible cell errors:

"#NULL!", "#DIV/0!", "#VALUE!", "#REF!", "#NAME?", "#NUM!", "#N/A".

#### Cell type
Outcome's fields will be of type as specified in excel file.

*Request example*

```curl -X POST -d <<json-request-body>> http://math.engine/api/models/120/72c361803a5b116e0682581fe958fdee/calculate```

*Response 200 data example*

```
{
"status": 200,
"data" : [
{
"id" : 1,
"market" : "Norm.Dist_300",
"outcome" : "A",
"coef" : 0.9259436130523682,
"param" : 999999.0,
"m_code" : "MATCH_NORM.DIST_300",
"o_code" : "A",
"param2" : 0.0,"mgp_code" : "DISTRIBUTION",
"mn_code" : "NORM_300",
"mgp_weight" : 1,
"mn_weight" : 11
}, {
"id" : 2,
"market" : "Norm.Dist_300",
"outcome" : "B",
"coef" : "#VALUE!",
"param" : 999999.0,
"m_code" : "MATCH_NORM.DIST_300",
"o_code" : "B",
"param2" : 0.0,
"mgp_code" : "DISTRIBUTION",
"mn_code" : "NORM_300",
"mgp_weight" : 1,
"mn_weight" : 11
}
]
}
```


### Method: RELEASE
### DELETE /api/models/:model-id/:event-id

Deallocates models resources for selected event.

*Parameters*

* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

*Responses*

* 204 if deallocation was successful
* 404 "ENF" if event id is not found
* 423 "CIP" if engine is busy, event is locked and computation in progress

*Request example*

```curl --request DELETE http://math.engine/api/models/120/754fbcde-662c-4b3f-9f51-acb4e67913da```
