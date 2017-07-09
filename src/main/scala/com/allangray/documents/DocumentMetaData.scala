package com.allangray.documents

import java.time.LocalDate
import java.util.Date

import org.bson.{BsonValue, Document}


case class DocumentMetaData(id: BsonValue,
                            fileName:String,
                            metaData: Document,
                            createdDate: Date)