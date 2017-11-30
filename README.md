# SFTP to Batch Job POC -  Use case #2

* SftpOutboundGateway -> task-launcher -> batch job
* Tested with kafka binder only, local dataflow and local SFTP
* POC code, handles specific use case

# Pre-req's:
* SFTP server
* Local DataFlow server and shell
* Built file ingest batch job jar from [Batch Ingest Sample](https://github.com/spring-cloud/spring-cloud-dataflow-samples/tree/master/batch/file-ingest)
* Built SFTP filename transformer jar from [sftp-filename-processor](https://github.com/chrisjs/sftp-filename-processor)

# Build source
```
$ mvn clean package
```

# Init apps if needed

dataflow:> app import --uri http://bit.ly/Bacon-RELEASE-stream-applications-kafka-10-maven

# Register source

dataflow:> app register --name sftpLister --type source --uri file:////path/to/sftp-lister-source-1.0.0.jar

# Register processor

dataflow:> app register --name filenameTransformer --type processor --uri file:////path/to/sftp-filename-processor-X.X.X.jar

# Create inbound SFTP lister stream

dataflow:> stream create inboundSftpLister --definition "sftpLister --username=user --password=pass --host=127.0.0.1 --port=6666 --allow-unknown-keys=true --remote-dir=/remote/dir/ | filenameTransformer --uri=file:////path/to/ingest-1.0.0.jar --data-source-url=jdbc:h2:tcp://localhost:19092/mem:dataflow --data-source-user-name=sa --sftp-username=user --sftp-password=pass --sftp-host=127.0.0.1 --sftp-port=6666 --remote-file-path-job-parameter-name=remoteFilePath --local-file-path-job-parameter-name=localFilePath --local-file-path-job-parameter-value=/local/path/ > :filesAvailable"

# Deploy stream enabling message header passthrough

dataflow:> stream deploy inboundSftpLister --properties "app.sftpLister.spring.cloud.stream.kafka.binder.headers=file_remoteFile,file_remoteDirectory"

# Create ingest job stream

dataflow:> stream create ingestJob --definition ":filesAvailable > task-launcher-local" --deploy

* Copy data to SFTP location for retrieval

# Clean up

dataflow:> app unregister --name sftpLister --type source

dataflow:> app unregister --name filenameTransformer --type processor

dataflow:> stream destroy inboundSftpLister

dataflow:> stream destroy ingestJob

