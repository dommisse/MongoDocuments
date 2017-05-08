package com.allangray.documents

import java.nio.channels.AsynchronousFileChannel
import java.nio.file.{Path, Paths, StandardOpenOption}

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import org.mongodb.scala._
import org.mongodb.scala.bson.{Document => _, _}
import org.mongodb.scala.gridfs.helpers.AsynchronousChannelHelper
import org.mongodb.scala.gridfs._

import scala.concurrent.Await
import scala.concurrent.duration._


case class ErrorResult(errCode: String, errReason : String)

trait Documents {
  def loadDocument(fileName:String, filePath: String) : ErrorResult
  def retrieveDocument(searchString: String) : Document
}

case class AGDocuments(name: String, documentType:String) extends Documents {
  override def loadDocument(fileName: String, filePath: String): ErrorResult = ???

  override def retrieveDocument(searchString: String): Document = ???

  private def writeGridFS(database: MongoDatabase) = {

    // Create a gridFSBucket with a custom bucket name "files"
    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")

    // Get the input stream
    val inputPath: Path = Paths.get("/home/dev/temp/test.txt")

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

}
