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

fun Sequence<Cell>.assigned(): Sequence<Cell> {
    return this.filter { it.isAssigned }
}

fun Sequence<Cell>.unassigned(): Sequence<Cell> {
    return this.filter { !it.isAssigned }
}

fun Sequence<Cell>.biValue(): Sequence<Cell> {
    return this.filter(Cell::isBiValue)
}

fun Sequence<Cell>.excluding(cell: Cell): Sequence<Cell> {
    return this.filter { it.cellIndex != cell.cellIndex }
}

fun Sequence<Cell>.excluding(cellSet: CellSet): Sequence<Cell> {
    return this.filter { !cellSet[it.cellIndex] }
}

fun Sequence<House>.excluding(houseSet: HouseSet): Sequence<House> {
    return this.filter { house -> house.type != houseSet.type || !houseSet[house.regionIndex] }
}

fun Sequence<House>.excluding(excludedHouse: House): Sequence<House> {
    return this.filter { house -> house !== excludedHouse }
}

fun Sequence<House>.unsolved(): Sequence<House> {
    return this.filter { !it.isSolved }
}

fun Sequence<House>.biValue(candidate: Int): Sequence<House> {
    return this.filter { house -> house.getPotentialPositionsAsSet(candidate).isBiValue }
}