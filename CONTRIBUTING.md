
We're glad you're thinking about contributing to the project.
Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

## Contributing to the project
We use [GitHub](https://github.com/udaan-com/r2dbi) as our central development hub.
[Issues](https://github.com/udaan-com/r2dbi/issues), and [Pull Requests](https://github.com/udaan-com/r2dbi/pulls) all happen here.

If you find a bug that you can reproduce, please file an issue with the project. 
Contributions in the form of bug reports along with potential fixes are greatly appreciated. 
You're encouraged to submit a pull request with your proposed solution. 
Including a test that showcases both the bug and the fix will expedite the process.

We may provide feedback on PRs asking you to make changes to your code even if it was not obvious 
when you wrote the code and there were no documented rules. This is unfortunate and we apologize for 
that in advance.

We value backwards compatibility for our API. Large PRs that affect the public API will receive a lot of scrutiny.

### Coding guidelines

* R2dbi is a library and any dependency that we use, we also force upon our users. Minimize the footprint of external dependencies;
* Prefer stateless, immutable objects (`data` classes) over anything else.
* Ensure that the code is thread-safe wherever required. Clearly document the thread-safety requirement in the changes. 
* Maintain backward compatability all the time. If an API must be discouraged, mark it `@Deprecated` and keep it functionally intact
* Make minimal changes to the code. Ensure that the internals of the library are not exposed. Use `internal`/`private` to limit the scope aggressively.
* Any new public interface/method should be marked with [`@ExperimentalAPI`] to tell users to not rely on it 

*Please run `mvn clean install` locally before opening a PR. Your local build run from the command line should pass.*

### Testing

* JUnit 5 and `testcontainers` are used for all testing
* code is tested for `mssql` and `postgreSQL` using testcontainers.
* ensure that the functionality is test for both the databases

## Development Setup

Most modern IDEs configure themselves correctly by importing the repository as an Apache Maven project.

### Building
use `mvn clean install` to build and install the package locally
