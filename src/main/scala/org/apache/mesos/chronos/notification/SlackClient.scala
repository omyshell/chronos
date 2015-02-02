package org.apache.mesos.chronos.notification

import java.io.{DataOutputStream, StringWriter}
import java.net.{HttpURLConnection, URL}
import java.util.logging.Logger

import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import com.fasterxml.jackson.core.JsonFactory

class SlackClient(val webhookUrl: String,
                  val token: String,
                  val channel: String) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {

    val jsonBuffer = new StringWriter
    val factory = new JsonFactory()
    val generator = factory.createGenerator(jsonBuffer)

    // Create the payload
    generator.writeStartObject()
    generator.writeStringField("channel", channel)

    if (message.nonEmpty && message.get.nonEmpty) {
      generator.writeStringField("text", message.get)
    }

    generator.writeEndObject()
    generator.flush()

    val payload = jsonBuffer.toString

    var connection: HttpURLConnection = null
    try {
      val url = new URL(webhookUrl + "?token=" + token)
      connection = url.openConnection.asInstanceOf[HttpURLConnection]
      connection.setDoInput(true)
      connection.setDoOutput(true)
      connection.setUseCaches(false)
      connection.setRequestMethod("POST")

      val outputStream = new DataOutputStream(connection.getOutputStream)
      outputStream.writeBytes(payload)
      outputStream.flush()
      outputStream.close()

      log.info("Sent message to Slack! Response code:" +
        connection.getResponseCode +
        " - " +
        connection.getResponseMessage)
    } finally {
      if (connection != null) {
        connection.disconnect()
      }
    }
  }
}
