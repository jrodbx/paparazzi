package sample

import org.junit.Ignore
import org.junit.Test

class Test1 {
  @Test
  fun testA() {
  }

  @Test
  @Ignore
  fun testB() {
    throw Exception("testB")
  }
}
