package io.github.lvicentesanchez

import scalaz.EitherT

package object purefn {
  object lambdas {
    trait EitherTL[M[_], A] {
      type T[B] = EitherT[M, A, B]
    }
  }
}
