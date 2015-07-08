TODO:
- Allow for null to be stored in the context. (Null values currently lead to containsKey() returning false.)
- Reduce the number of times in which package names are defined as strings.
- Ensure that packaging is robust (jar is built fine, properties file found in correct locations, etc.)
- ADD UNIT TESTS!
- Add auto completion for parameters. Auto complete currently works for command names, but it would be cool to be able to type a command name, and then begin auto completing the valid parameters specific to that command. Even better, it would be cool to be able to auto complete parameter values (such as when the parameter is known to point to a file, auto complete using file names.)
