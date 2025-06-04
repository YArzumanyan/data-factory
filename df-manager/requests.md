# Example Requests for Data Factory Manager API

This document provides example requests for the Data Factory Manager API.

## Datasets

### Upload a Dataset

```http
POST /api/v1/datasets
Content-Type: multipart/form-data

# Form fields
title: Sample Dataset
description: This is a sample dataset for demonstration purposes

# File
file: @path/to/your/dataset.csv
```

### Get Dataset Metadata

```http
GET /api/v1/datasets/550e8400-e29b-41d4-a716-446655440000
```

## Plugins

### Upload a Plugin

```http
POST /api/v1/plugins
Content-Type: multipart/form-data

# Form fields
title: CSV Parser Plugin
description: A plugin that parses CSV files

# File
file: @path/to/your/plugin.jar
```

### Get Plugin Metadata

```http
GET /api/v1/plugins/550e8400-e29b-41d4-a716-446655440000
```

## Pipelines

### Create a Pipeline

```http
POST /api/v1/pipelines
Content-Type: application/json

{
  "title": "Data Processing Pipeline",
  "description": "A pipeline that processes data from CSV to JSON",
  "variables": [
    {
      "id": "input_data",
      "title": "Input CSV Data",
      "datasetUuid": "550e8400-e29b-41d4-a716-446655440000"
    },
    {
      "id": "processed_data",
      "title": "Processed Data"
    },
    {
      "id": "output_data",
      "title": "Output JSON Data"
    }
  ],
  "steps": [
    {
      "id": "process_step",
      "title": "Process CSV Data",
      "pluginUuid": "550e8400-e29b-41d4-a716-446655440001",
      "inputs": ["input_data"],
      "outputs": ["processed_data"]
    },
    {
      "id": "convert_step",
      "title": "Convert to JSON",
      "pluginUuid": "550e8400-e29b-41d4-a716-446655440002",
      "inputs": ["processed_data"],
      "outputs": ["output_data"],
      "precededBy": ["process_step"]
    }
  ]
}
```

### Get Pipeline Metadata

```http
GET /api/v1/pipelines/550e8400-e29b-41d4-a716-446655440000
```