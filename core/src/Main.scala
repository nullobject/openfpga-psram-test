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

package core

import arcadia._
import arcadia.gfx._
import arcadia.mem._
import arcadia.mem.arbiter.BurstMemArbiter
import arcadia.mem.buffer.BurstBuffer
import arcadia.mem.sdram.{SDRAM, SDRAMIO}
import arcadia.pocket.Bridge
import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util._

/** The top-level module. */
class Main extends Module {
  val io = IO(new Bundle {
    /** SDRAM port */
    val sdram = SDRAMIO(Config.sdramConfig)
    /** Video clock */
    val videoClock = Input(Clock())
    /** Video port */
    val video = Output(new VideoIO)
    /** RGB output */
    val rgb = Output(RGB(Config.COLOR_WIDTH.W))
    /** Bridge port */
    val bridge = new Bridge
  })

  // States
  object State {
    val write :: read :: readWait :: next :: Nil = Enum(4)
  }

  val stateReg = RegInit(State.write)
  val addrReg = RegInit(0.U(16.W))
  val (_, wrap) = Counter(stateReg === State.next, Config.CLOCK_FREQ / 2)

  // SDRAM
  val sdram = Module(new SDRAM(Config.sdramConfig))
  sdram.io.sdram <> io.sdram

  // Download buffer
  val downloadBuffer = Module(new BurstBuffer(buffer.Config(
    inAddrWidth = Bridge.ADDR_WIDTH,
    inDataWidth = Bridge.DATA_WIDTH,
    outAddrWidth = Config.sdramConfig.addrWidth,
    outDataWidth = Config.sdramConfig.dataWidth,
    burstLength = Config.sdramConfig.burstLength
  )))
  downloadBuffer.io.in <> io.bridge.rom

  // Arbiter
  val arbiter = Module(new BurstMemArbiter(2, Config.sdramConfig.addrWidth, Config.sdramConfig.dataWidth))
  arbiter.io.in(0) <> downloadBuffer.io.out.asBurstMemIO
  arbiter.io.in(1).rd := stateReg === State.read
  arbiter.io.in(1).wr := false.B
  arbiter.io.in(1).burstLength := Config.sdramConfig.burstLength.U
  arbiter.io.in(1).addr := addrReg
  arbiter.io.in(1).din := DontCare
  arbiter.io.in(1).mask := DontCare
  arbiter.io.out <> sdram.io.mem

  val dataReg = RegEnable(arbiter.io.in(1).dout, stateReg === State.readWait && arbiter.io.in(1).valid)

  switch(stateReg) {
    is(State.write) {
      when(io.bridge.done) {
        stateReg := State.read
      }
    }
    is(State.read) {
      when(!sdram.io.mem.waitReq) {
        stateReg := State.readWait
      }
    }
    is(State.readWait) {
      when(sdram.io.mem.burstDone) {
        stateReg := State.next
      }
    }
    is(State.next) {
      when(wrap) {
        addrReg := addrReg + 1.U
        stateReg := State.read
      }
    }
  }

  // Video timing
  withClock(io.videoClock) {
   val videoTiming = Module(new VideoTiming(Config.videoTimingConfig))
    videoTiming.io.offset := SVec2(0.S, 0.S)
    val video = videoTiming.io.timing

    // The debug ROM contains alphanumeric character tiles
    val debugRom = Module(new SinglePortRom(
      addrWidth = Config.DEBUG_ROM_ADDR_WIDTH,
      dataWidth = Config.DEBUG_ROM_DATA_WIDTH,
      depth = 512,
      initFile = "roms/alpha.mif"
    ))

    // Address text
    val addrText = Module(new DebugLayer("ADDR: $%04X\nDATA: $%04X"))
    addrText.io.args := Seq(addrReg, dataReg)
    addrText.io.pos := UVec2(100.U, 100.U)
    addrText.io.color := 0xf.U
    addrText.io.tileRom <> debugRom.io
    addrText.io.video <> video

    val pixel = addrText.io.data

    val rgb = RGB(pixel, pixel, pixel)
    val black = RGB(0.U(Config.COLOR_WIDTH.W), 0.U(Config.COLOR_WIDTH.W), 0.U(Config.COLOR_WIDTH.W))

    // Video output
    io.video := RegNext(video)
    io.rgb := RegNext(Mux(video.displayEnable, rgb, black))
  }
}

object Main extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/core"),
    Seq(ChiselGeneratorAnnotation(() => new Main))
  )
}
