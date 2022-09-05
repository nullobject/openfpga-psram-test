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

package arcadia.mem.psram

import chisel3._
import chiseltest._
import org.scalatest._
import flatspec.AnyFlatSpec
import matchers.should.Matchers

trait PSRAMTestHelpers {
  protected val psramConfig = Config(
    clockFreq = 100_000_000D, // 100 MHz
    burstLength = 4,
    burstWrap = true,
    tPU = 20,
  )

  protected def mkPSRAM(config: Config = psramConfig) = new PSRAM(config)

  protected def waitForInit(dut: PSRAM) =
    while (!dut.io.debug.init.peekBoolean()) { dut.clock.step() }

  protected def waitForConfig(dut: PSRAM) =
    while (!dut.io.debug.config.peekBoolean()) { dut.clock.step() }

  protected def waitForIdle(dut: PSRAM) =
    while (!dut.io.debug.idle.peekBoolean()) { dut.clock.step() }

  protected def waitForActive(dut: PSRAM) =
    while (!dut.io.debug.active.peekBoolean()) { dut.clock.step() }

  protected def waitForRead(dut: PSRAM) =
    while (!dut.io.debug.read.peekBoolean()) { dut.clock.step() }

  protected def waitForWrite(dut: PSRAM) =
    while (!dut.io.debug.write.peekBoolean()) { dut.clock.step() }
}

class PSRAMTest  extends AnyFlatSpec with ChiselScalatestTester with Matchers with PSRAMTestHelpers {
  behavior of "FSM"

  it should "move to the mode state after initializing" in {
    test(mkPSRAM()) { dut =>
      waitForInit(dut)
      dut.clock.step(2)
      dut.io.debug.config.expect(true)
    }
  }

  it should "move to the idle state after configuring the device" in {
    test(mkPSRAM()) { dut =>
      waitForConfig(dut)
      dut.clock.step(6)
      dut.io.debug.idle.expect(true)
    }
  }

  it should "move to the active state" in {
    test(mkPSRAM()) { dut =>
      dut.io.mem.rd.poke(true)
      waitForIdle(dut)
      dut.clock.step()
      dut.io.debug.active.expect(true)
    }
  }

  it should "move to the read state after the active state" in {
    test(mkPSRAM()) { dut =>
      dut.io.mem.rd.poke(true)
      waitForRead(dut)
      dut.clock.step()
      dut.io.debug.read.expect(true)
    }
  }

  it should "move to the write state after the active state" in {
    test(mkPSRAM()) { dut =>
      dut.io.mem.wr.poke(true)
      waitForWrite(dut)
      dut.clock.step()
      dut.io.debug.write.expect(true)
    }
  }

  it should "return to the idle state from the read state" in {
    test(mkPSRAM()) { dut =>
      dut.io.mem.rd.poke(true)
      waitForRead(dut)
      dut.io.psram.wait_n.poke(true)
      dut.clock.step(4)
      dut.io.debug.idle.expect(true)
    }
  }

  it should "return to the idle state from the write state" in {
    test(mkPSRAM()) { dut =>
      dut.io.mem.wr.poke(true)
      waitForWrite(dut)
      dut.io.psram.wait_n.poke(true)
      dut.clock.step(4)
      dut.io.debug.idle.expect(true)
    }
  }

  behavior of "initialize"

  it should "initialize the PSRAM" in {
    test(mkPSRAM()) { dut =>
      // init 0
      dut.io.psram.ce0_n.expect(true)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.adv_n.expect(true)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
      dut.clock.step()

      // init 1
      dut.io.psram.ce0_n.expect(true)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.adv_n.expect(true)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
      dut.clock.step()

      // opcode
      dut.io.psram.ce0_n.expect(false)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.cre.expect(true)
      dut.io.psram.adv_n.expect(false)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
      dut.io.psram.addr.expect("b00_10_00".U)
      dut.io.psram.din.expect("b0_0_011_0_0_1_00_01_0_001".U)
      dut.clock.step()

      // bcr
      dut.io.psram.adv_n.expect(true)
      dut.io.psram.we_n.expect(false)
      dut.clock.step(5)

      // done
      dut.io.psram.ce0_n.expect(true)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.adv_n.expect(true)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
    }
  }

  behavior of "read"

  it should "read from the PSRAM (burst=4)" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // read
      dut.io.mem.rd.poke(true)
      dut.io.mem.addr.poke(0x123456)
      waitForActive(dut)
      dut.io.psram.ce0_n.expect(false)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.cre.expect(false)
      dut.io.psram.adv_n.expect(false)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
      dut.io.psram.addr.expect(0x12)
      dut.io.psram.din.expect(0x3456)
      dut.clock.step()

      // wait
      dut.io.psram.wait_n.poke(false)
      dut.clock.step()

      // data 0
      dut.io.psram.wait_n.poke(true)
      dut.io.psram.oe_n.expect(false)
      dut.io.psram.dout.poke(0x1234)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.io.mem.burstDone.expect(false)
      dut.io.mem.dout.expect(0x1234)

      // data 1
      dut.io.psram.oe_n.expect(false)
      dut.io.psram.dout.poke(0x5678)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.io.mem.burstDone.expect(false)
      dut.io.mem.dout.expect(0x5678)

      // data 2
      dut.io.psram.oe_n.expect(false)
      dut.io.psram.dout.poke(0x90ab)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.io.mem.burstDone.expect(true)
      dut.io.mem.dout.expect(0x90ab)

      // data 3
      dut.io.psram.oe_n.expect(false)
      dut.io.psram.dout.poke(0xcdef)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.io.mem.burstDone.expect(true)
      dut.io.mem.dout.expect(0xcdef)

      // done
      dut.io.psram.ce0_n.expect(true)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.adv_n.expect(true)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
    }
  }

  it should "assert the output enable" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // read
      dut.io.mem.rd.poke(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)

      // output enable
      dut.io.psram.oe_n.expect(true)
      dut.clock.step()
      dut.io.psram.oe_n.expect(false)
      dut.clock.step()
      dut.io.psram.oe_n.expect(false)
      dut.clock.step()
      dut.io.psram.oe_n.expect(false)
      dut.clock.step()
      dut.io.psram.oe_n.expect(false)
      dut.clock.step()
      dut.io.psram.oe_n.expect(true)
    }
  }

  it should "assert the wait signal" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // read
      dut.io.mem.rd.poke(true)
      dut.io.mem.wait_n.expect(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)

      // wait
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(true)
    }
  }

  it should "assert the valid signal" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // read
      dut.io.mem.rd.poke(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)
      dut.clock.step()

      // valid
      dut.io.mem.valid.expect(false)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.clock.step()
      dut.io.mem.valid.expect(true)
      dut.clock.step()
      dut.io.mem.valid.expect(false)
    }
  }

  it should "assert the burst done signal" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // read
      dut.io.mem.rd.poke(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)
      dut.clock.step()

      // burst done
      dut.io.mem.burstDone.expect(false)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.clock.step()
      dut.io.mem.burstDone.expect(true)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
    }
  }

  behavior of "write"

  it should "write to the PSRAM (burst=4)" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // write
      dut.io.mem.wr.poke(true)
      dut.io.mem.addr.poke(0x123456)
      waitForActive(dut)
      dut.io.psram.ce0_n.expect(false)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.cre.expect(false)
      dut.io.psram.adv_n.expect(false)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(false)
      dut.io.psram.addr.expect(0x12)
      dut.io.psram.din.expect(0x3456)

      // wait
      dut.io.psram.wait_n.poke(false)
      dut.clock.step()

      // data 0
      dut.io.psram.wait_n.poke(true)
      dut.io.mem.din.poke(0x1234)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.din.expect(0x1234)

      // data 1
      dut.io.mem.din.poke(0x5678)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.din.expect(0x5678)

      // data 2
      dut.io.mem.din.poke(0x90ab)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.din.expect(0x90ab)

      // data 3
      dut.io.mem.din.poke(0xcdef)
      dut.clock.step()
      dut.io.mem.burstDone.expect(true)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.din.expect(0xcdef)

      // done
      dut.io.psram.ce0_n.expect(true)
      dut.io.psram.ce1_n.expect(true)
      dut.io.psram.adv_n.expect(true)
      dut.io.psram.oe_n.expect(true)
      dut.io.psram.we_n.expect(true)
    }
  }

  it should "not assert the output enable signal" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // write
      dut.io.mem.wr.poke(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)

      // output enable
      dut.io.psram.oe_n.expect(true)
      dut.clock.step()
      dut.io.psram.oe_n.expect(true)
      dut.clock.step(6)
      dut.io.psram.oe_n.expect(true)
      dut.clock.step()
      dut.io.psram.oe_n.expect(true)
    }
  }

  it should "assert the wait signal" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // write
      dut.io.mem.wr.poke(true)
      dut.io.mem.wait_n.expect(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)

      // wait
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(false)
      dut.clock.step()
      dut.io.mem.wait_n.expect(true)
    }
  }

  it should "assert the burst done signal" in {
    test(mkPSRAM()) { dut =>
      waitForIdle(dut)

      // write
      dut.io.mem.wr.poke(true)
      waitForActive(dut)
      dut.io.psram.wait_n.poke(true)
      dut.clock.step()

      // burst done
      dut.io.mem.burstDone.expect(false)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
      dut.clock.step()
      dut.io.mem.burstDone.expect(true)
      dut.clock.step()
      dut.io.mem.burstDone.expect(false)
    }
  }
}
