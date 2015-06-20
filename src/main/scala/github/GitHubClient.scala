package github

import java.util.Base64

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsValue, Json}

// just to avoid ambiguous constructor of GitHubClient
case class AuthHeader(value: String) {}

case class GitHubClient(authHeader: AuthHeader) {
  val logger = LoggerFactory.getLogger(this.getClass)

  val baseUrl = "https://api.github.com"
  val httpclient = HttpClients.createDefault()

  /**
   * Initialize with access token.
   * See https://help.github.com/articles/creating-an-access-token-for-command-line-use/
   */
  def this(accessToken: String) = this(AuthHeader(s"token $accessToken"))

  /**
   * Initialize with user name and password.
   */
  def this(username: String, password: String) = {
    this(AuthHeader("Basic " + Base64.getEncoder.encodeToString((username + ':' + password).getBytes)))
  }

  /**
   * Call 'GET $resources' and return the json body
   *
   * @param resource resource to get. e.g., /issues
   * @param params HTTP query strings
   * @return response body in JSON
   */
  def get(resource: String, params: Map[String, Any] = Map()): JsValue = {
    val httpGet = new HttpGet(s"$baseUrl$resource${params.map(q => s"${q._1}=${q._2}").mkString("?","&", "")}")
    httpGet.addHeader("Authorization", authHeader.value)

    logger.debug(s"Requesting ${httpGet.getMethod} ${httpGet.getURI}")
    val response = httpclient.execute(httpGet)
    logger.debug(s"Response from ${httpGet.getMethod} ${httpGet.getURI}: ${response.getStatusLine}")

    Json.parse(response.getEntity.getContent)
  }

  /**
   * Call 'GET $resources'. The response body is assumed to be in JSON array.
   *
   * @param resource resource to get. e.g., /issues
   * @param params HTTP query strings
   * @param pageSize value to set "per_page" parameter
   * @param numOfPagesToFetch number of pages to fetch. no limit when None.
   * @return
   */
  def pagedGet(resource: String, params: Map[String, Any] = Map(), pageSize: Int = 100, numOfPagesToFetch: Option[Int] = None): Iterator[JsArray] = {
    new Iterator[JsArray] {
      // page index starts with 1
      var pageToFetch = 0
      var array: JsArray = JsArray()

      override def hasNext: Boolean = {
        pageToFetch == 0 || (pageToFetch <= numOfPagesToFetch.getOrElse(Int.MaxValue) && !array.value.isEmpty)
      }

      override def next(): JsArray = {
        pageToFetch += 1
        array = get(resource, params + ("per_page" -> pageSize, "page" -> pageToFetch)).as[JsArray]
        array
      }
    }
  }
}
