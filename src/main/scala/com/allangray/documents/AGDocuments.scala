package com.allangray.documents

import java.nio.channels.AsynchronousFileChannel
import java.nio.file.{Path, Paths, StandardOpenOption}

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import org.mongodb.scala._
import java.time.LocalDate

import org.bson.BsonValue
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.gridfs.helpers.AsynchronousChannelHelper
import org.mongodb.scala.gridfs._
import org.mongodb.scala.model.Filters

import scala.concurrent.Await
import scala.concurrent.duration._


case class ErrorResult(errCode: String, errReason : String)

trait Documents {
  def saveDocument(fileName:String, filePath: String) : ErrorResult
  //def searchDocuments(searchString: String) : Seq[DocumentMetaData]
  //def getDocument(documentId: String) : Document
}

case class AGDocuments(database:MongoDatabase) extends Documents {
  override def saveDocument(fileName: String, filePath: String): ErrorResult = ???

   def searchDocuments(searchString: String) = {

    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")

    println("File names:")

    //val findResult = customFSBucket.find(Filters.equal("metadata.contactID", "1-22345"))
    //collection.find(and(gte("stars", 2), lt("stars", 5), eq("categories", "Bakery")))
    val findObservable = customFSBucket.find(org.mongodb.scala.model.Filters.and(Filters.equal("metadata.contactID", "1-22345"),
      Filters.gte("length", 1)))


     val filesData =  for {
       findResult <- findObservable

     } yield findResult


//    findResult.subscribe(
//      (files: GridFSFile) => {
//        println("files found...")
//        //println(files.getFilename, " : " + files.getId + " : " + files.getMetadata + " : " + files.hashCode()) + " :  " + files.getObjectId
//        buildMetaResults(files)
//      },
//      (t: Throwable) => println("Failed with " + t.toString)
//      //() => println("Done reading")
//    )
    Await.result(findObservable.toFuture(), 2.seconds)


    def buildMetaResults(files: GridFSFile):DocumentMetaData = {
      DocumentMetaData(files.getId,files.getFilename,files.getMetadata,(files.getUploadDate))
    }

     buildMetaResults(findResult)
  }

   def getDocument(documentId: String) = {

    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")

     val findResult = customFSBucket.find(Filters.equal("_id" , new ObjectId(documentId)))

    findResult.subscribe(
      (files: GridFSFile) => {
        println("files found...")
        println(files.getFilename, " : " + files.getId + " : " + files.getMetadata + " : " + files.hashCode()) + " :  " + files.getObjectId
        outputResults(files.getId,files.hashCode().toString)
      },
      (t: Throwable) => println("Failed with " + t.toString),
      () => println("Done reading")
    )
    Await.result(findResult.toFuture(), 2.seconds)

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

  private def writeToGridFS(database: MongoDatabase,bucketName:String,fileName:String,filePath:String) = {
    val customFSBucket: GridFSBucket = GridFSBucket(database, bucketName)
    // Get the input stream
    val inputPath: Path = Paths.get(filePath + "/" + fileName)

    val fileToRead: AsynchronousFileChannel = AsynchronousFileChannel.open(inputPath, StandardOpenOption.READ)
    val streamToUploadFrom: AsyncInputStream = AsynchronousChannelHelper.channelToInputStream(fileToRead) // Using the AsynchronousChannelHelper

    // Create some custom options
    val options: GridFSUploadOptions = new GridFSUploadOptions().metadata(Document("type" -> "Passport", "contactID" -> "1-22345"))

    val trackMe = customFSBucket.uploadFromStream(fileName, streamToUploadFrom, options)

    trackMe.subscribe(
      (x: ObjectId) => println("Done with: " + x),
      (t: Throwable) => println("Failed: " + t.toString)
    )
    Await.result(trackMe.toFuture(), 2.seconds)
    streamToUploadFrom.close()
  }



}
