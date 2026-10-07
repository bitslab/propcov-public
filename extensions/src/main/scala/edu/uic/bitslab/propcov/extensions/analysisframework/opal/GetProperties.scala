package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import org.opalj.br.Method
import org.opalj.br.reader.Java8Framework.ClassFile
import org.opalj.io.process

import java.io.{DataInputStream, InputStream}
import java.util.function.Predicate


object GetProperties {
  def get(inputStream: InputStream, isPropertyTest: Predicate[Method]): List[(String, String)] = {
    val classInputStream = new DataInputStream(inputStream)

    val cfs: List[(String, String)] = process(classInputStream) { in =>
      ClassFile(in)
        .flatMap(c => c.methods)
        .filter(m => isPropertyTest.test(m))
        .map(m => (m.classFile.thisType.simpleName + "#" + m.name, OpalUtil.methodToJavaJVM(m)))
    }
    cfs
  }
}

// need to review class names