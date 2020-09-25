/*
 * Sudoku creator / solver / teacher.
 *
 * Copyright (c) 2020 Thomas Neidhart
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.github.netomi.sudoku.model

import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HouseSetTest
{
    @Test
    fun houseTypeRow() {
        val grid = Grid.of(GridType.CLASSIC_9x9)

        val houseSet = MutableHouseSet.empty(grid, HouseType.ROW)
        houseSet.set(1)

        assertTrue(houseSet.houses(grid).count() == 1)
        assertSame(grid.getRow(1), houseSet.houses(grid).first())
    }

    @Test
    fun houseTypeColumn() {
        val grid = Grid.of(GridType.CLASSIC_9x9)

        val houseSet = MutableHouseSet.empty(grid, HouseType.COLUMN)
        houseSet.set(0)
        houseSet.set(5)
        houseSet.set(7)

        assertTrue(houseSet.houses(grid).count() == 3)
        assertSame(grid.getColumn(0), houseSet.houses(grid).first())
    }
}