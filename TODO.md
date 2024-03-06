# TODO.md

## Current tasks

### Todo

- [ ] Refine error messages when the actual error is nested below the top level  

### Doing

- [ ] Finish `indexed` sad case  

### Done ✓


## Possible future features

Instead of strictly being an identity function, what if a type constructor added `(with-meta ...)` pointing back to a machine-readable version of themselves?  Then one could have a reflective notion of identity for at least some kinds of data in a running Clojure(script) instance.
