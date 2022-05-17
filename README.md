# goldly studio

- goldly studio lets you get started with clojure datascience work quickly
- it contains:
  - the most common clojure datascience libraries
  - visualizations
  - notebooks


## run

First, clone this repo. Then run: `clojure -X:docs-run`
After starting, open web-browser on port 8080.

## build

You only need to build a custom cljs build:
- if you want to use ui-components that are not included in goldly-docs.
- if you want you want to compile your own clojurescript code.
  (this could be relevant if you need say core.async which is not available in 
  sci interpreted clojurescript)

For this demo, it is not neccesary to build cljs. But lets do it for fun anyhow.

The following commands will build a bundel, and recompile it in case a source file
changes. Shadow-cljs calls this "watch".


``` 
  clojure -X:docs-build :profile '"npm-install"'
  clojure -X:docs-build :profile '"compile2"'
  clojure -X:docs-run
```




