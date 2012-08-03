# Hakea

The Hakea [Git] [1] repository and source code indexer.

# Requirements

[Java] [2] (version 1.6) is required to run the indexing service and [Solr] [3] distribution.

# Building and running

Hakea is broken up into three major components:

1. The core library
2. The indexing service
3. The Solr distribution

This design aims to separate the concerns of indexing from the implementation of the indexer itself. Consumers of the core library can utilize the available processors to index an arbitrary collection of projects, or Git repositories.

Hakea's default indexer implementation is the indexing service, which has been built on top of [Dropwizard] [4].

Use Maven to build each component:

    $ mvn package

The core library is packaged as a `JAR`, the indexing service is packaged as a "fat" `JAR`, and the Solr distribution is packaged as a `ZIP` archive.

Move the Solr distribution out of the `hakea-solr/target/` directory and to someplace else Maven cannot accidentally delete it. Decompress the archive and start Solr:

    $ java -jar start.jar

Take the indexing service `JAR` from `hakea-indexing-service/target/`, configuration file from `hakea-indexing-service/conf/hakea-indexing-service.yml`, and place them in another safe place. Open the configuration file and point Hakea to the Git repositories to index. Time to get the indexing service running.

Running the indexing service is relatively easy. All of the dependencies required to run the indexing service are included in the `JAR`, so as long as the configuration file is ready the indexing can begin:

    $ java -server -jar hakea-indexing-service-0.0.1-SNAPSHOT.jar server hakea-indexing-service.yml

The indexing service will now attempt to clone all configured Git repositories and begin indexing. After indexing has completed successfully the indexing service will monitor those Git repositories for changes.

# The indexer crashed! What did I do wrong?

Nothing at all. Depending on the size of the Git repositories Hakea is indexing it may be necessary to run the indexing service with more memory. To give the index service more room to work add the following flags to the `java` command:

    $ java -server -Xms2048M -Xmx2048M -jar …

Continue to increase the amount of memory available to the indexer until it is able to finish indexing successfully.

# License

Copyright (c) 2012 Eric Czarny.

Hakea should be accompanied by a LICENSE file containing the license relevant to this distribution.

If no LICENSE exists please contact Eric Czarny <eczarny@gmail.com>.

[1]: http://git-scm.com/
[2]: http://www.java.com/
[3]: http://lucene.apache.org/solr/
[4]: http://dropwizard.codahale.com/
