# Prometheus exporter for OpenTSDB

## Motivation
Currently there is no off-the-shelf solution for integrating the Prometheus alerting server with OpenTSDB. Prometheus employs a pull model to fuel its alerts: it periodically scrapes a predefined set of endpoints which expose the alerting-relevant metrics in a specific format that can be parsed by the Prometheus server component. This repo implements a software component that is capable of exposing data from an OpenTSDB server in a Prometheus server friendly format.

## How it works
The OpenTSDB exporter module is nothing more than a simple HTTP server which exposes two endpoints:

 - `/config` - the configuration endpoint returns the mapping rules between the time-series data stored in OpenTSDB and the metrics exposed to the Prometheus server on the /metrics endpoint (see below)
 - `/metrics` -  the endpoint that is being periodically scraped by the Prometheus server component. Issuing HTTP GET requests against this endpoint will present the alerting information in a form that can be understood by the Prometheus server. The content of the exposed alerting info is determined by the configuration data uploaded via the `/config` endpoint (see above).
 
Before the TSDB exporter can expose any alerting info on the `/metrics` endpoint for the Prometheus server to consume, one needs to define a way to translate the time-series data stored in OpenTSDB into something the Prometheus server can understand. However this type of mapping is not straight-forward: the two systems handle data in two completely different ways based on different assumptions and conventions. One can simply cannot define a single pre-defined way of exposing data from OpenTSDB (which deals with time-series data) to Prometheus-server metrics (which are simple tagged values that are being sampled at periodic time intervals).

This component implements a user-defined mechanism for specifying this translation process in the form of a set of JSON-formatted configuration files. The application periodically scans the contents of a particular user-specified folder (part of the application's configuration) and loads all the JSON files it can find and parses the metric definitions stored within. The remainder of this section describes the format of these definitions.

A single JSON metric configuration file contains a set of _metric definitions_. A metric definition links a single Prometheus metric to a single OpenTSDB query. Below is an example of a Prometheus metric (this is the format understood by Prometheus server):

```
# HELP http_request_duration_microseconds The HTTP request latencies in microseconds.
# TYPE http_request_duration_microseconds summary
http_request_duration_microseconds{handler="alerts",quantile="0.5"} NaN
http_request_duration_microseconds{handler="alerts",quantile="0.9"} NaN
http_request_duration_microseconds{handler="alerts",quantile="0.99"} NaN
```

Note that a single Prometheus metric has the following properties:

 - **name** - in our case this is `http_request_duration_microseconds`
 - **description** - in our case this is `The HTTP request latencies in microseconds.`
 - **type** - in our case this is `summary`
 - **set of tagged values** - a Prometheus metric consists in a set of values, each value with a different set of tags (i.e. key - value pairs). The tags are optional: for example, you can have a metric with a single non-tagged value.
 
There are a number of restrictions on the values allowed for each of these properties. For example, the **name** property only allows alpha-numeric characters and underscores. At the same time, the **type** property must be one of the following: `counter`, `gauge`, `summary` and `histogram`. For more information about the best practices for metric and label naming see this [link](https://prometheus.io/docs/practices/naming/). Another important resource for an in-depth look at the Prometheus data model is available [here](https://prometheus.io/docs/concepts/data_model/).

On the other side of the equation we have OpenTSDB queries (exposed by the OpenTSDB server on the `/api/query` endpoint). Details about the ins-and-outs of the REST APIs used by this component can be found at this [page](http://opentsdb.net/docs/build/html/api_http/query/index.html). On of the key aspects about the REST API exposed by OpenTSDB is that it allows us to bundle multiple _sub-queries_ into a single HTTP call. We took advantage of this possibility to establish the following mapping between Prometheus metrics and OpenTSDB queries:

 - a single Prometheus metric corresponds to a single OpenTSDB query (i.e. HTTP call against the OpenTSDB server)
 - a single tagged value that is part of a particular Prometheus metric corresponds to a single sub-query inside the associated OpenTSDB query for that particular Prometheus metric.
 
 In a nutshell, we map the tagged values that are part of a Prometheus metric to a set of sub-queries that are part of an aggregate OpenTSDB query. This mapping is defined by the user through a set of JSON-formatted configuration files.

## Understanding the mappings configuration file
 We'll use the power of an example to better illustrate the structure of the mappings configuration file:
 
  - we have a Prometheus metric called `test_app_metrics_one` of type `counter` that is composed of two tagged values as follows:
  
    - the first tagged value with tags `{severity=warning;escalation=email}` should be mapped to an OpenTSDB query on the `test.app.metrics.one` time-series tagged with `environment=stage` aggregated via the `avg` operator over the last 10 seconds.
 
    - the second tagged value with tags `{severity=critical;escalation=pagerduty}` should be mapped to an OpenTSDB query on the `test.app.metrics.one` time-series tagged with `environment=prod` aggregated via the `avg` operator over the last 10 seconds.
    
The output presented by our component when the Prometheus server queries the `/metrics` endpoint should look something like below:

```
# HELP test_app_metrics_one TestApp metrics: one
# TYPE test_app_metrics_one counter
test_app_metrics_one{severity=warning,escalation=email} 15
test_app_metrics_one{severity=critical,escalation=pagerduty} 10
```

The query that hits the OpenTSDB server on the `/api/query` endpoint should be a POST HTTP request with the following JSON payload:

```json
{
    "start": "10s-ago",
    "showQuery": true,
    "queries": [
        {          
          "metric": "test.app.metrics.one",
          "aggregator": "avg",
          "rate": false,
          "tags": {
            "environment": "stage"
          }
        },
        {
          "metric": "test.app.metrics.one",
          "aggregator": "avg",
          "rate": false,
          "tags": {
              "environment": "prod"
          }
        }
    ]
}
```

The responses returned by OpenTSDB for each of these queries will be mapped back by our component to the Prometheus metric and it's corresponding tagged values to yield the final result presented above. **NOTE**: the `"showQuery": true` flag attached to the OpenTSDB query allows us to match the OpenTSDB sub-query responses to the appropriate Prometheus tagged value.

The mapping configuration file that yields the results described in this example is shown in the listing below:

```json
[
  {
    "name": "test_app_metrics_one",
    "description": "TestApp metrics: one",
    "type": "counter",

    "query": {
      "start": "10s-ago",

      "mappings": [
        {
          "subQuery": {
            "metric": "test.app.metrics.one",
            "aggregator": "avg",
            "rate": false,
            "tags": {
              "environment": "stage"
            }
          },
          "prometheusTags": {
            "severity": "warning",
            "escalation": "email"
          }
        },
        {
          "subQuery": {
            "metric": "test.app.metrics.one",
            "aggregator": "avg",
            "rate": false,
            "tags": {
              "environment": "prod"
            }
          },
          "prometheusTags": {
            "severity": "critical",
            "escalation": "pager-duty"
          }
        }
      ]
    }
  }
]
```

The configuration file consists in a _list_ of Prometheus metrics definitions. Each entry in this list contains information about the Prometheus metric that is being exposed on the `/metrics` endpoint towards the Prometheus server component: things like the metric **name**, **description** and **type**. The key takeaway from this example is in the `query/mappings` entry: this is the part that describes how a specific tagged value on the Prometheus side maps to an OpenTSDB sub-query. 

The `subQuery` entry supports everything that an OpenTSDB query supports (see [this](http://opentsdb.net/docs/build/html/api_http/query/index.html) for complete details). In this very simplistic example we only specified the OpenTSDB metric **name**, **tags** and **aggregator**. But you can set options related to downsampling, filters, rate & rateOptions etc.

The result that OpenTSDB sends back in response to one of these queries looks like the following:

```json
[
  {
    "metric": "test.app.metrics.one",
    "tags": {
      "environment": "stage"
    },
    "aggregateTags": [],
    "query": {
      # info about the original sub-query which allows us to pair the
      # the incoming results with the original query
      ...
    },
    "dps": {
      "1492327102": 15,
      "1492327103": 15,
      "1492327104": 15,
      "1492327105": 15,
      "1492327106": 15,
      "1492327107": 15,
      "1492327108": 15,
      "1492327109": 15,
      "1492327111": 15
    }
  },
  {
    "metric": "test.app.metrics.one",
    "tags": {
      "environment": "prod"
    },
    "aggregateTags": [],
    "query": {
      # info about the original sub-query which allows us to pair the
      # the incoming results with the original query
      ...
    },
    "dps": {
      "1492327102": 10,
      "1492327103": 10,
      "1492327104": 10,
      "1492327105": 10,
      "1492327106": 10,
      "1492327107": 10,
      "1492327108": 10,
      "1492327109": 10,
      "1492327111": 10
    }
  }
]
```

One thing to note at this point is that the response corresponding to a single OpenTSDB query contains multiple data points (see the `dps`) entry. This means that in the analysis window that we selected (of 10 seconds ago) there were multiple values available for the `test.app.metrics.one` time series. The OpenTSDB exporter selects the **oldest available value** (i.e. corresponding to the largest timestamp) as the output metric value that is presented to the Prometheus server component.

## Configuration options
This section lists all the configuration options made available by the OpenTSDB exporter component:

| Config option |  Type  | Required | Description                 |
| ------------- | ------- | -------- | -------------------------- |
| OPEN_TSDB_URL | env var |   YES    | URL of the OpenTSDB server |
| METRICS_DIR   | env var |   YES    | Path to the folder where the metric definitions are stored |

**NOTE**: the OpenTSDB exporter will periodically scan the `METRICS_DIR` folder (currently set for 10 sec) and it will load/parse all the JSON files it finds at that location. You are not required to place all your metrics in a single configuration file. You can also place new files in that folder and the component will automatically pick them up w/o the need to restart it (there is 0 downtime for configuration change).

## Performance considerations
The OpenTSDB exporter will issue a separate query against the OpenTSDB server for each Prometheus metric (not for each individual tagged value because those are represented as OpenTSDB sub-queries) every time the Prometheus server scrapes the `/metrics` endpoint.

Thus, one can foresee a situation when a Prometheus server configuration with a high scraping setting (i.e. a small scraping window) in combination with a large number of metric definitions on the tsdb exporter side can lead to a high pressure on the OpenTSDB REST API endpoint.

**Example**: Prometheus server is configured with a scraping interval of 1s and there are 100 metric definitions on the OpenTSDB exporter side. In this scenario, the exporter will hit the OpenTSDB REST API endpoint with 100 individual queries/second.

