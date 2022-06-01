# goldly studio

- goldly studio lets you get started with clojure datascience work quickly
- it contains:
  - the most common clojure datascience libraries
  - visualizations
  - notebooks and sample-datas


## run

First, clone this repo. Then run: `clojure -X:docs`
After starting, open web-browser on port 8080.

## eval all notebooks

Run: `clojure -X:nbeval`

## run - including python notebooks

You need to have python3 executeable installed.

One option is to install guix to your operating system and then 
start an adhoc guix environment with `bb python-env`

Then run: `clojure -X:docs:python:jdk-17`

After starting, open web-browser on port 8080.





