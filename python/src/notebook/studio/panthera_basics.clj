(ns notebook.studio.panthera-basics
  (:require
   [libpython-clj.require :refer [require-python]]
   [libpython-clj.python :as py :refer [py. py.. py.-]]
   [panthera.panthera :as pt]
   [pinkgorilla.python.core :refer [py-initialize!]]
   [goldly.python2 :refer [with-show]]))

;;; # Basic panthera concepts
;;; 
;;; This is an introductory guide to the concepts driving panthera and its usage.

(defn show
  [obj]
  ^:R [:p/phtml
       (py/call-attr obj "to_html")])


;;; ## Series
;;; *Serieses* are like vectors that act also as columns for *data-frames* (see [Data-frame](#dataframe) section). One *series* must have all the contained data with the same data type and if there is more than one type when you create a *series* than this one takes the most relaxed one.

;;; ### Create

(pt/series [1 2 3])



;;; If we print the *series* we see on the left its index and on the right its values. As you can see below the *series* itself we get the underlying data type (*dtype*) as well. Let's swap 3 with "a" and see what happens.

(pt/series [1 2 "a"])


;;; Now the *dtype* it's become `object`, which in *panthera* means either `string` or something that can be represented with a `string` and is not a primitive.
;;; If we get this data back to Clojure we'll see that we get the underlying original representation with mixed data types.

(vec (pt/series [1 2 "a"]))

;;; This means that we can always move from a representation to another without many problems. A *series* can be treated as a Clojure vector if we want to:

(map inc (pt/series (range 3)))

;;; But when we do this we lose metadata tied to it. The difference with regular vectors is mostly this metadata, a *series* specifically:
;;; - can have a name
;;; - has a *dtype*
;;; - has an index that can be freely named
;;; Let's see a few examples:


(pt/series {:name "my-series"})

;;; We just created an empty *series* with the name "my-series" to show that it can exist even with just metadata. The map passed as an argument lets you add other options to the function call without bothering about their position (in Python there is a clear distinction between *arguments* and *keyword arguments*, [more info](https://treyhunner.com/2018/04/keyword-arguments-in-python/)).
;;; 
;;; We can combine arguments together to get the wanted outcome

(pt/series 1 {:name "my-series" :index ["idx"]})

;;; ### Indexing and subsetting
;;; Now "my-series" has a name, a value and a named index. This distinction is very important in *panthera*: indexing can be done by name and by position.

;; @@ [clj]
(-> (pt/series (range 5) {:name "my-series" :index ["a" "b" "c" "d" "e"]})
    (pt/select-rows [0 3]))

(-> (pt/series (range 5) {:name "my-series" :index ["a" "b" "c" "d" "e"]})
    (pt/select-rows ["a" "d"] :loc))

;;; As you can see above we were able to get the same values from the *series*, but the first time we used pure positional indexing, while the second one we used named indexing.
;;; 
;;; This isn't something logical, it just works like this in pandas. So you'll have to memorize:
;;; 
;;; - `:iloc`: positional indexing
;;; - `:loc`: named indexing or booleans
;;; 
;;; Be aware that the result of this can be this behaviour:

(-> (pt/series (range 5) {:name "my-series" :index (map #(+ 100 %) (range 5))})
    (pt/select-rows [0 3] :iloc))

(-> (pt/series (range 5) {:name "my-series" :index (map #(+ 100 %) (range 5))})
    (pt/select-rows [100 103] :loc))


(-> (pt/series (range 5) {:name "my-series"})
    (pt/select-rows [0 3] :loc))

;;; What happens above is that somewhat unexpectedly we get always the same values. Let's review every cell by itself:
;;; 
;;; - the first time our series can be thought as a map like `{100 0 101 1 102 2 ...}`, but this in panthera doesn't change the fact that the first value is 0, the second is 1 and so on. So by getting `[0 3]` the result is a *series* with the first and fourth values
;;; - the second time we ask for named indices, and this for Clojurians is probably the clearest case: `(select-keys {100 0 101 1 102 2 ...} [100 103])` would give the same result
;;; - the last case is probably the least clear, we ask for named indices (`:loc`), but they are integers and they are positional. This happens because when we don't have named indices both *serieses* and *data-frames* assign a monotonically increasing index that has the value of the index itself as a label. If we had to represent a panthera index in pure Clojure it would be something like `{0 "0" 1 "1" 2 "2" ...}`
;;; 
;;; There's another way to subset by index: slicing

(-> (pt/series (range 10))
    (pt/select-rows (pt/slice 3 6)))


(-> (pt/series (range 5) {:name "my-series" :index ["a" "b" "c" "d" "e"]})
    (pt/select-rows (pt/slice "a" "d") :loc))

;;; ### Math and stats
;;; 
;;; Math is easy with panthera! The only thing to keep in mind is that operations are **vectorized**, so something like `(+ [1 2 3] 1)` would result in `[2 3 4]`.
;;; 
;;; To avoid confusion the panthera operations are named differently than the core functions (`+`, `-`, `*`, etc).

(pt/add (pt/series [1 2 3]) 1)

(pt/pow (pt/series (range 5)) 3)

(pt/add (pt/series [1 2 3]) 1 (pt/series [-1 -2 -3]))

;;; The only note about these operations is that in order to work the first argument has to be a panthera data structure.
;;; 
;;; There are more advanced stats functions besides the more regular ones:

(pt/mean (pt/series (range 10)))
(pt/kurtosis (pt/series (concat (range 10) [100])))
(pt/skew (pt/series (concat (range 10) [100])))


(pt/var (pt/series (concat (range 10) [100])))


(pt/corr (pt/series (range 10)) (pt/series (range 9 0 -1)))

;;; ### Conversions
;;; It might happen that you'd like to work with different data types than the ones inferred by panthera. The advice here is to do this only on the Python side of things.

(pt/->numeric (pt/series ["1" "2"]))
(pt/->datetime "2019-01-01")
(pt/->datetime (pt/series ["2019-01-01" "2019-02-01"]))

;;; Below an example of why you should be careful to deal with different data types in panthera

(-> (pt/series ["2019-01-01" "2019-02-01"])
    pt/->datetime
    pt/->clj)

(-> (pt/series ["2019-01-01" "2019-02-01"])
    pt/->datetime
    pt/->clj
    first
    :unnamed
    type)

;;; The safest way to deal with dates on the Clojure side of things is to convert them to strings

(-> (pt/series ["2019-01-01" "2019-02-01"])
    pt/->datetime
    pt/->clj
    first
    :unnamed
    str)

;;; You can have fun with regular numeric types as well

(pt/astype (pt/series [1 2 3]) :float32)

;;; ### Reshaping
;;; 
;;; There are many facilities to let you *hack 'n' slash* data almost however you want

(pt/cut (pt/series (range 10)) 3)

;;; Intervals aren't handled (yet) on the Clojure side, so keep 'em strictly in Python if you want to deal with them.
;;; 
;;; With `factorize` you can convert values to ints, so basically you get categories.

(pt/factorize (pt/series [:a :b :c]))

;;; With `remap` yu can, well, *remap* your values however you like. Just be aware that you have to pass `remap` every value present in the series in the new encoding, otherwise those not specified will be interpreted as NaNs.


(pt/remap (pt/series [:a :b :c]) {:a "this" :b "that"})

;;; An example on one way to deal with `remap` when you want to remap only some values

(def remapper
  (-> (pt/series [:a :b :c :d :e :f :g :h :i :j])
      pt/unique
      (#(zipmap % %))
      (assoc "e" "only-this-one")))

(pt/remap
 (pt/series [:a :b :c :d :e :f :g :h :i :j])
 remapper)

;;; `rolling` lets you calculate statistics on a rolling window basis

(pt/rolling (pt/series (range 10)) 2)


(-> (pt/series (range 10))
    (pt/rolling 2)
    pt/mean)

;;; ### Missing values
;;; 
;;; Dealing with missing values is what really makes the difference between a full-fledged data analysis framework and much more limited solutions.
;;; 
;;; panthera gives you many options to try easing the pain a bit

(pt/dropna (pt/series [1 2 nil 3]))



;;; Note that though the name might let you think that we're mutating the original series, this is similar to Clojure's `drop`



(def my-srs (pt/series [1 2 nil 3]))

(pt/dropna my-srs)
my-srs


;;; There are various ways to check if your data contains some missing observation. The easiest and fastest one is `hasnans?`.

(pt/hasnans? (pt/series (concat (range 1000) [nil])))

;;; `hasnans?` is a cached value, but this shouldn't be an issue considering that everything is as immutable as possible.
;;; This is another potentially slower way to do the same thing

(pt/all? (pt/not-na? (pt/series (concat (range 1000) [nil]))))

;;; Of course `not-na?` and `all?` have their uses (for instance if you pass the result of `not-na?` to `select-rows` you'll filter NaNs out of the series).
;;; panthera's workhorse to deal with missing observations is `fill-na` which lets you assign a value to NaNs

(pt/fill-na (pt/series [1 2 nil 4]) 3)

;;; <a id='dataframe'></a>
;;; ## Data-frame
;;; A data-frame is basically a collection of serieses as columns. In other words it's a rectangular data structure akin to a matrix, but while the latter is usually only numeric, data-frames can have mixed column types.

;;; ### Create

(pt/data-frame [{:a 1 :b 2} {:a 3 :b 4}])

;;; The easiest way to create a data-frame is with a vector of maps where every map is a row, keys are columns names and values, well, are corresponding values.
;;; 
;;; As we saw earlier data-frames are a collection of serieses, so we can create one starting from a bunch of them.

(pt/data-frame [(pt/series [1 2 3]) (pt/series [4 5 6])])
(pt/data-frame {:a (pt/series [1 2 3]) :b (pt/series [:x :y :z])})
(pt/dtype (pt/data-frame {:a (pt/series [1 2 3]) :b (pt/series [:x :y :z])}))


;;; Above we see that panthera doesn't complain about the column `a` having type `int64` and `b` having type `object` and we can keep working on them as much as we want.
;;; 
;;; ### Indexing and subsetting
;;; 
;;; Now we have two dimensions to work with! No worries, it is always possible to operate on both of them. But first let's check all the metadata available to us

(def df (pt/data-frame [{:a 1 :b 2} {:a 3 :b 4}]))
(pt/index df)
(pt/names df)

;;; So, what we saw for serieses works for data-frames as well

(def df (pt/data-frame (map #(zipmap [:a :b :c] %) (partition 3 (range 30)))))
(pt/select-rows df [0 5])
(pt/select-rows df (pt/slice 2 5))


;;; The new thing is subsetting columns, we can do this by name with `subset-cols`. You can select any number of columns in this way, as long as they are in the given data-frame

(pt/subset-cols df :a :c)


;;; ### Math and stats
(pt/mean df)

