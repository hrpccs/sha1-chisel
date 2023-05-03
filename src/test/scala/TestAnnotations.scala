package sha1

import chiseltest.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import firrtl.AnnotationSeq
import java.nio.file.{Files, Paths}

object WriteVcdEnabler {
  val annos: AnnotationSeq = Seq(WriteVcdAnnotation)
}

object TestAnnotations {
  val annos = WriteVcdEnabler.annos
}
