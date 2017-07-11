package com.allangray.documents


import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths, StandardOpenOption}
import java.util.concurrent.CountDownLatch

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.gridfs._
import org.mongodb.scala.gridfs.helpers.AsynchronousChannelHelper
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observable, Observer, WriteConcern}

import scala.concurrent.Await
import scala.concurrent.duration._


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

//      val latch: CountDownLatch = new CountDownLatch(2)
//      latch.await()

      //AGDocument
//      val testDocs = AGDocuments(database).searchDocuments("dfsfds")
//      testDocs.map(f=> println(f.fileName + " " + f.metaData.toString))
      //


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
    val inputPath: Path = Paths.get("/home/dev/temp/temp.jpg")
    //val inputPath: Path = Paths.get("/home/dev/temp/temp.txt")

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

  private def readGridFS(database: MongoDatabase): Unit = {

    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")
    val dstByteBuffer: ByteBuffer = ByteBuffer.allocate(1024 * 1024)

    val downloadStream: GridFSDownloadStream = customFSBucket.openDownloadStream(new ObjectId("5964b7ca229d6d432fa5d1e3"))
    //val downloadStream: GridFSDownloadStream = customFSBucket.openDownloadStream(new ObjectId("5964c558229d6d47e044cbd3"))


    val test = downloadStream.read(dstByteBuffer).map(result => {
      dstByteBuffer.flip
      val bytes: Array[Byte] = new Array[Byte](result)
      dstByteBuffer.get(bytes)
      println(new String(bytes, StandardCharsets.UTF_8))
    }).toFuture()
    Await.result(test,2.seconds)

    //.head()

  }

}
