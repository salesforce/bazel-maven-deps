# Integration Tests

Data in this directory is used by `tool/src/test/java/com/salesforce/tools/corebuild/graph/tool/cli/it/JarWithDependenciesIT.java`.

The implementation is pretty generic:
1. Content from `source` is copied to `tool/target/it/<test>`
2. Graph tool is run using `tool/target/module-graph-tool-..-jar-with-dependencies.jar`
3. Output in `tool/target/it/<test>` is verified to ensure everything form `expected` is there.

