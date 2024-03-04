# R2Dbi
R2Dbi is a Kotlin library inspired by [JDBI](https://github.com/jdbi/jdbi) and built on Reactive Relational Database Connectivity ([R2DBC](https://r2dbc.io/)) SPI. 
R2DBC brings [Reactive Programming APIs](https://projectreactor.io/) to relational databases. R2DBI simplifies database operations by offering a 
declarative style similar to [JDBI SqlObjects](https://jdbi.org/#sql-objects). 
It provides easy-to-use interfaces for executing `SQL queries` and mapping `data` to `objects`.  

While primarily designed for Kotlin, it may also work with Java, although it hasn't been tested yet.
R2dbi started out to be used in declarative mode (ie define an annotated `interface`). The fluent interface is still
being developed and hence not ready to use. It is available to experiment and play with.

# License
R2Dbi is licensed under the commercial friendly [Apache 2.0 license](LICENSE).

# Basic Usage
define the SQL to execute and the shape of the results - by creating an annotated `interface`.
```kotlin
import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

// Declare the API using annotations on a Java interface
interface UserDao {
    // Using kotlinx.coroutines.flow.Flow
    @SqlQuery("SELECT 1")
    fun getOne(): Flow<Long>
    
    @SqlQuery("SELECT * FROM 'user' where name = :name")
    fun findUser(@Bind("name") name: String): Flow<User>;

    // Using reactor.core.publisher.Flux
    @SqlQuery("SELECT * FROM 'user'")
    fun getAllUsers(): Flux<User>;

}
```

This library supports both `kotlinx.coroutines.flow.Flow` and `org.reactivestreams.Publisher` / `reactor.core.publisher.Flux`
as the return type. Note that no other return type (especially blocking) is supported. 

You look at [TestQueryDao.kt](r2dbi-core%2Fsrc%2Ftest%2Fkotlin%2Fcom%2Fudaan%2Fr2dbi%2FtestDao%2FTestQueryDao.kt) and 
[TestDynamicInterfaceBase.kt](r2dbi-core%2Fsrc%2Ftest%2Fkotlin%2Fcom%2Fudaan%2Fr2dbi%2FTestDynamicInterfaceBase.kt) to checkout
more examples.

> 🚧<br/>
> Before utilizing this library, it's crucial to grasp the fundamentals of Coroutines and Project Reactor.
> <br/>
> For Instance, without subscribing to a `Publisher` or attempting to materialize a value of a `Flow`, the underlying SQL query won't execute.

# Building, Testing, Contributing
see [CONTRIBUTING.md](CONTRIBUTING.md)

# Versioning
TODO

# Project Members
[Shashwat Agarwal](https://github.com/shashwata)


