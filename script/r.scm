

(define-public r-packages
  ; possibly add this channel too ?
  ; https://github.com/UMCUGenetics/guix-additions/blob/master/umcu/packages/rstudio.scm
  ; r repos: Bioconductor + Cran
  (list ;; for RMarkdown
   "r"
   "r-rserve" ; tcp server adaptor
    ; "rstudio" ; channel guix-science
   ;"r-knitr"
   ;"r-yaml"
   ;"r-markdown"
   ;"r-rmarkdown"
   ;"texlive"
   ;; commonly used r packages
   ;"r-psych"

   ; clojisr goldly-example-datascience
   "r-ggplot2"
   "r-dt"
   "r-svglite"
   "r-plotly"

   "r-lattice"
   "r-foreign"
   "r-readr"
   ;"r-haven"
   "r-dplyr"
   "r-tidyr"
   ;"r-stringr"
   ;"r-forecast"
   ;"r-lme4"
   ;"r-nlme"
   ;"r-nnet"
   ;"r-glmnet"
   ;"r-caret"
   ;"r-xmisc"
   ;"r-splitstackshape"
   ;"r-tm"
   ;"r-quanteda"
   ;"r-topicmodels"
   ;"r-stm"
   ;;"r-parallel"
   ;"r-nlp"
   ))

(specifications->manifest r-packages)
