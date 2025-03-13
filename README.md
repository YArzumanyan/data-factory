# data-factory (WIP)

## Data Factory Overview

The data factory is a collection of 4 applications/components that will work together. The idea of the project is to create a portal where the users can create pipelines, run and share them. Each pipeline will be constructed of datasets and scripts that will be used to run the pipeline. The 4 components making this possible will be Metadata Store, Manager CLI, Executor CLI and Database.

## Pipeline configuration

Pipeline is a collection of scripts and datasets that are connected together, when executed it should run everything in a certain order and yield a result at the end. Each pipeline will be represented in a json format like a directed graph. Each edge is a sort of input for a node and each node is a dataset or a script. Inputs for each node is a dataset or an output from another script, and outputs are results of a script. The configuration will have multiple ways of linking the datasets and scripts.

## Json schema

There will be a json schema for the configuration which will be used application-wide to validate the config file. The config file will several properties. This schema doesn't have to be enforced too much in a form of schema file, but it will be in more details validated programatically.

## ROOT type

### type

An array of strings, that are the types of the object. Based on the types it will be validated and worked with differently in the application.
- `pipeline`: this object is a pipeline, always a standalone file
- `dataset`: this object is a dataset
	- `dataset-file`: This means that this is a standalone file with dataset configuration. This dataset requires `distribution` property and unique node `id`
	- `dataset-derived`: this dataset will be calculated by a pipeline. requires `pipeline` property. If the pipeline requires additional bindings, then this will also require to add `bindings` property
	- `dataset-reference`: dataset that will be referenced by id through cli when generating the pipeline. This is the id of a dataset-file from `Metadata Store`
- `script`: this object is a script
	- `script-file`: This means that this is a standalone file with script configuration. This script requires `distribution` property and unique node `id`
	- `script-reference`: Script That will be referenced by id through cli when generating the pipeline. This is the id of a script-file from `Metadata Store`

_Note: In database combinations can be saved as 8 bit number as there are 7 items and some of them can't even be at the same time_   

### previousVersion (optional)

This is a reference of the previous version of this object. the `url` of the previous version is written here

## PIPELINE type extends ROOT type

### url

String identifier, be it a simple id or a path to the resource (https, ftp, file and such)

### nodes

Array of SCRIPT and DATASET types

## SCRIPT and DATASET type extends ROOT type

### id

Is the node id used for referencing the nodes

### label (optional)

Name of the node

### dependsOn

Array of node ids that this node depends on. This is used to organize the run order of the nodes.

### pipeline (for dataset-derived type only)

The `url` of the pipeline that will be used to calculate this dataset.

### bindings (for dataset-derived type only (optional)) 

Will be the bindings that will be used for the calculation of the referenced pipeline if it requires additional bindings. It will be an array of objects with properties:
- `node` - the id of the node in the referenced `pipeline` that will be binded,
- `dataset` - the id of the node in current pipeline that will be binded to `node` 

### distribution

An object that will have `url` property linking where will the script or dataset be downloaded from or used from.
If the object has `distribution`, the `pipeline` and `bindings` properties will be ignored

## Components

### Database

This component will be a simple storage with basic api interface. The datasets and scripts will be stored here. They will be referenced from the `Metadata Store`

#### Implementation

- Application will be written in python using FastAPI for communication. 
- For storage a simple nosql database should suffice
- Endpoints will be documented by open api / swagger

### Metadata Store

It will store:
- The configuration file of the pipline.
- The standalone config files of scripts and datasets

Here all the files linked by `distribution.url` that were uploaded to `Database` will be linked to `Database`

#### Implementation

- The store will be created with java Spring Boot for REST API
- Storage could be some simple nosql database
- Endpoints will be documented by open api / swagger

### Manager CLI

This is the cli application that will be used to work with the pipeline config creation. Once the pipeline json file is created by the user. This cli can be used to validate, modify the config file to be deploy-ready and deploy this pipeline and referenced standalone dataset/script files to `Metadata Store`.

#### *Validation*

As mentioned in Pipeline configuration section, this function will be making sure the basic syntax of the json is correct and all the conditional definitions make sense. This goes for the pipeline and referenced standalone files.

#### *Preparing for deployment*

This function will modify the pipeline json and referenced standalone files so that they can be deployed to `Metadata Store`:

1. All the datasets and scripts that are stored locally (with file:// prefix), will be uploaded to `Database` and their id will be returned. User will have an option in cli to upload all the files and not only local ones (download from the mentioned distribution and upload to `Database`). 
2. Modify the pipeline and standalone files and change all the `url` in `distribution`s that were uploaded to `Database` to the ids from `Database`. The `url` will be distinguishable with a prefix, so that it's understood by the applications that it's an id from `Database`

#### *Deployment*

When the pipeline and the standalone files are finally ready they will be uploaded to the `Metadata Store`

### Implementation

- The component will be implemented in python with typer library for user interractions

### Executor CLI

Through this cli user will use the pipeline id to fetch the pipeline from `Metadata Store` and generate an orchestrator script in python. The orchestrator script will be ran through this cli. It will create and setup a docker container and copy the pipeline and orchestrator script inside it. Then run the orchestrator script.
The root pipeline can also have bindings through cli. Meaning the pipeline is ran with --dataset-binding and --script-binding options followed by space separated bindings. Example:

```bash
executor-cli run pipeline_id --dataset-binding dataset-ref=the-dataset --script-binding --script-ref=the-script
```

where the `dataset-ref` is the id of the dataset with type 'dataset-reference' and `the-dataset` is the id of the standalone dataset file. Script binding works similarly

#### *Orchestrator script*
 
1. Fetch pipeline config file
2. Fetch standalone config files
3. Organize the run order based on dependsOn property of the nodes
4. Fetch all the required datasets and scripts for the pipeline using only distribution `url`s
5. Run the scripts with defined bindings
6. For each derived dataset we reach in this order that doesn't have `distribution` property in pipeline config file:
   - Fetch the pipeline from `Metadata Store` and generate an orchestrator script
   - Run the orchestrator script and save the result locally
   - Modify the config file, add `distribution` property with `url` as the local result

#### *Finalization*

The final result will be extracted from the docker container and the docker container will be removed.

#### *Implementation*

- The component will be implemented in python with typer library for user interractions
- Will use libraries to work with docker and http requests

## Assembly

All the components will have Dockerfiles and will be assembled by a main docker-composer.

Because the `Manager CLI` creates a docker container, to avoid creating container inside a container, the docker compose file will mound the docker socket as well.

## Data schema
 
File dataset:
```json
{
  "id": "dataset-students",
  "type": ["dataset", "dataset-file"],
  "distribution": {
    "url": "https://webik.mff.cuni.cz/students/2024-2025.csv"
  }
}
```
 
Pipeline:
```json
{
  "id": "students-by-study-program",
  "type": ["pipeline"],
  "previousVersion": "students-by-study-program-v0",
  "nodes": [{
    "id": "script-filter-dw",
    "type": ["script", "script-reference"],
  }, {
    "id": "input-students",
    "type": ["dataset-reference"],
    "label": "Students to filter"
  }]
}
```
 
Script:
```json
{
  "id": "script-filter-dw",
  "type": ["script", "script-file"],
  "previousVersion": "script-filter-dw-v0",
  "distribution": {
    "url": "https://webik.mff.cuni.cz/scripts/execute.zip"
  },
  // TODO Entrypoint ..
}
```
 
Run pipeline:
```bash
executor-cli pipeline run students-by-study-program --dataset-binding input-students=dataset-students --script-binding script-filter-dw=script-filter-dw
```
 
Derived dataset:
```json
{
  "id": "dw-students",
  "type": ["dataset", "dataset-derived"],
  "pipeline": "students-by-study-program",
  "bindings": [{
    "node": "input-students",
    "dataset": "dataset-students"
  }]
}
```
 
Compute dataset:
```bash
executor-cli dataset compute dw-students
```
 
Derived dataset after computation:
```json
{
  "id": "dw-students",
  "type": ["dataset", "dataset-derived"],
  "pipeline": "students-by-study-program",
  "bindings": [{
    "node": "input-students",
    "dataset": "dataset-students"
  }],
  // NEW COMPUTED BY ...
  "distribution": {
    "url": "https://webik.mff.cuni.cz/students/2024-2025.csv"
  }
}
```

The computation most probably will be part of running pipeline instead of it being a standalone command
 
## API abstract architecture

### Separate endpoints per-type
- api/v1/pipeline
  - getPipelines
- api/v1/file-dataset
- api/v1/derived-dataset
- api/v1/dataset
 
### Single endpoint
Split by value of "type"
- filter ... api/v2/entries?type=pipeline
  - getPipelines

It is possible to have both side by side, the business logic remains the same. The Separate enpoints will be implemented first as it's closer to REST architecture