(ns studio.notebook.bad.ds-ames
  (:require
   [clojure.pprint :as pp]
   [clojure.set :as c-set]
  ; [clojure.core.matrix :as m]
  
  ; [tech.ml.dataset.pipeline.base
  ;  :refer [col]   ;;We use col a lot, and int map is similar
  ;  :as dsp]
  ; [tech.ml.dataset.pipeline.column-filters :as cf]
   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset :as ds]
   [tech.v3.dataset.column :as ds-col]
   ;[tech.ml :as ml]
  ; [tech.ml.loss :as loss]
   ;[tech.v3.utils :as ml-utils]
  ; [tech.ml.regression :as ml-regression]
   ;;use tablesaw as dataset backing store
   ;[tech.libs.tablesaw :as tablesaw]
   ;;model generators
   ;[tech.libs.xgboost]
  ; [tech.libs.smile.regression]
   ; [tech.libs.smile.utils :as smile-utils]
   [tech.v3.io :as io] ;;put/get nippy
   ))

;;; # Getting Started
;;; This is a tutorial for a a couple of new Clojure libraries for Machine Learning and ETL -- part of the tech.ml stack.
;;; Author: Chris Nuernberger
;;; Translated to [Nextjournal](https://nextjournal.com/alan/tech-dataset-getting-started): Alan Marazzi
;;; The API is still alpha, we are putting our efforts into extending and beautifying it. Comments will be welcome!"
;;; Reading from an excellent article on [advanced regression techniques](https://www.kaggle.com/juliencs/a-study-on-regression-applied-to-the-ames-dataset).  
;;; The target is to predict the SalePrice column.

(import '[java.io File])

(defn pp-str
  [ds]
  (with-out-str
    (pp/pprint ds)))


(defn print-table
  ([ks data]
   (->> data
        (map (fn [item-map]
               (->> item-map
                    (map (fn [[k v]]
                           [k (if (or (float? v)
                                      (double? v))
                                (format "%.3f" v)
                                v)]))
                    (into {}))))
        (pp/print-table ks)))
  ([data]
   (print-table (sort (keys (first data))) data)))

(def src-dataset (ds/->dataset "resources/sample-data/ames/train.csv"))
(dtype/shape src-dataset)


(ds/descriptive-stats src-dataset)

;;; The shape is backward as compared to pandas.  This is by intention; core.matrix is a row-major linear algebra system.  tech.ml.dataset is column-major.  Thus, to ensure sanity when doing conversions we represent the data in a normal shape.  Note that pandas returns [1460 81].
;;; ## Outliers
;;; We first check for outliers, graph and then remove them.

^:R ['ui.vega/vega {:data {:values
                     (-> src-dataset
                         (ds/select-columns ["SalePrice" "GrLivArea"])
                         (ds/->flyweight))}
              :mark :point
              :encoding {:y {:field "SalePrice"
                             :type :quantitative}
                         :x {:field "GrLivArea"
                             :type :quantitative}}}]

(def filtered-ds (dsp/filter src-dataset "GrLivArea" #(dfn/< (dsp/col) 4000)))
^:R ['ui.vega/vega {:data {:values
                     (-> filtered-ds
                         (ds/select-columns ["SalePrice" "GrLivArea"])
                         (ds/->flyweight))}
              :mark :point
              :encoding {:y {:field "SalePrice"
                             :type :quantitative}
                         :x {:field "GrLivArea"
                             :type :quantitative}}}]

;;; ## Initial Pipeline
;;; We now begin to construct our data processing pipeline.  Note that all pipeline operations are available as repl functions from the pipeline namespace.

(defn initial-pipeline-from-article
  [dataset]
  (-> dataset
      ;;Convert any numeric or boolean columns to be all of one datatype.
      (dsp/remove-columns ["Id"])
      (dsp/->datatype)
      (dsp/m= "SalePrice" #(dfn/log1p (dsp/col)))
      (ds/set-inference-target "SalePrice")))

;;; ## Categorical Fixes
;;; Whether columns are categorical or not is defined by attributes.

(defn more-categorical
  [dataset]
  (dsp/assoc-metadata dataset ["MSSubClass" "OverallQual" "OverallCond"] :categorical? true))

(println "pre-categorical-count" (count (cf/categorical? filtered-ds)))

(def post-categorical-fix (-> filtered-ds
                              initial-pipeline-from-article
                              more-categorical))

(println "post-categorical-count" (count (cf/categorical? post-categorical-fix)))

;;; ## Missing Entries
;;; Missing data is a theme that will come up again and again.  Pandas has great tooling to clean up missing entries and we borrow heavily from them.
;; Impressive patience to come up with this list!!

(defn initial-missing-entries
  [dataset]
  (-> dataset
      ;; Handle missing values for features where median/mean or most common value doesn't
      ;; make sense

      ;; Alley : data description says NA means "no alley access"
      (dsp/replace-missing "Alley" "None")
      ;; BedroomAbvGr : NA most likely means 0
      (dsp/replace-missing ["BedroomAbvGr"
                            "BsmtFullBath"
                            "BsmtHalfBath"
                            "BsmtUnfSF"
                            "EnclosedPorch"
                            "Fireplaces"
                            "GarageArea"
                            "GarageCars"
                            "HalfBath"
                            ;; KitchenAbvGr : NA most likely means 0
                            "KitchenAbvGr"
                            "LotFrontage"
                            "MasVnrArea"
                            "MiscVal"
                            ;; OpenPorchSF : NA most likely means no open porch
                            "OpenPorchSF"
                            "PoolArea"
                            ;; ScreenPorch : NA most likely means no screen porch
                            "ScreenPorch"
                            ;; TotRmsAbvGrd : NA most likely means 0
                            "TotRmsAbvGrd"
                            ;; WoodDeckSF : NA most likely means no wood deck
                            "WoodDeckSF"]
                           0)
      ;; BsmtQual etc : data description says NA for basement features is "no basement"
      (dsp/replace-missing ["BsmtQual"
                            "BsmtCond"
                            "BsmtExposure"
                            "BsmtFinType1"
                            "BsmtFinType2"
                            ;; Fence : data description says NA means "no fence"
                            "Fence"
                            ;; FireplaceQu : data description says NA means "no
                            ;; fireplace"

                            "FireplaceQu"
                            ;; GarageType etc : data description says NA for garage
                            ;; features is "no garage"
                            "GarageType"
                            "GarageFinish"
                            "GarageQual"
                            "GarageCond"
                            ;; MiscFeature : data description says NA means "no misc
                            ;; feature"
                            "MiscFeature"
                            ;; PoolQC : data description says NA means "no pool"
                            "PoolQC"]
                           "No")
      (dsp/replace-missing "CentralAir" "N")
      (dsp/replace-missing ["Condition1"
                            "Condition2"]
                           "Norm")
      ;; Condition : NA most likely means Normal
      ;; EnclosedPorch : NA most likely means no enclosed porch
      ;; External stuff : NA most likely means average
      (dsp/replace-missing ["ExterCond"
                            "ExterQual"
                            ;; HeatingQC : NA most likely means typical
                            "HeatingQC"
                            ;; KitchenQual : NA most likely means typical
                            "KitchenQual"]
                           "TA")
      ;; Functional : data description says NA means typical
      (dsp/replace-missing "Functional" "Typ")
      ;; LotShape : NA most likely means regular
      (dsp/replace-missing "LotShape" "Reg")
      ;; MasVnrType : NA most likely means no veneer
      (dsp/replace-missing "MasVnrType" "None")
      ;; PavedDrive : NA most likely means not paved
      (dsp/replace-missing "PavedDrive" "N")
      (dsp/replace-missing "SaleCondition" "Normal")
      (dsp/replace-missing "Utilities" "AllPub")))

(println "pre missing fix #1")
(pp/pprint (ds/columns-with-missing-seq post-categorical-fix))

(def post-missing (initial-missing-entries post-categorical-fix))
(println "post missing fix #1")
(pp/pprint (ds/columns-with-missing-seq post-missing))

;;; ## String->Number
;;; We need to convert string data into numbers somehow.  One method is to build a lookup table such that 1 string column gets converted into 1 numeric column.  The exact encoding of these strings can be very important to communicate semantic information from the dataset to the ml system.  We remember all these mappings because we have to use them later.  They get stored both in the recorded pipeline and in the options map so we can reverse-map label values back into their categorical initial values.

(def str->number-initial-map
  {"Alley"  {"Grvl"  1 "Pave" 2 "None" 0}
   "BsmtCond"  {"No"  0 "Po"  1 "Fa"  2 "TA"  3 "Gd"  4 "Ex"  5}
   ;"BsmtExposure"  {"No"  0 "Mn"  1 "Av" 2 "Gd"  3}
   "BsmtFinType1"  {"No"  0 "Unf"  1 "LwQ" 2 "Rec"  3 "BLQ"  4
                    "ALQ"  5 "GLQ"  6}
   "BsmtFinType2"  {"No"  0 "Unf"  1 "LwQ" 2 "Rec"  3 "BLQ"  4
                    "ALQ"  5 "GLQ"  6}
   "BsmtQual"  {"No"  0 "Po"  1 "Fa"  2 "TA" 3 "Gd"  4 "Ex"  5}
   "ExterCond"  {"Po"  1 "Fa"  2 "TA" 3 "Gd" 4 "Ex"  5}
   "ExterQual"  {"Po"  1 "Fa"  2 "TA" 3 "Gd" 4 "Ex"  5}
   "FireplaceQu"  {"No"  0 "Po"  1 "Fa"  2 "TA"  3 "Gd"  4 "Ex"  5}
   "Functional"  {"Sal"  1 "Sev"  2 "Maj2"  3 "Maj1"  4 "Mod" 5
                  "Min2"  6 "Min1"  7 "Typ"  8}
   "GarageCond"  {"No"  0 "Po"  1 "Fa"  2 "TA"  3 "Gd"  4 "Ex"  5}
   "GarageQual"  {"No"  0 "Po"  1 "Fa"  2 "TA"  3 "Gd"  4 "Ex"  5}
   "HeatingQC"  {"Po"  1 "Fa"  2 "TA"  3 "Gd"  4 "Ex"  5}
   "KitchenQual"  {"Po"  1 "Fa"  2 "TA"  3 "Gd"  4 "Ex"  5}
   "LandSlope"  {"Sev"  1 "Mod"  2 "Gtl"  3}
   "LotShape"  {"IR3"  1 "IR2"  2 "IR1"  3 "Reg"  4}
   ;"PavedDrive"  {"N"  0 "P"  1 "Y"  2}
   "PoolQC"  {"No"  0 "Fa"  1 "TA"  2 "Gd"  3 "Ex"  4}
   "Street"  {"Grvl"  1 "Pave"  2}
   "Utilities"  {"ELO"  1 "NoSeWa"  2 "NoSewr"  3 "AllPub"  4}})


(defn str->number-pipeline
  [dataset]
  (->> str->number-initial-map
       (reduce (fn [dataset str-num-entry]
                 (apply dsp/string->number dataset str-num-entry))
               dataset)))

(def str-num-dataset (str->number-pipeline post-missing))

(ds/dataset-label-map str-num-dataset)

;;; ## Replacing values
;;; There is a numeric operator that allows you to map values from one value to another in a column.  We now use this to provide simplified versions of some of the columns.

(def replace-maps
  {;; Create new features
   ;; 1* Simplifications of existing features
   ;; The author implicitly leaves values at zero to be zero, so these maps
   ;; are intentionally incomplete
   "SimplOverallQual" {"OverallQual" {1  1, 2  1, 3  1, ;; bad
                                      4  2, 5  2, 6  2, ;; average
                                      7  3, 8  3, 9  3, 10  3 ;; good
                                      }}
   "SimplOverallCond" {"OverallCond" {1  1, 2  1, 3  1,       ;; bad
                                      4  2, 5  2, 6  2,       ;; average
                                      7  3, 8  3, 9  3, 10  3 ;; good
                                      }}
   "SimplPoolQC" {"PoolQC" {1  1, 2  1,    ;; average
                            3  2, 4  2     ;; good
                            }}
   "SimplGarageCond" {"GarageCond" {1  1,             ;; bad
                                    2  1, 3  1,       ;; average
                                    4  2, 5  2        ;; good
                                    }}
   "SimplGarageQual" {"GarageQual" {1  1,             ;; bad
                                    2  1, 3  1,       ;; average
                                    4  2, 5  2        ;; good
                                    }}
   "SimplFireplaceQu"  {"FireplaceQu" {1  1,           ;; bad
                                       2  1, 3  1,     ;; average
                                       4  2, 5  2      ;; good
                                       }}
   "SimplFunctional"  {"Functional" {1  1, 2  1,           ;; bad
                                     3  2, 4  2,           ;; major
                                     5  3, 6  3, 7  3,     ;; minor
                                     8  4                  ;; typical
                                     }}
   "SimplKitchenQual" {"KitchenQual" {1  1,             ;; bad
                                      2  1, 3  1,       ;; average
                                      4  2, 5  2        ;; good
                                      }}
   "SimplHeatingQC"  {"HeatingQC" {1  1,           ;; bad
                                   2  1, 3  1,     ;; average
                                   4  2, 5  2      ;; good
                                   }}
   "SimplBsmtFinType1"  {"BsmtFinType1" {1  1,         ;; unfinished
                                         2  1, 3  1,   ;; rec room
                                         4  2, 5  2, 6  2 ;; living quarters
                                         }}
   "SimplBsmtFinType2" {"BsmtFinType2" {1 1,           ;; unfinished
                                        2 1, 3 1,      ;; rec room
                                        4 2, 5 2, 6 2  ;; living quarters
                                        }}
   "SimplBsmtCond" {"BsmtCond" {1 1,    ;; bad
                                2 1, 3 1, ;; average
                                4 2, 5 2  ;; good
                                }}
   "SimplBsmtQual" {"BsmtQual" {1 1,      ;; bad
                                2 1, 3 1, ;; average
                                4 2, 5 2  ;; good
                                }}
   "SimplExterCond" {"ExterCond" {1 1,      ;; bad
                                  2 1, 3 1, ;; average
                                  4 2, 5 2  ;; good
                                  }}
   "SimplExterQual" {"ExterQual" {1 1,      ;; bad
                                  2 1, 3 1, ;; average
                                  4 2, 5 2  ;; good
                                  }}})


(defn simplifications
  [dataset]
  (->> replace-maps
       (reduce (fn [dataset [target-name coldata-map]]
                 (let [[col-name replace-data] (first coldata-map)]
                   (dsp/m= dataset target-name
                           #(dsp/int-map replace-data (dsp/col col-name)
                                         :not-strict? true))))
               dataset)))

(def replace-dataset (simplifications str-num-dataset))

(pp/pprint (-> (ds/column str-num-dataset "KitchenQual")
               (ds-col/unique)))

(pp/pprint (-> (ds/column replace-dataset "SimplKitchenQual")
               (ds-col/unique)))

;;; ## Linear Combinations
;;; We create a set of simple linear combinations that derive from our semantic understanding of the dataset.

(defn linear-combinations
  [dataset]
  (-> dataset
      (dsp/m= "OverallGrade" #(dfn/* (col "OverallQual") (col "OverallCond")))
      ;; Overall quality of the garage
      (dsp/m= "GarageGrade" #(dfn/* (col "GarageQual") (col "GarageCond")))
      ;; Overall quality of the exterior
      (dsp/m= "ExterGrade" #(dfn/* (col "ExterQual") (col "ExterCond")))
      ;; Overall kitchen score
      (dsp/m= "KitchenScore" #(dfn/* (col "KitchenAbvGr") (col "KitchenQual")))
      ;; Overall fireplace score
      (dsp/m= "FireplaceScore" #(dfn/* (col "Fireplaces") (col "FireplaceQu")))
      ;; Overall garage score
      (dsp/m= "GarageScore" #(dfn/* (col "GarageArea") (col "GarageQual")))
      ;; Overall pool score
      (dsp/m= "PoolScore" #(dfn/* (col "PoolArea") (col "PoolQC")))
      ;; Simplified overall quality of the house
      (dsp/m= "SimplOverallGrade" #(dfn/* (col "SimplOverallQual")
                                          (col "SimplOverallCond")))
      ;; Simplified overall quality of the exterior
      (dsp/m= "SimplExterGrade" #(dfn/* (col "SimplExterQual") (col "SimplExterCond")))
      ;; Simplified overall pool score
      (dsp/m= "SimplPoolScore" #(dfn/* (col "PoolArea") (col "SimplPoolQC")))
      ;; Simplified overall garage score
      (dsp/m= "SimplGarageScore" #(dfn/* (col "GarageArea") (col "SimplGarageQual")))
      ;; Simplified overall fireplace score
      (dsp/m= "SimplFireplaceScore" #(dfn/* (col "Fireplaces") (col "SimplFireplaceQu")))
      ;; Simplified overall kitchen score
      (dsp/m= "SimplKitchenScore" #(dfn/* (col "KitchenAbvGr")
                                          (col "SimplKitchenQual")))
      ;; Total number of bathrooms
      (dsp/m= "TotalBath" #(dfn/+ (col "BsmtFullBath") (dfn/* 0.5 (col "BsmtHalfBath"))
                                  (col "FullBath") (dfn/* 0.5 (col "HalfBath"))))
      ;; Total SF for house (incl. basement)
      (dsp/m= "AllSF"  #(dfn/+ (col "GrLivArea") (col "TotalBsmtSF")))
      ;; Total SF for 1st + 2nd floors
      (dsp/m= "AllFlrsSF" #(dfn/+ (col "1stFlrSF") (col "2ndFlrSF")))
      ;; Total SF for porch
      (dsp/m= "AllPorchSF" #(dfn/+ (col "OpenPorchSF") (col "EnclosedPorch")
                                   (col "3SsnPorch") (col "ScreenPorch")))
      ;; Encode MasVrnType
      (dsp/string->number "MasVnrType" ["None" "BrkCmn" "BrkFace" "CBlock" "Stone"])
      (dsp/m= "HasMasVnr" #(dfn/not-eq (col "MasVnrType") 0))))


(def linear-combined-ds (linear-combinations replace-dataset))



(let [print-columns ["TotalBath" "BsmtFullBath" "BsmtHalfBath"
                     "FullBath" "HalfBath"]]
  (println (ds/select linear-combined-ds print-columns (range 10))))

(let [print-columns ["AllSF" "GrLivArea" "TotalBsmtSF"]]
  (println (ds/select linear-combined-ds print-columns (range 10))))


;;; ## Correlation Table
;;; Let's check the correlations between the various columns and the target column (SalePrice).  

(def article-correlations
  ;;Default for pandas is pearson.
  ;;  Find most important features relative to target
  (->> {"SalePrice"            1.000
        "OverallQual"          0.819
        "AllSF"                0.817
        "AllFlrsSF"            0.729
        "GrLivArea"            0.719
        "SimplOverallQual"     0.708
        "ExterQual"            0.681
        "GarageCars"           0.680
        "TotalBath"            0.673
        "KitchenQual"          0.667
        "GarageScore"          0.657
        "GarageArea"           0.655
        "TotalBsmtSF"          0.642
        "SimplExterQual"       0.636
        "SimplGarageScore"     0.631
        "BsmtQual"             0.615
        "1stFlrSF"             0.614
        "SimplKitchenQual"     0.610
        "OverallGrade"         0.604
        "SimplBsmtQual"        0.594
        "FullBath"             0.591
        "YearBuilt"            0.589
        "ExterGrade"           0.587
        "YearRemodAdd"         0.569
        "FireplaceQu"          0.547
        "GarageYrBlt"          0.544
        "TotRmsAbvGrd"         0.533
        "SimplOverallGrade"    0.527
        "SimplKitchenScore"    0.523
        "FireplaceScore"       0.518
        "SimplBsmtCond"        0.204
        "BedroomAbvGr"         0.204
        "AllPorchSF"           0.199
        "LotFrontage"          0.174
        "SimplFunctional"      0.137
        "Functional"           0.136
        "ScreenPorch"          0.124
        "SimplBsmtFinType2"    0.105
        "Street"               0.058
        "3SsnPorch"            0.056
        "ExterCond"            0.051
        "PoolArea"             0.041
        "SimplPoolScore"       0.040
        "SimplPoolQC"          0.040
        "PoolScore"            0.040
        "PoolQC"               0.038
        "BsmtFinType2"         0.016
        "Utilities"            0.013
        "BsmtFinSF2"           0.006
        "BsmtHalfBath"        -0.015
        "MiscVal"             -0.020
        "SimplOverallCond"    -0.028
        "YrSold"              -0.034
        "OverallCond"         -0.037
        "LowQualFinSF"        -0.038
        "LandSlope"           -0.040
        "SimplExterCond"      -0.042
        "KitchenAbvGr"        -0.148
        "EnclosedPorch"       -0.149
        "LotShape"            -0.286}
       (sort-by second >)))

(def tech-ml-correlations
  (get
   (ds/correlation-table linear-combined-ds); :pearson)
   "SalePrice"))

(pp/print-table (map #(zipmap [:pandas :tech.ml.dataset]
                              [%1 %2])
                     (take 20 article-correlations)
                     (take 20 tech-ml-correlations)))

;;; ## Polynomial Combinations
;;; We now extend the power of our linear models to be effectively polynomial models for a subset of the columns.  We do this using the correlation table to indicate which columns are worth it (the author used the top 10).

(defn polynomial-combinations
  [dataset correlation-table]
  (let [correlation-colnames (->> correlation-table
                                  (drop 1)
                                  (take 10)
                                  (map first))]
    (->> correlation-colnames
         (reduce (fn [dataset colname]
                   (-> dataset
                       (dsp/m= (str colname "-s2") #(dfn/pow (col colname) 2))
                       (dsp/m= (str colname "-s3") #(dfn/pow (col colname) 3))
                       (dsp/m= (str colname "-sqrt") #(dfn/sqrt (col colname)))))
                 dataset))))

(def poly-data (-> (polynomial-combinations linear-combined-ds tech-ml-correlations)
                   dsp/string->number))


(println (ds/select poly-data
                    ["OverallQual"
                     "OverallQual-s2"
                     "OverallQual-s3"
                     "OverallQual-sqrt"]
                    (range 10)))

;;; ## Numeric Vs. Categorical
;;; The article considers anything non-numeric to be categorical.  This is a point on which the tech.ml.dataset system differs.  For tech, any column can be considered categorical and the underlying datatype does not change this definition.  Earlier the article converted numeric columns to string to indicate they are categorical but we just set metadata.
;;; This, and parsing difference between tablesaw and pandas, lead to different outcomes in the next section.

(def numerical-features (cf/numeric-and-non-categorical-and-not-target poly-data))
(def categorical-features (dsp/with-ds poly-data
                            (cf/and #(cf/not cf/target?)
                                    #(cf/not numerical-features))))


;(println "numeric-features" (count numerical-features))
;(println "categorical-features" (count categorical-features))
;(println "inference targets" (cf/target? poly-data))

;;I printed out the categorical features from the when using pandas.
#_(pp/pprint (->> (c-set/difference
                   (set ["MSSubClass", "MSZoning", "Alley", "LandContour", "LotConfig",
                         "Neighborhood", "Condition1", "Condition2", "BldgType",
                         "HouseStyle", "RoofStyle", "RoofMatl", "Exterior1st",
                         "Exterior2nd", "MasVnrType", "Foundation", "Heating",
                         "CentralAir",
                         "Electrical", "GarageType", "GarageFinish", "Fence",
                         "MiscFeature",
                         "MoSold", "SaleType", "SaleCondition"])
                   (set categorical-features))
                  (map (comp ds-col/metadata (partial ds/column poly-data)))))

(defn fix-all-missing
  [dataset]
  (-> dataset
      ;;Fix any remaining numeric columns by using the median.
      (dsp/replace-missing cf/numeric? #(dfn/median (col)))
      ;;Fix any string columns by using 'NA'.
      (dsp/replace-missing cf/string? "NA")
      (dsp/string->number)))


(def missing-fixed (fix-all-missing poly-data))

(pp/pprint (ds/columns-with-missing-seq missing-fixed))

;;; ## Training And Viewing Results
;;; Let's setup a simple gridsearch and few the errors and residuals.

#_(defn render-results
    [title gridsearch-results]
    [:div
     [:h3 title]
     (vega-viz/accuracy-graph gridsearch-results :y-scale [0.10, 0.20])])


(defn train-regressors
  [dataset-name dataset loss-fn & [options]]
  (let [base-gridsearch-systems [:smile.regression/lasso
                                 :xgboost/regression
                                 :smile.regression/ridge]
        trained-results (ml-regression/train-regressors
                         dataset options
                         :loss-fn loss-fn
                         :gridsearch-regression-systems base-gridsearch-systems)]
    (println "Got" (count trained-results) "Trained results")
    (vec trained-results)))


(defn train-graph-regressors
  [dataset-name dataset loss-fn & [options]]
  (let [trained-results (train-regressors dataset-name dataset loss-fn options)]
    (->> (apply concat [(render-results dataset-name trained-results)]
                (->> trained-results
                     (sort-by :average-loss)
                     (map (fn [model-result]
                            [[:div
                              [:h3 (format "%s-%.4f"
                                           (get-in model-result [:options :model-type])
                                           (:average-loss model-result))]
                              [:div
                               [:span
                                [:h4 "Predictions"]
                                (vega-viz/graph-regression-verification-results
                                 model-result :target-key :predictions
                                 :y-scale [10 14]
                                 :x-scale [10 14])]
                               [:span
                                [:h4 "Residuals"]
                                (vega-viz/graph-regression-verification-results
                                 model-result :target-key :residuals
                                 :y-scale [10 14]
                                 :x-scale [-1 1])]]]]))))
         (into [:div]))))

;(oz/view! (train-graph-regressors "Missing Fixed" missing-fixed loss/rmse))

;;; ## Skew
;;; Here is where things go a bit awry.  We attempt to fix skew.  The attempted fix barely reduces the actual skew in the dataset.  We will talk about what went wrong.  We also begin running models on the stages to see what the effect of some of these things are.
;;; We start setting the target in the options for the pipeline.  This allows the rest of the system downstream (training) to automatically infer the feature columns.

(defn skew-column-filter
  [& [dataset]]
  (dsp/with-ds (cf/check-dataset dataset)
    (cf/and cf/numeric?
            #(cf/not "SalePrice")
            #(cf/not cf/categorical?)
            (fn []
              (cf/> #(dfn/abs (dfn/skewness (col)))
                    0.5)))))

#_(def skew-fixed (-> (dsp/m= missing-fixed
                              skew-column-filter
                              #(dfn/log1p (col)))))

;(println "Pre-fix skew counts" (count (skew-column-filter missing-fixed)))
;(println "Post-fix skew counts" (count (skew-column-filter skew-fixed)))

;;; That didn't work.  Or at least it barely did.  What happened??
;; I apologize for the formatting.  This is a poor replacement for emacs with paredit

(let [before-columns (set (skew-column-filter missing-fixed))
      after-columns (set (skew-column-filter skew-fixed))
      check-columns (c-set/intersection before-columns after-columns)]
  (->> check-columns
       (map (fn [colname]
              (let [{before-min :min
                     before-max :max
                     before-mean :mean
                     before-skew :skew}
                    (-> (ds/column missing-fixed colname)
                        (ds-col/stats [:min :max :mean :skew]))
                    {after-min :min
                     after-max :max
                     after-mean :mean
                     after-skew :skew}
                    (-> (ds/column skew-fixed colname)
                        (ds-col/stats [:min :max :mean :skew]))]
                {:column-name colname
                 :before-skew before-skew
                 :after-skew after-skew
                 :before-mean before-mean
                 :after-mean after-mean})))
       (print-table [:column-name
                     :before-skew :after-skew
                     :before-mean :after-mean])))

;;; Maybe you can see the issue now.  For positive skew and  and small means, the log1p fix has very little effect.  For very large numbers, it may skew the result all the way to be negative.  And then for negative skew, it makes it worse.
;;; No easy fixes here today, but a combined method attempting several versions of the skew fix and including the best one could eventually figure it all out in an automated way.
;;; In any case, let's see some actual results:

(oz/view! (train-graph-regressors "Skew Fixed" skew-fixed loss/rmse))

;;; ## std-scaler
;;; There are two scale methods so far in the tech.ml.dataset system.  
;;; * **range-scaler** - scale column such that min/max equal a range min/max.  Range defaults to [-1 1].
;;; * **std-scaler** - scale column such that mean = 0 and variance,stddev = 1.

(def poly-std-scale-ds (dsp/std-scale missing-fixed))

(def std-scale-ds (dsp/std-scale skew-fixed))

(println "Before std-scaler")

(->> (ds/select skew-fixed (take 10 numerical-features) :all)
     (ds/columns)
     (map (fn [col]
            (merge (ds-col/stats col [:mean :variance])
                   {:column-name (ds-col/column-name col)})))
     (print-table [:column-name :mean :variance]))

(println "\n\nAfter std-scaler")

(->> (ds/select std-scale-ds (take 10 numerical-features) :all)
     (ds/columns)
     (map (fn [col]
            (merge (ds-col/stats col [:mean :variance])
                   {:column-name  (ds-col/column-name col)})))
     (pp/print-table [:column-name :mean :variance]))


;;; Before std-scaler
;;; After std-scaler
;;; ## Final Models

;;; We now train our prepared data across a range of models.

(oz/view! [:div (train-graph-regressors "Final Result-No Skew Fix" poly-std-scale-ds loss/rmse)
           (train-graph-regressors "Final Result-Skew Fix" skew-fixed loss/rmse)])

;;; ## Going To Production
;;; You won't see this ever talked about in notebooks and that is unfair to the rest of the organization but you have to take everything above and go to production with it at some point.
;;; Without getting into too much detail, we show how to build a production pipeline using the tech system.  In essence, you can capture context during the training dataset processing and then use this context to make building the inference
;;; pipeline just a bit easier.
;;; There is quite a bit of ephemeral data used during the above dataset processing.  Sometimes we do a string->number 
;;; conversion and we don't specify precisely how to map the values.  We had std-scaler which recorded means and variances for all of the systems.  We had a correlation table that we referenced to build out column augmentations.
;;; We can't make going to production automatic, but we can do at least a bit in this area.

(defn data-pipeline
  "Now you have a model and you want to go to production."
  [dataset training?]
  (let [sale-price-col (when training?
                         (dsp/without-recording
                          (-> dataset
                              ;;Sale price is originally an integer
                              (dsp/m= "SalePrice" #(-> (dsp/col)
                                                       (dtype/->reader :float64)
                                                       dfn/log1p))
                              (ds/column "SalePrice"))))

        dataset (if training?
                  (ds/remove-columns dataset ["SalePrice"])
                  dataset)
        dataset
        (-> dataset
            (dsp/remove-columns ["Id"])
            (dsp/->datatype)
            more-categorical
            initial-missing-entries
            str->number-pipeline
            simplifications
            linear-combinations
            (dsp/store-variables #(hash-map :correlation-table
                                            (-> (ds/add-column % sale-price-col)
                                                (ds/correlation-table :pearson)
                                                (get "SalePrice"))))
            (polynomial-combinations (dsp/read-var :correlation-table))
            fix-all-missing
            dsp/std-scale)]
    (if training?
      (-> (ds/add-column dataset sale-price-col)
          (ds/set-inference-target "SalePrice"))
      dataset)))



(def inference-pipeline-data (dsp/pipeline-train-context
                              (data-pipeline src-dataset true)))

(def pipeline-train-dataset (:dataset inference-pipeline-data))


(def inference-pipeline-context (:context inference-pipeline-data))


;;At inference time we wouldn't have the saleprice column
(def test-inference-src-dataset (dsp/remove-columns src-dataset ["SalePrice"]))


;;Now we can build the same dataset easily using context built during
;;the training system.  This means any string tables generated or any range
;;k-means, stdscale, etc are all in the context.

(def pipeline-inference-dataset (:dataset
                                 (dsp/pipeline-inference-context
                                  inference-pipeline-context
                                  (data-pipeline test-inference-src-dataset false))))


(println (ds/select pipeline-train-dataset ["OverallQual"
                                            "OverallQual-s2"
                                            "OverallQual-s3"
                                            "OverallQual-sqrt"]
                    (range 10)))


(println (ds/select pipeline-inference-dataset ["OverallQual"
                                                "OverallQual-s2"
                                                "OverallQual-s3"
                                                "OverallQual-sqrt"]
                    (range 10)))
