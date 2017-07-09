package com.allangray.documents


import java.nio.channels.AsynchronousFileChannel
import java.nio.file.{Path, Paths, StandardOpenOption}
import java.util.concurrent.CountDownLatch

import org.mongodb.scala.bson._
import org.mongodb.scala.gridfs._
import org.mongodb.scala.gridfs.helpers.AsynchronousChannelHelper
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observable, Observer, WriteConcern}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.mongodb.scala.bson.ObjectId


object Main {
  def main(args: Array[String]): Unit = {
    try {
      println("****************STARTED***********************")
      val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
      val database: MongoDatabase = mongoClient.getDatabase("mydb")
      val collection: MongoCollection[Document] = database.getCollection("movies")

      // insert a document
      val document: Document = Document("_id" -> 2, "x" -> 4)
      val doc2: Document = Document("_id" -> 4, "firstName" -> "Rory", "lastName" -> "Bolus")
      //writeSomething(collection, doc2)
      //readRecords(collection, Some(document))
      //val latch: CountDownLatch = new CountDownLatch(1)
      //latch.await()
      //mongoClient.close()

      //writeGridFS(database)

      readGridFS(database)

      val latch: CountDownLatch = new CountDownLatch(2)
      latch.await()

      mongoClient.close()

    } catch {
      case e: Exception => println(e.toString())
      case _: Any => println("what the what")
    }

  }

  private def writeSomething(collection: MongoCollection[Document], document: Document) = {

    println("Lets Write")
    val insertObservable: Observable[Completed] = collection.insertOne(document)

    insertObservable.subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println(s"onNext: $result")

      override def onError(e: Throwable): Unit = println(s"onError: $e")

      override def onComplete(): Unit = println("onComplete")
    })
  }

  private def readRecords(collection: MongoCollection[Document], document: Option[Document]) = {
    println("Lets Read")



    val observable = collection.find()

    observable.subscribe(
      (doc: Document) => println(doc.toJson()),
      (t: Throwable) => println("Failed"),
      () => println("Done")
    )

    Await.result(observable.toFuture(), 2.seconds)

    //    while (!observable.toFuture().isCompleted) {
    //      Thread sleep 200
    //    }


  }

  private def writeGridFS(database: MongoDatabase) = {

    // Create a gridFSBucket with a custom bucket name "files"
    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore").withWriteConcern(WriteConcern.ACKNOWLEDGED)

    // Get the input stream
    val inputPath: Path = Paths.get("/home/daviddo/test.txt")

    val fileToRead: AsynchronousFileChannel = AsynchronousFileChannel.open(inputPath, StandardOpenOption.READ)
    val streamToUploadFrom: AsyncInputStream = AsynchronousChannelHelper.channelToInputStream(fileToRead) // Using the AsynchronousChannelHelper

    // Create some custom options
    val options: GridFSUploadOptions = new GridFSUploadOptions().metadata(Document("type" -> "Passport", "contactID" -> "1-22345"))

    val trackMe = customFSBucket.uploadFromStream("Passport-FirstLastName", streamToUploadFrom, options)

    trackMe.subscribe(
      (x: ObjectId) => println("Done with: " + x),
      (t: Throwable) => println("Failed: " + t.toString)
    )
    Await.result(trackMe.toFuture(), 2.seconds)
    streamToUploadFrom.close()

  }

  private def readGridFS(database: MongoDatabase) = {

    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")

    println("File names:")
    //val docToFind = Document("id" -> "58cbb057fa58955608f97e3f")
    //val findResult = customFSBucket.find(Filters.equal("_id", "58e3666ba045686607752b08"))
    //val findResult = customFSBucket.find(Filters.equal("_id" , "58e3869af5f0396b876c1868" ))


    //val findResult = customFSBucket.find(Filters.equal("metadata.contactID", "1-22345"))
    //collection.find(and(gte("stars", 2), lt("stars", 5), eq("categories", "Bakery")))
    //val findResult = customFSBucket.find(org.mongodb.scala.model.Filters.and(Filters.equal("metadata.contactID", "1-22345"),
    //  Filters.gte("length", 1)))
    //gte("lenght",1)))

    //val findResult = customFSBucket.find(Filters.equal("filename" , "/home/dev/temp/test.txt" ))
    //db.fs.files.find({"filename" : "/home/dev/temp/test.pdf" }

//    val findResult = customFSBucket.find(Filters.equal("_id" , new ObjectId("5960747451987a0d7c0d2e8d")))
//
//      findResult.subscribe(
//      (files: GridFSFile) => {
//        println("files found...")
//        println(files.getFilename, " : " + files.getId + " : " + files.getMetadata + " : " + files.hashCode()) + " :  " + files.getObjectId
//        //outputResults(files.getId,files.hashCode().toString)
//      },
//      (t: Throwable) => println("Failed with " + t.toString),
//      () => println("Done reading")
//    )
//    Await.result(findResult.toFuture(), 2.seconds)

    val futureFiles: Future[Seq[GridFSFile]] = customFSBucket.find(Filters.equal("_id" , new ObjectId("5960747451987a0d7c0d2e8d"))).toFuture()

    Await.result(futureFiles,2.seconds)
    futureFiles.foreach((files:GridFSFile) => {outputResults(files.getId,files.getFilename)})

    def outputResults(fileId: BsonValue, hash: String): Unit = {
      val outputPath: Path = Paths.get("/home/dev/temp/mongodb-tutorial" + hash + ".jpg")


      var streamToDownloadTo: AsynchronousFileChannel = AsynchronousFileChannel.open(outputPath,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE
        //,StandardOpenOption.DELETE_ON_CLOSE
      )

      val trackMe = customFSBucket.downloadToStream(fileId, AsynchronousChannelHelper.channelToOutputStream(streamToDownloadTo))

      trackMe.subscribe(
        (x: Long) => println("OnNext: " + x),
        (t: Throwable) => println("Failed: " + t.toString),
        () => {
          println("Complete")
          streamToDownloadTo.close()
        }
      )

      Await.result(trackMe.toFuture(), 2.seconds)


    }

  }


}
