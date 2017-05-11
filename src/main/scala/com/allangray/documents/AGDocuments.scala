package com.allangray.documents

import java.io.File
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
case class metaData(key:String, value:String)

trait Documents {
  def writeDocument(fileName:String,filePath:String,metaData: metaData) : ErrorResult
  def retrieveDocument(searchString: String) : Document
}

case class AGDocuments(database: MongoDatabase,bucketName:String) extends Documents {


  override def writeDocument(fileName:String,filePath:String,metaData: metaData): ErrorResult = {
    writeGridFS(database,bucketName,fileName,filePath,  metaData)
    ErrorResult("","")
  }


  override def retrieveDocument(searchString: String): Document = ???

  private def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def writeFolder(folderPath: String) = {
    val listOfFiles = getListOfFiles(folderPath)
    listOfFiles.map(f => writeDocument(f.getName, f.getCanonicalPath,metaData("fileName" , f.getName)))
  }
  private def writeGridFS(database: MongoDatabase,bucketName:String,fileName:String,filePath:String,metaData: metaData) = {

    // Create a gridFSBucket with a custom bucket name "files"
    val customFSBucket: GridFSBucket = GridFSBucket(database, bucketName)

    // Get the input stream
    val inputPath: Path = Paths.get(filePath)  // + "/" + fileName)

    val fileToRead: AsynchronousFileChannel = AsynchronousFileChannel.open(inputPath, StandardOpenOption.READ)
    val streamToUploadFrom: AsyncInputStream = AsynchronousChannelHelper.channelToInputStream(fileToRead) // Using the AsynchronousChannelHelper

    // Create some custom options
    //val options: GridFSUploadOptions = new GridFSUploadOptions().metadata(Document("type" -> "Passport", "contactID" -> "1-22345"))
    val options: GridFSUploadOptions = new GridFSUploadOptions().metadata(Document(metaData.key -> metaData.value))

    val trackMe = customFSBucket.uploadFromStream("Passport-FirstLastName", streamToUploadFrom, options)

    trackMe.subscribe(
      (x: ObjectId) => println("Done with: " + x),
      (t: Throwable) => println("Failed: " + t.toString)
    )
    Await.result(trackMe.toFuture(), 2.seconds)
    streamToUploadFrom.close()

  }

}
