package core.util.runtime

private[util] object DefaultRuntime {
  lazy implicit val runtime: zio.Runtime[zio.ZEnv] = zio.Runtime.default
}
