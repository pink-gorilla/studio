


(define-public python-packages
(list 
  ; possibly add this channel too ?
  ; https://github.com/UMCUGenetics/guix-additions/blob/master/umcu/packages/python.scm
  ; https://unix.stackexchange.com/questions/621269/use-a-python-projects-requirements-txt-as-input-to-a-guix-package-definition?rq=1
  "python"  
  ;"python-2"
  ;   "python-wrapper"
  ; "python-cython"
  ;"python-future"
  ;"python-h5py" 
  ;"python-mappy"
  "python-numpy" 
  "python-scipy" 
  "python-pandas"
  ;"python-setuptools
  "python-yarl" ; for edgar
  "python-certifi"
  "python-tqdm"
  "python-lxml"
  "python-soupsieve"
  "python-urllib3"
  "python-beautifulsoup4"
  "python-async-timeout"
  "python-chardet"
  "python-requests" 
  "python-typing-extensions"
  "python-attrs"
  "python-aiohttp"

  "python-yubikey-manager"
  ;"rofi-pass"
  ;"rofi"
  ;"python-yubikey-oath-dmenu"
 ))


(specifications->manifest python-packages)