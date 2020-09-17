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
package com.github.netomi.sudoku.solver.techniques

import com.github.netomi.sudoku.model.*
import com.github.netomi.sudoku.solver.BaseHintFinder
import com.github.netomi.sudoku.solver.HintAggregator
import com.github.netomi.sudoku.solver.HintFinder
import com.github.netomi.sudoku.solver.SolvingTechnique

/**
 * A [HintFinder] implementation to look for houses which have a
 * single missing digit to place.
 */
class FullHouseFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.FULL_HOUSE

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        val expectedCardinality = grid.gridSize - 1

        grid.houses.unsolved().forEach { house ->
            val assignedValues = house.assignedValueSet
            if (assignedValues.cardinality() == expectedCardinality) {
                val value = assignedValues.firstUnsetBit()
                // Create a hint for the unassigned cell.
                val cell = house.cells.unassigned().first()
                placeValueInCell(grid, hintAggregator, cell.cellIndex, house.cellSet, value)
            }
        }
    }
}

/**
 * A [HintFinder] implementation that looks for houses where
 * a certain digit can only be placed in a single cell anymore.
 */
class HiddenSingleFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.HIDDEN_SINGLE

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        grid.houses.unsolved().forEach { house ->
            house.unassignedValues().forEach { value ->
                val possiblePositions = house.getPotentialPositionsAsSet(value)
                if (possiblePositions.cardinality() == 1) {
                    val cellIndex = possiblePositions.firstSetBit()
                    placeValueInCell(grid, hintAggregator, cellIndex, house.cellSet, value)
                }
            }
        }
    }
}

/**
 * A [HintFinder] implementation that checks if a digit can only
 * be placed in a single cell within a specific house.
 */
class NakedSingleFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.NAKED_SINGLE

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        grid.cells.unassigned().forEach { cell ->
            val possibleValues = cell.possibleValueSet
            if (possibleValues.cardinality() == 1) {
                val value = possibleValues.firstSetBit()
                placeValueInCell(grid, hintAggregator, cell.cellIndex, CellSet.of(cell), value)
            }
        }
    }
}