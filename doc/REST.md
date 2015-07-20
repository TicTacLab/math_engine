# REST API for MaltEngine

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC 2119](http://www.ietf.org/rfc/rfc2119.txt).

Any request may return error code 500 with body type text/plain if internal error occured.


## DELETE /session/:ssid
Invalidates user's session and deallocates models resources.

### Parameters
* ssid - required, session id

### Responses
* 200 if invalidation was successful
* 404 if session id is not valid
* 409 if engine is busy, session is locked and computation in progress

### Response format
text/plain

### Request example
        
    curl --request DELETE http://dc02malt01.dc02.favorit/session/754fbcde-662c-4b3f-9f51-acb4e67913da
    
### Response example
  
  if success, response body will be empty
  if error, body will be with text description of error
    
 
    
## GET /model/in-params
Return in params for specified model. ssid and id couple should be unique.

### Parameters
* id - required, ID of model to which request is applied
* ssid - required, client generated session id

### Responses
* 200 if in-params found
* 500 if model id not valid

### Response format
  "text/plain"

### Request example
  
    curl http://dc02malt01.dc02.favorit/model/in-params?id=120&ssid=754fbcde-662c-4b3f-9f51-acb4e67913d3

### Response example
    [
     {"code":"P_SCORE_A_PART_1","value":0.0,"name":"Score A Part 1","type":"Parameter","id":1},
     {"code":"P_SCORE_B_PART_1","value":0.0,"name":"Score B Part 1","type":"Parameter","id":2},
     {"code":"P_SCORE_A_PART_2","value":0.0,"name":"Score A Part 2","type":"Parameter","id":3},
     {"code":"P_SCORE_B_PART_2","value":0.0,"name":"Score B Part 2","type":"Parameter","id":4},
     {"code":"P_SCORE_A_PART_3","value":0.0,"name":"Score A Part 3","type":"Parameter","id":5},
     {"code":"P_SCORE_B_PART_3","value":0.0,"name":"Score B Part 3","type":"Parameter","id":6}
    ]
    


## POST /model/calc/:ssid


### Parameters
* ssid - required, client generated session id, should be persistent between requrests
* request body - json: 
```
{
  "id" : 120, // model id
  "ssid" : "f13813ca-ad53-4070-ac79-3c22f57a8822-120-889dfc38-5575-48ae-b45b-239f6cd4b2f9", // ssid
  "params" : [
    {
      "id" : "9",
      "value" : "150.0"
    }, {
      "id" : "3",
      "value" : "0.0"
    }
  ]
}
```

### Responses
* 200 calculation success or in progress

### Response format
For status = 200 "application/protobuf", otherwise "text/plain".
200 could have to separate messages, calculation in progress and successful result

### Request example

    curl -H "Content-Type: application/json" -X POST -d <<json-request-body>> http://dc02malt01.dc02.favorit/model/calc/754fbcde-662c-4b3f-9f51-acb4e67913da
    
### Response ok example

    {
      "type" : "outcomes",
      "data" : [ {
        "id" : 1,
        "market" : "Norm.Dist_300",
        "outcome" : "A",
        "coef" : 0.9259436130523682,
        "param" : 999999.0,
        "m_code" : "MATCH_NORM.DIST_300",
        "o_code" : "A",
        "param2" : 0.0,
        "timer" : 4046,
        "mgp_code" : "DISTRIBUTION",
        "mn_code" : "NORM_300",
        "mgp_weight" : 1,
        "mn_weight" : 11
      }, {
        "id" : 2,
        "market" : "Norm.Dist_300",
        "outcome" : "B",
        "coef" : 150.0,
        "param" : 999999.0,
        "m_code" : "MATCH_NORM.DIST_300",
        "o_code" : "B",
        "param2" : 0.0,
        "timer" : 0,
        "mgp_code" : "DISTRIBUTION",
        "mn_code" : "NORM_300",
        "mgp_weight" : 1,
        "mn_weight" : 11
      }]
    }

### Response calculation in progress example
    {
      "type" : "error",
      "error" : "Workbook: 120 calculation inprogress",
      "error_type" : "inprogress"
    }


## POST /model/calc/:ssid/binary
Same as /model/calc/:ssid but without timer fields in outcomes
