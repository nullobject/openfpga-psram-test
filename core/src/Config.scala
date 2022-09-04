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

import arcadia.gfx.VideoTimingConfig
import arcadia.mem._

object Config {
  /** The system clock frequency (Hz) */
  val CLOCK_FREQ = 96000000

  val COLOR_WIDTH = 8 // BPP

  // Tile ROMs
  val TILE_ROM_ADDR_WIDTH = 17
  val TILE_ROM_DATA_WIDTH = 32
  val DEBUG_ROM_ADDR_WIDTH = 9
  val DEBUG_ROM_DATA_WIDTH = 32

  /** SDRAM configuration */
  val sdramConfig = sdram.Config(clockFreq = CLOCK_FREQ, burstLength = 2)

  /** Video timing configuration */
  val videoTimingConfig = VideoTimingConfig(
    clockFreq = 6000000,
    clockDiv = 1,
    hFreq = 15625, // Hz
    vFreq = 59.19, // Hz
    hDisplay = 256,
    vDisplay = 224,
    hFrontPorch = 40,
    vFrontPorch = 16,
    hRetrace = 32,
    vRetrace = 8,
    vOffset = 16
  )
}
