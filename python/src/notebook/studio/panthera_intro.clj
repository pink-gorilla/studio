(ns notebook.studio.panthera-intro
  (:require
   [panthera.panthera :as pt]
   [libpython-clj.python :as py]
   [pinkgorilla.python.core :refer [py-initialize!]]))

;;; # Data science intro with panthera
;;; ## Clojure + Pandas + Numpy = 💖
;;; 
;;; I'll show how it is possible to get the most out of the [Pandas](https://pandas.pydata.org/) & the Clojure ecosystem at the same time.
;;; This intro is based on this [Kaggle notebook](https://www.kaggle.com/kanncaa1/data-sciencetutorial-for-beginners) you can follow along with that if you come from the Python world.
;;; ## Env setup
;;; The easiest way to go is the provided [Docker image](https://cloud.docker.com/u/alanmarazzi/repository/docker/alanmarazzi/panthera), but if you want to setup your machine just follow along.
;;; ### System install
;;; If you want to install everything at the system level you should do something equivalent to what we do below:
;;; ```bash
;;; sudo apt-get update
;;; sudo apt-get install libpython3.6-dev
;;; pip3 install numpy pandas
;;; ```
;;; 
;;; ### conda
;;; To work within a conda environment just create a new one with:
;;; 
;;; ```bash
;;; conda create -n panthera python=3.6 numpy pandas
;;; conda activate panthera
;;; ```
;;; Than start your REPL from the activated conda environment. This is the best way to install requirements for panthera because in the process you get MKL as well with Numpy.
;;; 
;;; ### Here
;;; 
;;; Let's just add panthera to our classpath and we're good to go!
;;; Now require panthera main API namespace and define a little helper to better inspect data-frames


(py-initialize!)


(defn show
  "renders python object as html in gorilla-notebook"
  [obj]
  ^:R [:p/phtml
       (py/call-attr obj "to_html")])


;;; ## A brief primer
;;; We will work with Pokemons! Datasets are available [here](https://www.kaggle.com/kanncaa1/data-sciencetutorial-for-beginners/data).
;;; We can read data into panthera from various formats, one of the most used is `read-csv`. Most panthera functions accept either a data-frame and/or a series as a first argument, one or more required arguments and then a map of options.
;;; To see which options are available you can check docs or even original [Pandas docs](https://pandas.pydata.org/pandas-docs/stable/reference/api/pandas.read_csv.html#pandas.read_csv), just remember that if you pass keywords they'll be converted to Python automatically (for example `:index-col` becomes `index_col`), while if you pass strings you have to use its original name.
;;; Below as an example we `read-csv` our file, but we want to get only the first 10 rows, so we pass a map to the function like `{:nrows 10}`.

(pt/read-csv "./resources/pokemon/pokemon.csv" {:nrows 10})


;;; The cool thing is that we can chain operations, the threading first macro is our friend!
;;; Below we read the whole csv, get the correlation matrix and then show it

(-> (pt/read-csv "./resources/pokemon/pokemon.csv")
    pt/corr)

;;; Since we'll be using `pokemon.csv` a lot, let's give it a name, `defonce` is great here

(defonce pokemon (pt/read-csv "./resources/pokemon/pokemon.csv"))

;;; Let's see how plotting goes

(defn heatmap
  [data x y z]
  ^:R [:p/vega
       {:data {:values data}
        :width 500
        :height 500
        :encoding {:x {:field x
                       :type "nominal"}
                   :y {:field y
                       :type "nominal"}}
        :layer [{:mark "rect"
                 :encoding {:color {:field z
                                    :type "quantitative"}}}
                {:mark "text"
                 :encoding {:text
                            {:field z
                             :type "quantitative"
                             :format ".2f"}
                            :color {:value "white"}}}]}])

(-> pokemon
    pt/corr
    pt/reset-index
    (pt/melt {:id-vars :index})
    pt/->clj
    (heatmap :index :variable :value))

;;; What we did is plotting the heatmap of the correlation matrix shown above. Don't worry too much to all the steps we took, we'll be seeing all of them one by one later on!
;;; What if we already read our data but we want to see only some rows? We have the `head` function for that

(pt/head pokemon)

(pt/head pokemon 10)

;;; Another nice thing we can do is to get columns names


(pt/names pokemon)

;;; Now when you see an output as the above one, that means that the data we have is still in Python. That's ok if you keep working within panthera, but what if you want to do something with column names using Clojure?


(vec (pt/names pokemon))

;;; That's it! Just call `vec`and now you have a nice Clojure vector that you can deal with.
;;; > N.B.: with many Python objects you can directly treat them as similar Clojure collections. For instance in this case we can do something like below


(doseq [a (pt/names pokemon)] (println a))

;;; ## Some plotting
;;; Plotting is nice to learn how to munge data: you get a fast visual feedback and usually results are nice to look at!
;;; Let's plot `Speed` and `Defense`

(defn line-plot
  [data x y & [color]]
  (let [spec {:data {:values data}
              :mark "line"
              :width 600
              :height 300
              :encoding {:x {:field x
                             :type "quantitative"}
                         :y {:field y
                             :type "quantitative"}
                         :color {}}}]
    ^:R [:p/vega
         (if color
           (assoc-in spec [:encoding :color] {:field color :type "nominal"})
           (assoc-in spec [:encoding :color] {:value "blue"}))]))

(-> pokemon
    (pt/subset-cols :# :Speed :Defense)
    (pt/melt {:id-vars :#})
    pt/->clj
    (line-plot :# :value :variable))

;;; Let's look at the operation above:

;;; - `subset-cols`: we use this to, well, subset columns. We can choose N columns by label, we will get a 'new' data-frame with only the selected columns
;;; - `melt`: this transforms the data-frame from wide to long format (for more info about it see [further below](#reshape)
;;; - `->clj`: this turns data-frames and serieses to a Clojure vector of maps
;;; `subset-cols` is pretty straightforward:

(-> pokemon (pt/subset-cols :Speed :Attack) pt/head show)

(-> pokemon (pt/subset-cols :Speed :Attack :HP :#) pt/head  show)

(-> pokemon (pt/subset-cols :# :Attack) pt/head)

;;; `->clj` tries to understand what's the better way to transform panthera data structures to Clojure ones



(-> pokemon (pt/subset-cols :Speed) pt/head pt/->clj)

(-> pokemon (pt/subset-cols :Speed :HP) pt/head pt/->clj)

;;; Now we want to see what happens when we plot `Attack` vs `Defense`

(defn scatter
  [data x y & [color]]
  (let [spec {:data {:values data}
              :mark "point"
              :width 600
              :height 300
              :encoding {:x {:field x
                             :type "quantitative"}
                         :y {:field y
                             :type "quantitative"}
                         :color {}}}]
    ^:R [:p/vega
         (if color
           (assoc-in spec [:encoding :color] {:field color :type "nominal"})
           (assoc-in spec [:encoding :color] {:value "dodgerblue"}))]))

(-> pokemon
    (pt/subset-cols :Attack :Defense)
    pt/->clj
    (scatter :attack :defense))

;;; And now the `Speed` histogram

(defn hist
  [data x & [color]]
  (let [spec {:data {:values data}
              :mark "bar"
              :width 600
              :height 300
              :encoding {:x {:field x
                             :bin {:maxbins 50}
                             :type "quantitative"}
                         :y {:aggregate "count"
                             :type "quantitative"}
                         :color {}}}]

    ^:R [:p/vega
         (if color
           (assoc-in spec [:encoding :color] {:field color :type "nominal"})
           (assoc-in spec [:encoding :color] {:value "dodgerblue"}))]))

(-> pokemon
    (pt/subset-cols :Speed)
    pt/->clj
    (hist :speed))

;;; ## Data-frames basics
;;; ### Creation
;;; How to create data-frames? Above we read a csv, but what if we already have some data in the runtime we want to deal with? Nothing easier than this:


(show (pt/data-frame [{:a 1 :b 2} {:a 3 :b 4}]))

;;; What if we don't care about column names, or we'd prefer to add them to an already generated data-frame?

(show (pt/data-frame (to-array-2d [[1 2] [3 4]])))

;;; Columns of data-frames are just serieses:

(-> pokemon (pt/subset-cols "Defense") pt/pytype)


(pt/series [1 2 3])

;;; The column name is the name of the series:



(pt/series [1 2 3] {:name :my-series})

;;; ### Filtering
;;; One of the most straightforward ways to filter data-frames is with booleans. We have `filter-rows` that takes either booleans or a function that generates booleans

(-> pokemon
    (pt/filter-rows #(-> % (pt/subset-cols "Defense") (pt/gt 200)))
    show)

;;; `gt` is exactly what you think it is: `>`. Check the [Basic concepts](https://github.com/alanmarazzi/panthera/blob/master/examples/basic-concepts.ipynb) notebook to better understand how math works in panthera.
;;; Now we'll have to introduce Numpy in the equation. Let's say we want to filter the data-frame based on 2 conditions at the same time, we can do that using `npy`:


(require '[panthera.numpy :refer [npy]])


(defn my-filter
  [col1 col2]
  (npy :logical-and
       {:args [(-> pokemon
                   (pt/subset-cols col1)
                   (pt/gt 200))
               (-> pokemon
                   (pt/subset-cols col2)
                   (pt/gt 100))]}))

(-> pokemon
    (pt/filter-rows (my-filter :Defense :Attack))
    show)

;;; `panthera.numpy` works a little differently than regular panthera, usually you need only `npy` to have access to all of numpy functions.
;;; For instance:

(-> pokemon
    (pt/subset-cols :Defense)
    ((npy :log))
    pt/head)

;;; Above we just calculated the `log` of the whole `Defense` column! Remember that `npy` operations are vectorized, so usually it is faster to use them (or equivalent panthera ones) than Clojure ones (unless you're doing more complicated operations, then Clojure would *probably* be faster).
;;; Now let's try to do some more complicated things:

(/ (pt/sum (pt/subset-cols pokemon :Speed))
   (pt/n-rows pokemon))

;;; Above we see how we can combine operations on serieses, but of course that's a `mean`, and we have a function for that!

(defn col-mean
  [col]
  (pt/mean (pt/subset-cols pokemon col)))

;;; Now we would like to add a new column that says `high` when the value is above the mean, and `low` for the opposite.
;;; `npy` is really helpful here:

(npy :where {:args [(pt/gt (pt/head (pt/subset-cols pokemon :Speed)) (col-mean :Speed))
                    "high"
                    "low"]})

;;; But this is pretty ugly and we can't chain it with other functions. It is pretty easy to wrap it into a chainable function:

(defn where
  [& args]
  (npy :where {:args args}))

(-> pokemon
    (pt/subset-cols :Speed)
    pt/head
    (pt/gt (col-mean :Speed))
    (where "high" "low"))

;;; That seems to work! Let's add a new column to our data-frame:


(def speed-level
  (-> pokemon
      (pt/subset-cols :Speed)
      (pt/gt (col-mean :Speed))
      (where "high" "low")))

(-> pokemon
    (pt/assign {:speed-level speed-level})
    (pt/subset-cols :speed_level :Speed)
    (pt/head 10)
    show)

;;; Of course we didn't actually add `speed_level` to `pokemon`, we created a new data-frame. Everything here is as immutable as possible, let's check if this is really the case:

(vec (pt/names pokemon))

;;; ## Inspecting data
;;; Other than `head` we have `tail`

(show (pt/tail pokemon))

;;; We can always check what's the shape of the data structure we're interested in. `shape` returns rows and columns count

(pt/shape pokemon)

;;; If you want just one of the two you can either use one of `n-rows` or `n-cols`, or get the required value by index:


(pt/n-rows pokemon)

((pt/shape pokemon) 0)


;;; ## Exploratory data analysis
;;; Now we can move to something a little more interesting: some data analysis.
;;; One of the first things we might want to do is to look at some frequencies. `value-counts` is our friend

(-> pokemon
    (pt/subset-cols "Type 1")
    (pt/value-counts {:dropna false}))

;;; As we can see we get counts by group automatically and this can come in handy!
;;; There's also a nice way to see many stats at once for all the numeric columns: `describe`

(show (pt/describe pokemon))

;;; If you need some of these stats only for some columns, chances are that there's a function for that!


(-> (pt/subset-cols pokemon :HP)
    ((juxt pt/mean pt/std pt/minimum pt/maximum)))

;;; <a id='reshape'></a>
;;; ## Reshaping data
;;; Some of the most common operations with rectangular data is to reshape them how we most please to make other operations easier.
;;; The R people perfectly know what I mean when I talk about [tidy data](https://www.jstatsoft.org/article/view/v059i10/v59i10.pdf), if you have no idea about this check the link, but the main point is that while most are used to work with double entry matrices (like the one above built with `describe`), it is much easier to work with *long data*: one row per observation and one column per variable.
;;; In panthera there's `melt` as a workhorse for this process

(-> pokemon pt/head show)

(-> pokemon pt/head (pt/melt {:id-vars "Name" :value-vars ["Attack" "Defense"]}) show)

;;; Above we told panthera that we wanted to `melt` our data-frame and that we would like to have the column `Name` act as the main id, while we're interested in the value of `Attack` and `Defense`.
;;; This makes much easier to group values by some variable:

(-> pokemon
    pt/head
    (pt/melt {:id-vars "Name" :value-vars ["Attack" "Defense"]})
    (pt/groupby :variable)
    pt/mean)

;;; If you've ever used Excel you already know about `pivot`, which is the opposite of `melt`

(-> pokemon
    pt/head
    (pt/melt {:id-vars "Name" :value-vars ["Attack" "Defense"]})
    (pt/pivot {:index "Name" :columns "variable" :values "value"})
    show)

;;; What if we have more than one data-frame? We can combine them however we want!

(show
 (pt/concatenate
  [(pt/head pokemon)
   (pt/tail pokemon)]
  {:axis 0
   :ignore-index true}))

;;; Just a second to discuss some options:
;;; - `:axis`: most of panthera operations can be applied either by rows or columns, we decide which with this keyword where 0 = rows and 1 = columns
;;; - `:ignore-index`: panthera works by index, to better understand what kind of indexes there are and most of their quirks check [Basic concepts](https://nbviewer.jupyter.org/github/alanmarazzi/panthera/blob/master/examples/basic-concepts.ipynb#Indexing-and-subsetting)
;;; To better understand `:axis` let's make another example

(show
 (pt/concatenate
  (repeat 2 (pt/head pokemon))
  {:axis 1}))

;;; ## Types, types everywhere
;;; There are many dedicated types, but no worries, there are nice ways to deal with them.

(pt/dtype pokemon)

;;; I guess there isn't much to say about `:int64` and `:bool`, but surely `:object` looks more interesting. When panthera (numpy included) finds either strings or something it doesn't know how to deal with it goes to the less tight type possible which is an `:object`.
;;; `:object`s are usually bloated, if we want to save some overhead and it makes sense to deal with categorical values we can convert them to `:category`

(-> pokemon
    (pt/subset-cols "Type 1")
    (pt/astype :category)
    pt/head)

(-> pokemon
    (pt/subset-cols "Speed")
    (pt/astype :float)
    pt/head)

;;; ## Dealing with missing data
;;; One of the most painful operations for data scientists and engineers is dealing with the unknown: `NaN` (or `nil`, `Null`, etc).
;;; panthera tries to make this as painless as possible:

(-> pokemon
    (pt/subset-cols "Type 2")
    (pt/value-counts {:dropna false}))

;;; We could check for `NaN` in other ways has well:

(-> pokemon (pt/subset-cols "Type 2") ((juxt pt/hasnans? (comp pt/all? pt/not-na?))))

;;; One of the ways to deal with missing data is to just drop rows

(-> pokemon
    (pt/dropna {:subset ["Type 2"]})
    (pt/subset-cols "Type 2")
    (pt/value-counts {:dropna false}))
;
;;; But let's say we want to replace missing observations with a flag or value of some kind, we can do that easily with `fill-na`

(-> pokemon
    (pt/subset-cols "Type 2")
    (pt/fill-na :empty)
    (pt/head 10))



;;; ## Time and dates
;;; Programmers hate time, that's a fact. Panthera tries to make this experience as painless as possible

(def times
  ["1992-01-10","1992-02-10","1992-03-10","1993-03-15","1993-03-16"])

(pt/->datetime times)

(-> pokemon
    pt/head
    (pt/set-index (pt/->datetime times))
    show)

(-> pokemon
    pt/head
    (pt/set-index (pt/->datetime times))
    (pt/select-rows "1993-03-16" :loc))

(-> pokemon
    pt/head
    (pt/set-index (pt/->datetime times))
    (pt/select-rows (pt/slice "1992-03-10" "1993-03-16") :loc)
    show)
