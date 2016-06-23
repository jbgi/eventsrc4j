# Eventsrc4j

Purely functional event sourcing library (not framework) for Java!
Largely inspired by the excellent scala library https://bitbucket.org/atlassianlabs/eventsrc 

# What is event sourcing

The basic concept is that we represent the state of data as a series of immutable 'events'. These events are ordered, and when we replay them in time-sequence order, we can generate a view or snapshot of our data at any point in time. This gives us a number of useful characteristics including:

- In-built audit trail and recoverability - since we don't delete or mutate data, we can always determine what our data was at any point in time.
- Flexibility in our queries - By re-interpreting the events we can generate new views from existing data.
- Easy support for a messaging-based service architecture - We can also send these events to other systems in a reliable and scalable fashion where it can be processed, typically in a more efficient manner for a specific use case e.g. sending events to ElasticSearch for full-text search of entities. We can also send these events to a remote replica easily for disaster recovery.
