package ghscala
package scalajhttp

import scalaj.http._
import scalaz.Endo

object ScalajHttp{

  val OPTIONS = List( HttpOptions.connTimeout(30000) , HttpOptions.readTimeout(30000) )

  def apply(req: ghscala.Request): Http.Request = {
    val r0 = req.method match {
      case "GET"      => get(req.url)
      case "POST"     => post(req.url)
    }
    val r1 = r0.params(req.params)
    req.basicAuth.fold(r1){case (user, pass) => r1.auth(user, pass)}
  }

  private def get(url:String): Http.Request = Http(url).options(OPTIONS)

  private def post(url:String): Http.Request = Http.post(url).options(OPTIONS)

  // TODO https://gist.github.com/xuwei-k/5608828

}
