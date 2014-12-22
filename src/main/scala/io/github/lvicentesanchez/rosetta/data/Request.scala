package io.github.lvicentesanchez.rosetta.data

import argonaut.Json

case class Request(handler: String, body: Json)