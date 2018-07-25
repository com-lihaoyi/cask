package cask.model

sealed trait Status {
  val code: Int
  val reason: String
}

object Status {
  val codesToStatus: Map[Int, Status] = Map(
    100 -> Continue,
    101 -> SwitchingProtocols,
    200 -> OK,
    201 -> Created,
    202 -> Accepted,
    203 -> NonAuthoritativeInformation,
    204 -> NoContent,
    205 -> ResetContent,
    206 -> PartialContent,
    300 -> MultipleChoices,
    301 -> MovedPermanently,
    302 -> Found,
    303 -> SeeOther,
    304 -> NotModified,
    305 -> UseProxy,
    307 -> TemporaryRedirect,
    308 -> PermanentRedirect,
    400 -> BadRequest,
    401 -> Unauthorized,
    402 -> PaymentRequired,
    403 -> Forbidden,
    404 -> NotFound,
    405 -> MethodNotAllowed,
    406 -> NotAcceptable,
    407 -> ProxyAuthenticationRequired,
    408 -> RequestTimeout,
    409 -> Conflict,
    410 -> Gone,
    411 -> LengthRequired,
    412 -> PreconditionFailed,
    413 -> RequestEntityTooLarge,
    414 -> RequestURITooLong,
    415 -> UnsupportedMediaType,
    416 -> RequestedRangeNotSatisfiable,
    417 -> ExpectationFailed,
    418 -> Teapot,
    420 -> EnhanceYourCalm,
    429 -> TooManyRequests,
    451 -> UnavailableForLegalReasons,
    500 -> InternalServerError,
    501 -> NotImplemented,
    502 -> BadGateway,
    503 -> ServiceUnavailable,
    504 -> GatewayTimeout,
    505 -> HTTPVersionNotSupported
  )

  val statusToCodes: Map[String, Int] =
    codesToStatus.map { case (code, status) => status.reason -> code }


  case class Unknown(code: Int, reason: String) extends Status

  case object Continue extends Status {
    val code = 100
    val reason: String = "Continue"
  }

  case object SwitchingProtocols extends Status {
    val code = 101
    val reason: String = "Switching Protocols"
  }

  case object OK extends Status {
    val code = 200
    val reason: String = "OK"
  }

  case object Created extends Status {
    val code = 201
    val reason: String = "Created"
  }

  case object Accepted extends Status {
    val code = 202
    val reason: String = "Accepted"
  }

  case object NonAuthoritativeInformation extends Status {
    val code = 203
    val reason: String = "Non-Authoritative Information"
  }

  case object NoContent extends Status {
    val code = 204
    val reason: String = "No Content"
  }

  case object ResetContent extends Status {
    val code = 205
    val reason: String = "Reset Content"
  }

  case object PartialContent extends Status {
    val code = 206
    val reason: String = "Partial Content"
  }

  case object MultipleChoices extends Status {
    val code = 300
    val reason: String = "Multiple Choices"
  }

  case object MovedPermanently extends Status {
    val code = 301
    val reason: String = "Moved Permanently"
  }

  case object Found extends Status {
    val code = 302
    val reason: String = "Found"
  }

  case object SeeOther extends Status {
    val code = 303
    val reason: String = "See Other"
  }

  case object NotModified extends Status {
    val code = 304
    val reason: String = "Not Modified"
  }

  case object UseProxy extends Status {
    val code = 305
    val reason: String = "Use Proxy"
  }

  case object TemporaryRedirect extends Status {
    val code = 307
    val reason: String = "Temporary Redirect"
  }

  case object PermanentRedirect extends Status {
    val code = 308
    val reason: String = "Permanent Redirect"
  }

  case object BadRequest extends Status {
    val code = 400
    val reason: String = "Bad Request"
  }

  case object Unauthorized extends Status {
    val code = 401
    val reason: String = "Unauthorized"
  }

  case object PaymentRequired extends Status {
    val code = 402
    val reason: String = "Payment Required"
  }

  case object Forbidden extends Status {
    val code = 403
    val reason: String = "Forbidden"
  }

  case object NotFound extends Status {
    val code = 404
    val reason: String = "Not Found"
  }

  case object MethodNotAllowed extends Status {
    val code = 405
    val reason: String = "Method Not Allowed"
  }

  case object NotAcceptable extends Status {
    val code = 406
    val reason: String = "Not Acceptable"
  }

  case object ProxyAuthenticationRequired extends Status {
    val code = 407
    val reason: String = "Proxy Authentication Required"
  }

  case object RequestTimeout extends Status {
    val code = 408
    val reason: String = "Request Time-out"
  }

  case object Conflict extends Status {
    val code = 409
    val reason: String = "Conflict"
  }

  case object Gone extends Status {
    val code = 410
    val reason: String = "Gone"
  }

  case object LengthRequired extends Status {
    val code = 411
    val reason: String = "Length Required"
  }

  case object PreconditionFailed extends Status {
    val code = 412
    val reason: String = "Precondition Failed"
  }

  case object RequestEntityTooLarge extends Status {
    val code = 413
    val reason: String = "Request Entity Too Large"
  }

  case object RequestURITooLong extends Status {
    val code = 414
    val reason: String = "Request-URI Too Large"
  }

  case object UnsupportedMediaType extends Status {
    val code = 415
    val reason: String = "Unsupported Media Type"
  }

  case object RequestedRangeNotSatisfiable extends Status {
    val code = 416
    val reason: String = "Requested range not satisfiable"
  }

  case object ExpectationFailed extends Status {
    val code = 417
    val reason: String = "Expectation Failed"
  }

  case object Teapot extends Status {
    val code = 418
    val reason: String = "I'm a teapot"
  }

  case object EnhanceYourCalm extends Status {
    val code = 420
    val reason: String = "Enhance Your Calm"
  }

  case object TooManyRequests extends Status {
    val code = 429
    val reason: String = "Too Many Requests"
  }

  case object UnavailableForLegalReasons extends Status {
    val code = 451
    val reason: String = "Unavailable For Legal Reasons"
  }

  case object InternalServerError extends Status {
    val code = 500
    val reason: String = "Internal Server Error"
  }

  case object NotImplemented extends Status {
    val code = 501
    val reason: String = "Not Implemented"
  }

  case object BadGateway extends Status {
    val code = 502
    val reason: String = "Bad Gateway"
  }

  case object ServiceUnavailable extends Status {
    val code = 503
    val reason: String = "Service Unavailable"
  }

  case object GatewayTimeout extends Status {
    val code = 504
    val reason: String = "Gateway Time-out"
  }

  case object HTTPVersionNotSupported extends Status {
    val code = 505
    val reason: String = "HTTP Version not supported"
  }

}
