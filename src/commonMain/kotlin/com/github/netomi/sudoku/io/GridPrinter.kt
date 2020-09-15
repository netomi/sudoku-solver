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
package com.github.netomi.sudoku.io

import com.github.netomi.sudoku.model.Grid
import com.github.netomi.sudoku.model.GridVisitor

class GridPrinter constructor(private val style: STYLE, private val appendable: Appendable)
    : GridVisitor<Grid>
{
    enum class STYLE {
        ONE_LINE, SIMPLE
    }

    override fun visitGrid(grid: Grid): Grid {
        when (style) {
            STYLE.ONE_LINE -> printOnelineGrid(grid)
            STYLE.SIMPLE   -> printSimpleGrid(grid)
        }
        return grid
    }

    private fun printOnelineGrid(grid: Grid) {
        grid.cells().forEach { cell -> appendable.appendLine(cell.value.toString()) }
    }

    private fun printSimpleGrid(grid: Grid) {
        for (row in grid.rows()) {
            row.cells().forEach { cell -> appendable.append(cell.value.toString()) }
            appendable.appendLine()
        }
    }
}