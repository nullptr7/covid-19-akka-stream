package com.github.nullptr7
package entrypoint

import domain.{CalculationResponse, CovidData}
import util.CovidDataDownloader.{actorSystem, downloadFile}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, FlowShape, Materializer, SystemMaterializer, UniformFanOutShape}

import java.io.File
import scala.io.BufferedSource
import scala.util.{Failure, Success}

object Covid19AverageCalculator {

  private implicit val system: ActorSystem = ActorSystem("Covid19AverageCalculator")
  private implicit val materializer: Materializer = SystemMaterializer(system).materializer

  def main(args: Array[String]): Unit = {

    import domain.CovidData._

    val covidSource: Source[String, NotUsed] = Source.fromIterator(() => {
      covidDataSource._2.close()
      covidDataSource._1
    }).drop(1)

    val stringToCovidDataDomainFlow: Flow[String, CovidData, NotUsed] = Flow[String].map(_.split(",", -1))
                                                                                    .map(x => CovidData(x(2), x(5), x(8)))

    val destination = Sink.foreach[CalculationResponse](x => {
      println(s"Country: ${x.country} DaysOfData: ${x.daysOfData} Average ${x.`type`} is ${x.stats}")
    })

    val staticInFlowGraph = GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._
      val A = builder.add(stringToCovidDataDomainFlow)
      val B = builder.add(Broadcast[CovidData](2))

      val groupedByCountry = Flow[CovidData].groupBy(300, _.country)

      val C: FlowShape[CovidData, CalculationResponse] = builder.add(groupedByCountry.fold[CalculationResponse](CalculationResponse(`type` = "NewCases"))(calculateAverageNewCases)
                                                                                     .map(x => CalculationResponse(x.daysOfData, x.stats / x.daysOfData, x.country, x.`type`))
                                                                                     .mergeSubstreams)

      val D: FlowShape[CovidData, CalculationResponse] = builder.add(groupedByCountry.fold[CalculationResponse](CalculationResponse(`type` = "Deaths"))(calculateAverageDeaths)
                                                                                     .map(x => CalculationResponse(x.daysOfData, x.stats / x.daysOfData, x.country, x.`type`))
                                                                                     .mergeSubstreams)
      A ~> B ~> C
           B ~> D

      UniformFanOutShape(A.in, C.out, D.out)
    }

    val calculateAverage: RunnableGraph[NotUsed] = RunnableGraph.fromGraph {
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val A = builder.add(staticInFlowGraph)

        covidSource ~> A ~> destination
                       A ~> destination

        ClosedShape
      }
    }

    downloadFile(Uri("https://covid.ourworldindata.org/data/owid-covid-data.csv")).onComplete {
      case Success(value) =>
        println(s"File download successfully, ${value.status.get} with size ${value.count}")
        actorSystem.terminate()
        calculateAverage.run()(materializer)
      case Failure(exception) =>
        println(s"Failure in downloading the file ${exception.getMessage}")
    }(actorSystem.dispatcher)

  }

  private def covidDataSource: (Iterator[String], BufferedSource) = {
    val bufferedSource: BufferedSource = io.Source.fromFile(new File("src/main/resources/covid-data.csv"))
    val covidDataIterable: Iterator[String] = bufferedSource.getLines()
    (covidDataIterable, bufferedSource)
  }

  private def calculateAverageNewCases: (CalculationResponse, CovidData) => CalculationResponse = {
    (counter, data) =>
      CalculationResponse(daysOfData = counter.daysOfData + 1,
                          stats = counter.stats + data.newCases.get,
                          country = data.country,
                          `type` = counter.`type`)
  }

  private def calculateAverageDeaths: (CalculationResponse, CovidData) => CalculationResponse = {
    (counter, data) =>
      CalculationResponse(daysOfData = counter.daysOfData + 1,
                          stats = counter.stats + data.newDeaths.get,
                          country = data.country,
                          `type` = counter.`type`)
  }
}
