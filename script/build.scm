


(define-public build-packages
  (list 
    "coreutils" ; glibcc gcc bash 
    ;"direnv" google this
      
    ;"idutils"
    ; "guile" 
    "hello"
    ;"nss-certs" ; tls certificates
       
    "make"
    "gcc-toolchain"
    ; "llvm" "clang" "clang-runtime" "clang-toolchain"
    ; "glibc" "gdb" "gcc-toolchain"
    ; "gnu-make" "cmake" "pkg-config"
    ; "patchelf" "binutils" "elfutils"
  ))


; another approach
; this is perhaps better, as I dont need manifest+os
; guix environment -l guix-requirements.scm.
;https://unix.stackexchange.com/questions/621269/use-a-python-projects-requirements-txt-as-input-to-a-guix-package-definition?rq=1

; or this: package reprocessing
; https://github.com/alezost/guix-config/blob/master/modules/al/guix/utils.scm
