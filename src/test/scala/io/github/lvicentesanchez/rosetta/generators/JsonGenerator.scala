package io.github.lvicentesanchez.rosetta.generators

import argonaut._, Argonaut._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen.{ listOfN, const ⇒ value, oneOf }
import org.scalacheck.{ Gen, Arbitrary }

trait JsonGenerator {
  val maxJsonStructureDepth = 3

  val jsonNumberGenerator: Gen[Json] = arbitrary[Double].map(jNumberOrNull)

  def isValidJSONCharacter(char: Char): Boolean = !char.isControl && char != '\\' && char != '\"'

  val stringGenerator: Gen[String] = arbitrary[String]

  val jsonStringGenerator: Gen[Json] = stringGenerator.map(string ⇒ jString(string))

  val jsonBoolGenerator: Gen[Json] = oneOf(jBool(true), jBool(false))

  val jsonNothingGenerator: Gen[Json] = value(jNull)

  def jsonArrayItemsGenerator(depth: Int = maxJsonStructureDepth): Gen[Seq[Json]] = listOfN(5, jsonValueGenerator(depth - 1))

  def jsonArrayGenerator(depth: Int = maxJsonStructureDepth): Gen[Json] = jsonArrayItemsGenerator(depth).map { values ⇒ jArray(values.toList) }

  def jsonObjectFieldsGenerator(depth: Int = maxJsonStructureDepth): Gen[Seq[(Json, Json)]] = listOfN(5, arbTuple2(Arbitrary(jsonStringGenerator), Arbitrary(jsonValueGenerator(depth - 1))).arbitrary)

  private def arbImmutableMap[T: Arbitrary, U: Arbitrary]: Arbitrary[Map[T, U]] =
    Arbitrary(Gen.listOf(arbTuple2[T, U].arbitrary).map(_.toMap))

  def jsonObjectGenerator(depth: Int = maxJsonStructureDepth): Gen[Json] = arbImmutableMap(Arbitrary(arbitrary[String]), Arbitrary(jsonValueGenerator(depth - 1))).arbitrary.map { map ⇒
    jObjectAssocList(map.toList)
  }

  def jsonValueGenerator(depth: Int = maxJsonStructureDepth): Gen[Json] = {
    if (depth > 1) {
      oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator, jsonArrayGenerator(depth - 1), jsonObjectGenerator(depth - 1))
    } else {
      oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator)
    }
  }
  val nonJsonObjectGenerator = oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator, jsonArrayGenerator())

  val jsonObjectOrArrayGenerator = oneOf(jsonObjectGenerator(), jsonArrayGenerator())

}