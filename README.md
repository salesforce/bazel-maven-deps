# bazel_maven_deps

Maven Dependencies for Bazel is an alternate approach for working with Maven dependencies in Bazel.
In comparison to [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external) the following key difference exist.

1. Enforcement of pinning, i.e. it's not possible to use dependencies without pinning.
2. Use of multiple files instead of a single file reducing contention in version control for large mono-repos with frequent updates per day.
3. Command line tool for managing dependencies.
4. Use of Maven Artifact Resolver with support for caching in the local Maven repository.


## Usage

### Setup in WORKSPACE

Please see the GitHub releases for snippet how to embed into `WORKSPACE` file.

### Invoke CLI

```shell
> bazel run @bazel_maven_deps//:cli -- --help
...

Usage: dependencies-tool [-h] COMMAND
  -h, --help   Prints this help text
Commands:
  lint-dependency-collection  Reformat and check the
                                //third_party/dependencies/*.bzl files.
  pin-dependencies            Resolve all dependencies from the collection and
                                generate the pinned catalog.
  print-dependency-catalog    Prints the content of the pinned catalog.
  get-version-variable        Returns the value of a version variable in
                                //third_party/dependencies/*.bzl files.
  set-dependency-version      Set a dependency version in
                                //third_party/dependencies/*.bzl files.
  set-version-variable        Set a version variable in
                                //third_party/dependencies/*.bzl files.
  add-dependency              Add a dependency to //third_party/dependencies/*.
                                bzl files.
  remove-dependency           Remove a dependency from
                                //third_party/dependencies/*.bzl files.
  dependency-info             Prints information about a dependency.
```

### Add a Maven dependency

```shell
> bazel run @bazel_maven_deps//:cli -- add-dependency 'foo.bar:whatever:3.1.1'
```

### Load pinned catalog in WORKSPACE

In order to activate the pinned catalog must be loaded.
Please add the following to your `WORKSPACE` file.

```Starlark
load("//third_party/dependencies/pinned_catalog:index.bzl", "setup_maven_dependencies")
setup_maven_dependencies()
```

If you want you can give a list of `maven_servers` to use an internal repository.
Please make sure your `.netrc` file is properly populated.
You can also use Bazel's download rewrite configuration if needed.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)
