/*
 *   __   __     __  __     __         __
 *  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *    \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *   ______     ______       __     ______     ______     ______
 *  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 * https://joshbassett.info
 * https://twitter.com/nullobject
 * https://github.com/nullobject
 *
 * Copyright (c) 2022 Josh Bassett
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import chisel3._
import chiseltest._
import core._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait DebugLayerTestHelpers {
  def mkModule(format: String, n: Int, args: UInt*) =
    new Module {
      val io = IO(new Bundle {
        val a = Output(Vec(n, UInt(8.W)))
      })
      io.a := DebugLayer.decodeLine(Format.tokenize(format), VecInit(args))
    }
}

class DebugLayerTest extends AnyFlatSpec with ChiselScalatestTester with Matchers with DebugLayerTestHelpers {
  behavior of "decodeLine"

  it should "decode a hexadecimal argument" in {
    test(mkModule("FOO %X", 6, 0xab.U(8.W))) { dut =>
      dut.io.a(0).expect(38)
      dut.io.a(1).expect(47)
      dut.io.a(2).expect(47)
      dut.io.a(3).expect(0)
      dut.io.a(4).expect(33)
      dut.io.a(5).expect(34)
    }
  }

  it should "decode a hexadecimal argument with width" in {
    test(mkModule("FOO %4X", 8, 0x1234.U(16.W))) { dut =>
      dut.io.a(0).expect(38)
      dut.io.a(1).expect(47)
      dut.io.a(2).expect(47)
      dut.io.a(3).expect(0)
      dut.io.a(4).expect(17)
      dut.io.a(5).expect(18)
      dut.io.a(6).expect(19)
      dut.io.a(7).expect(20)
    }
  }

  it should "decode a hexadecimal argument with width smaller than value" in {
    test(mkModule("FOO %2X", 6, 0x1234.U(16.W))) { dut =>
      dut.io.a(0).expect(38)
      dut.io.a(1).expect(47)
      dut.io.a(2).expect(47)
      dut.io.a(3).expect(0)
      dut.io.a(4).expect(19)
      dut.io.a(5).expect(20)
    }
  }

  it should "decode a hexadecimal argument with width larger than value" in {
    test(mkModule("FOO %4X", 8, 0x12.U(8.W))) { dut =>
      dut.io.a(0).expect(38)
      dut.io.a(1).expect(47)
      dut.io.a(2).expect(47)
      dut.io.a(3).expect(0)
      dut.io.a(4).expect(0)
      dut.io.a(5).expect(0)
      dut.io.a(6).expect(17)
      dut.io.a(7).expect(18)
    }
  }

  it should "decode a hexadecimal argument zero padding" in {
    test(mkModule("FOO %04X", 8, 0x12.U(16.W))) { dut =>
      dut.io.a(0).expect(38)
      dut.io.a(1).expect(47)
      dut.io.a(2).expect(47)
      dut.io.a(3).expect(0)
      dut.io.a(4).expect(16)
      dut.io.a(5).expect(16)
      dut.io.a(6).expect(17)
      dut.io.a(7).expect(18)
    }
  }

  it should "decode a hexadecimal argument zero padding lol" in {
    test(mkModule("FOO %04X", 8, 0x12.U(8.W))) { dut =>
      dut.io.a(0).expect(38)
      dut.io.a(1).expect(47)
      dut.io.a(2).expect(47)
      dut.io.a(3).expect(0)
      dut.io.a(4).expect(16)
      dut.io.a(5).expect(16)
      dut.io.a(6).expect(17)
      dut.io.a(7).expect(18)
    }
  }
}
