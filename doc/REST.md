# Math Engine

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC 2119](http://www.ietf.org/rfc/rfc2119.txt).


# Event conduction using math engine

Math Engine предназначен для расчета матемитических моделей описанных в формате MS Excel.
Порядок поготовки моделей для Math engine(ME) описан в документе Guidelines.
В данном документе мы будет оперировать такими понятиям как IN-params и OUT-values, которые предстваляют из себя IN/OUT sheets в excel файле. IN/OUT sheet явзяются endpoits для взяимодействия модели и math engine.


Порядок работы с ME состоит из следующих шагов:
1. Passing unique event-id
2. Filling IN-params and getting model's OUT-values
3. Deallocating resources

1. Passing unique event-id
Для ускорения расчета ME выделяет вычислительные ресурсы под каждое событие. Что бы уникально иденитфицировать событие ME необходим уникатльный event-id. Пара event-id + model-id однозначно идентифицирует собитие внитри ME.

Ресурсы выделяются "лениво", при превом запросе методов CALCULATE/IN-PARAMS, поэтому результат первого запроса вычисляется дольше чем последующие. Также ресурсы будут выделены повторно поле вызова метода RELEASE.

2. Filling IN-params and getting model's OUT-values
IN-params в модели должны быть предствленны в виде обязательных пар id + value на странице IN. IN sheet может содержать любое кол-во опциональных колонок.
API позволяет получить предствление IN sheet в виде JSON с помощью метода IN-PARAMS.

OUT-values это сериализированныя в JSON страница OUT рассчитанная по заданым IN-params.
Что бы получить OUT-values нужно выполнить метод CALCULATE с заданными IN-params.

Что бы сделать повторный запрос на расчет нужно дождаться завершения предыдущего для данного события, иначе ME вернет ошибку 423 "calculation in progress".

3. Deallocating resources
После окончания события необходимо освободить ресурсы ME с помощью метода RELEASE.
Ресурсы осовбождаются автоматически если к заданному событию не было запросов через интервал <<NNN>>


# REST API

Any request may return 500 errors if internal error occured.
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
### GET /models/:model-id/:event-id/in-params
Return in params for specified model. event-id and model-id couple should be unique.

### Params
* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

### Responses
* 200  if in-params found
* 404 "MNF" if model not found

### Request example

    curl http://math.engine/model/120/72c361803a5b116e0682581fe958fdee/in-params

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
### POST /models/:model-id/:event-id/calculate

### Params
* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

### Request body
```
{
  "model_id" : 120, // int
  "event_id" : "72c361803a5b116e0682581fe958fdee",  // string
  "params" : [
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

    curl -X POST -d <<json-request-body>> http://math.engine/model/120/72c361803a5b116e0682581fe958fdee/calculate
    
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
### DELETE /models/:model-id/:event-id
Deallocates models resources for selected event.

### Params
* model-id - required, ID of model to which request is applied
* event-id - required, unique event id

### Responses
* 204 if deallocation was successful
* 404 "ENF" if event id is not found
* 423 "CIP" if engine is busy, event is locked and computation in progress

### Request example
        
    curl --request DELETE http://math.engine/model/120/free/754fbcde-662c-4b3f-9f51-acb4e67913da
