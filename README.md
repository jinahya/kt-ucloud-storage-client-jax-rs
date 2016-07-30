# kt-ucloud-storage-client-jax-rs
[![Maven Central](https://img.shields.io/maven-central/v/com.github.jinahya/kt-ucloud-storage-client-jax-rs.svg?maxAge=2592000)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.jinahya%22%20AND%20a%3A%22kt-ucloud-storage-client-jax-rs%22)
[![Javadocs](http://www.javadoc.io/badge/com.github.jinahya/kt-ucloud-storage-client-jax-rs.svg)](http://www.javadoc.io/doc/com.github.jinahya/kt-ucloud-storage-client-jax-rs)

a simple client for [kt ucloud storage](https://ucloudbiz.olleh.com/portal/ktcloudportal.epc.productintro.ss.info.html).

## basic usage
```java
final String url; // authentication url
final String user; // access key ID
final String pass; // secret key
final StorageClient client = new StorageClient(url, user, pass);
```
### authenticate user
```java
final int statusCode = client.authenticateUser(r -> r.getStatus());
assert statusCode == 200;
```

## container
### create/update
```
final Family family = client.updateContainer(
    "containerName",
    null, // query paramters; MultivaluedMap<String, Object>
    null, // request heaers; MultivaluedMap<String, Object>
    r -> r.getStatusInfo().getFamily());
assert family == Family.SUCCESSFUL;
```
### read
```
final Family family = client.readContainer(
    "containerName",
    null, // query paramters; MultivaluedMap<String, Object>
    null, // request heaers; MultivaluedMap<String, Object>
    r -> r.getStatusInfo().getFamily());
assert family == Family.SUCCESSFUL;
```
#### consuming object names
```java
// clear the container
client.withObjectNames(
        containerName,
        null,
        null,
        (on, c) -> {
            c.deleteObject(
                    containerName,
                    on,
                    null,
                    null,
                    r -> {
                        // this is an object deletion result
                    }
            );
        }
);
```
### delete
```
client.deleteContainer(
    "containerName",
    null,
    null,
    r -> {
    }
);
```
## object
### create/update
```java
final Entity<?> entity = getSome();
final int status = client.updateObject(
    "containerName",
    "objectName",
    null,
    null,
    entity,
    r -> {
        // mess with the Response(r) here
    }
);
```
### read
```java
client.readObject(
    "containerName",
    "objectName",
    null,
    null,
    r -> {
        // mess with the Response(r) here
    }
);
```
### delete
```java
client.deleteObject(
    "containerName",
    "objectName",
    null,
    null,
    r -> {
        // mess with the Response(r) here
    }
);
```
