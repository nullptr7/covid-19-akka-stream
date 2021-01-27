package com.github.nullptr7
package util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{IOResult, Materializer, SystemMaterializer}
import akka.util.ByteString

import java.io.File
import scala.concurrent.Future
import scala.util.Try

object CovidDataDownloader {

  implicit val actorSystem: ActorSystem = ActorSystem("CovidDataDownloader")
  private implicit val localMat: Materializer = SystemMaterializer(actorSystem).materializer

  def responseOrFail(in: (Try[HttpResponse], Unit)): (HttpResponse, Unit) = in match {
    case (responseTry, context) => (responseTry.get, context)
  }

  def responseToByteSource(in: (HttpResponse, Unit)): Source[ByteString, Any] = in match {
    case (response, _) => response.entity.dataBytes
  }

  def downloadFile(uri: Uri): Future[IOResult] = {

    val file = new File("src/main/resources/covid-data.csv")
    file.delete()
    file.createNewFile()
    val request = Get(uri)
    val source = Source.single((request, ()))
    val requestResponseFlow = Http().superPool[Unit]()
    source.via(requestResponseFlow)
          .map(responseOrFail)
          .flatMapConcat(responseToByteSource)
          .runWith(FileIO.toPath(file.toPath))

  }

  /*  def main(args: Array[String]): Unit = {

      downloadFile(Uri.apply("https://file-examples-com.github.io/uploads/2017/02/file_example_CSV_5000.csv"), new File("src/main/resources/test.csv"))
        .onComplete {
          case Success(value) =>
            println(s"File download successfully, ${value.status.get}")
            actorSystem.terminate()
          case Failure(exception) => println(s"Failure in downloading the file ${exception.getMessage}")
        }(actorSystem.dispatcher)
    }*/
}
