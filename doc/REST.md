# Math Engine

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC 2119](http://www.ietf.org/rfc/rfc2119.txt).

# Introduction

The purpose of Math Engine (ME) is calculating of math models written in MS Excel format.
Guidelines for Excel file preparation are described in the "Guidelines.xls" file.
Math Engine interacts with excel math models through IN/OUT sheets by passing IN-params and receiving OUT-values. 

# Definitions
IN-params
: parameters that are passed from client to model through IN sheet on model excel file

OUT-values
: result of math model evaluation with applied IN-params

IN sheet
: sheet in model excel file used to accept input params from Math Engine. Must contain id and value columns. Also may contain any number of additional extra columns

OUT sheet
: sheet in model excel file used to receive calculation result from model in Math Engine. This sheet directly serialized to JSON


# Math Engine usage

ME usage consists of several steps: (Порядок работы с ME состоит из следующих шагов:)
1. Creating event by unique event-id.
2. Filling IN-params and getting model's OUT-values.
3. Destroying event.

### Creating event by unique event-id
For calculation speed up ME allocates computation resources for every sport event. 
For unique identification of event ME needs unique couple of event-id and model-id. 

(Для ускорения расчета ME выделяет вычислительные ресурсы под каждое событие. Что бы уникально иденитфицировать событие ME необходим уникатльный event-id. Пара event-id + model-id однозначно идентифицирует собитие внитри ME.)

Resources are allocated "lazily", after first call of method CALCULATE or IN-PARAMS. 
Because of this first request is processed longer then subsequent. 
Also resources will be allocated again if method CALCULATE or IN-PARAMS called after RELEASE with the same model-id and event-id. 

(Ресурсы выделяются "лениво", при превом выполнении методов CALCULATE/IN-PARAMS, поэтому результат первого запроса вычисляется дольше чем последующие. Также ресурсы будут выделены повторно если перед методами CALCULATE/IN-PARAMS был вызван метод RELEASE.)

### Filling IN-params and getting model's OUT-values
IN-params in Excel model file should be represented as required pairs of id + value on the IN sheet. 
IN sheet can also contain any number of optional columns.
API allows to get IN sheet data as JSON using IN-PARAMS method. 

(IN-params в модели должны быть предствленны в виде обязательных пар id + value на странице IN. IN sheet может содержать любое кол-во опциональных колонок. API позволяет получить предствление IN sheet в виде JSON с помощью метода IN-PARAMS.)

OUT-values are JSON representation of OUT sheet which is evaluated using IN-params.
CALCULATE method returns OUT-values as JSON by applying passed IN-params. 

(OUT-values это сериализированныя в JSON страница OUT рассчитанная по заданым IN-params. Что бы получить OUT-values нужно выполнить метод CALCULATE с заданными IN-params.)

Requests to single event are executed sequentially, ME will lock event until request returns. 
Calling methods on locked event will result in 423 "Calculation in progress" error. 

(Что бы сделать повторный запрос на расчет нужно дождаться завершения предыдущего для данного события, иначе ME вернет ошибку 423 "calculation in progress".)

### Destroying event
When interaction with ME on particular event is ended, client should release computation resources by calling method RELEASE. 
Also resources will be released automatically after some period of time, usually 15 minutes.

(После окончания события необходимо освободить ресурсы ME с помощью метода RELEASE. Ресурсы осовбождаются автоматически если к заданному событию не было запросов через интервал <<NNN>>)


# REST API

Any request may return 500 errors if internal error occurred.
All responses will have Content-Type: "application/json". 
Response 204 has no body as specified by HTTP protocol.

## Response structure
```
{
  "status": 200,
  "data": {} // or array of objects
}
```

## Error structure
```
{
  "status": 423,
  "errors": [
    {
      "code": "CIP",
      "message" "Calculation in progress"
    }
  ]
}
```


## Method: IN-PARAMS    
### GET /api/models/:model-id/:event-id/in-params
Return in params for specified model. event-id and model-id couple should be unique.

### Params
* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

### Responses
* 200  if in-params found
* 404 "MNF" if model not found

### Request example

    curl http://math.engine/api/models/120/72c361803a5b116e0682581fe958fdee/in-params

### Response 200 data example
```
[
  {"code":"P_SCORE_A_PART_1","value":0.0,"name":"Score A Part 1","type":"Parameter","id":1},
  {"code":"P_SCORE_B_PART_1","value":0.0,"name":"Score B Part 1","type":"Parameter","id":2},
  {"code":"P_SCORE_A_PART_2","value":0.0,"name":"Score A Part 2","type":"Parameter","id":3},
  {"code":"P_SCORE_B_PART_2","value":0.0,"name":"Score B Part 2","type":"Parameter","id":4},
  {"code":"P_SCORE_A_PART_3","value":0.0,"name":"Score A Part 3","type":"Parameter","id":5},
  {"code":"P_SCORE_B_PART_3","value":0.0,"name":"Score B Part 3","type":"Parameter","id":6}
]
```
  
    


## Method: CALCULATE
### POST /api/models/:model-id/:event-id/calculate

### Params
* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

### Request body
```
{
  "model_id" : 120, // int  --- есть идея выпилить нафиг, уже есть в URL
  "event_id" : "72c361803a5b116e0682581fe958fdee",  // string --- аналогично
  "params" : [ --- и сделать все тело запроса только этим массивом папаметров
    {
      "id" : "9", // string
      "value" : "150.0" // string
    }, {
      "id" : "3",
      "value" : "0.0"
    }
  ]
}
```
--- есть идея выпилить model_id и event_id поскольку они уже есть в URL, сделать тело просто массивом
```
[
  {
    "id" : "9", // string
    "value" : "150.0" // string
  }, 
  {
    "id" : "3",
    "value" : "0.0"
  }
]
```
request body is a json object and should contain ALL models in-parameters for correct computation.

### Responses
* 200 if calculation success
* 400 "MFP" if params are invalid
* 404 "MNF" if model not found
* 423 "CIP" if engine is busy, event is locked and computation in progress

### Cell errors
Any field of outcome in response may contain error string message if error occured while calculation. 
Per cell errors are not considered as reponse error, only as per cell fail, so response with cell errors will be 200 OK.
Possible cell errors: 
```
"#NULL!", "#DIV/0!", "#VALUE!", "#REF!", "#NAME?", "#NUM!", "#N/A". 
```

### Cell type
Outcome's fields will be of type as specified in excel file.

### Request example

    curl -X POST -d <<json-request-body>> http://math.engine/api/models/120/72c361803a5b116e0682581fe958fdee/calculate
    
### Response 200 data example
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
      "param2" : 0.0,
      "mgp_code" : "DISTRIBUTION",
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

## Method: RELEASE
### DELETE /api/models/:model-id/:event-id
Deallocates models resources for selected event.

### Params
* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

### Responses
* 204 if deallocation was successful
* 404 "ENF" if event id is not found
* 423 "CIP" if engine is busy, event is locked and computation in progress

### Request example
        
    curl --request DELETE http://math.engine/api/models/120/free/754fbcde-662c-4b3f-9f51-acb4e67913da
