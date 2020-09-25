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

import kotlin.test.*

class GridTest
{
    @Test
    fun cellSequences() {
        val grid = Grid.of(GridType.CLASSIC_9x9)
        val row  = grid.getRow(0)

        assertEquals(grid.gridSize, countItems(row.cells))
        grid.getCell(0).value = 1
        assertEquals(grid.gridSize, countItems(row.cells))
        assertEquals(grid.gridSize - 1, countItems(row.cells.unassigned()))
        assertEquals(1, countItems(row.cellsAfter(grid.getCell(7)).unassigned()))

        for (i in 0 until grid.gridSize) {
            grid.getCell(i).value = i + 1
        }

        assertEquals(grid.gridSize, countItems(row.cells))
        assertEquals(0, countItems(row.cells.unassigned()))
        assertEquals(0, countItems(row.cellsAfter(grid.getCell(7)).unassigned()))
    }

    @Test
    fun houseSequences() {
        val grid = Grid.of(GridType.CLASSIC_9x9)

        assertEquals(9 * 3, grid.houses.count())

        // excluding some houses
        val houseSet = MutableHouseSet.empty(grid, HouseType.ROW)
        houseSet.set(3)

        // sequence of all columns excluding some row should not exclude anything
        assertEquals(9, grid.columns.excluding(houseSet).count())

        // the row with index 3 should be excluded
        assertEquals(8, grid.rows.excluding(houseSet).count())
        assertFalse(grid.rows.excluding(houseSet).contains(grid.getRow(3)))
    }

    companion object {
        private fun <T> countItems(sequence: Sequence<T>): Int {
            return sequence.count()
        }
    }
}