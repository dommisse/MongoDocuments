package com.allangray.documents

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths, StandardOpenOption}

import com.mongodb.async.client.gridfs.helpers.AsynchronousChannelHelper
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.gridfs._
import org.mongodb.scala.model.Filters

import scala.concurrent.Await
import scala.concurrent.duration._


case class ErrorResult(errCode: String, errReason: String)

trait Documents {
  def saveDocument(fileName: String, filePath: String): ErrorResult
  def searchDocuments(searchString: String): Seq[DocumentMetaData]
  def getDocument(documentId: String): Byte
}

case class AGDocuments(database: MongoDatabase) extends Documents {

  override def saveDocument(fileName: String, filePath: String): ErrorResult = ???

  def searchDocuments(searchString: String): Seq[DocumentMetaData] = {

    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")

    println("File names:")

    //val findResult = customFSBucket.find(Filters.equal("metadata.contactID", "1-22345"))
    //collection.find(and(gte("stars", 2), lt("stars", 5), eq("categories", "Bakery")))
    val findObservable = customFSBucket.find(org.mongodb.scala.model.Filters.and(Filters.equal("metadata.contactID", "1-22345"),
      Filters.gte("length", 1)))

    var docList: List[DocumentMetaData] = Nil

    findObservable.subscribe(
      (files: GridFSFile) => {
        println("files found...")
        //println(files.getFilename, " : " + files.getId + " : " + files.getMetadata + " : " + files.hashCode()) + " :  " + files.getObjectId
        docList = buildMetaResults(files) :: docList
      },
      (t: Throwable) => println("Failed with " + t.toString)

    )
    Await.result(findObservable.toFuture(), 4.seconds)


    def buildMetaResults(files: GridFSFile): DocumentMetaData = {
      DocumentMetaData(files.getId, files.getFilename, files.getMetadata, (files.getUploadDate))
    }

    docList

  }

  def getDocument(documentId: String):Byte = {

    val customFSBucket: GridFSBucket = GridFSBucket(database, "DocStore")

    val dstByteBuffer: ByteBuffer = ByteBuffer.allocate(1024 * 1024)

    val downloadStream: GridFSDownloadStream = customFSBucket.openDownloadStream(new ObjectId("5964b7ca229d6d432fa5d1e3"))
    //val downloadStream: GridFSDownloadStream = customFSBucket.openDownloadStream(new ObjectId("5964c558229d6d47e044cbd3"))


val futureBytes =
    downloadStream.read(dstByteBuffer).map(result => {
      dstByteBuffer.flip
      val bytes: Array[Byte] = new Array[Byte](result)
      dstByteBuffer.get(bytes)
      //println(new String(bytes, StandardCharsets.UTF_8))
    }).head()
    val byteResult = Await.result(futureBytes,2.seconds)
    byteResult.get()
  }


  private def writeToGridFS(database: MongoDatabase, bucketName: String, fileName: String, filePath: String) = {
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
