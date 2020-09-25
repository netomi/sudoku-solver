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
 * A [HintFinder] implementation that looks for houses where a pair
 * of cells has the same two candidates left, forming a naked pair. The
 * same candidates in other cells of the same house can be removed.
 */
open class NakedPairFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.NAKED_PAIR

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        // look first in blocks to generate fewer hints.
        (grid.blocks + grid.rows + grid.columns).unsolved().forEach { house ->
            for (cell in house.cells.biValue()) {
                val possibleValues = cell.possibleValueSet

                for (otherCell in house.cellsAfter(cell).biValue()) {
                    val otherPossibleValues = otherCell.possibleValueSet

                    // If the two [CellSet]s containing the possible candidate values
                    // have the same candidates, we have found a naked pair.
                    if (possibleValues == otherPossibleValues) {
                        val affectedCells = house.cellSet.toMutableCellSet()
                        val matchingCells = CellSet.of(cell, otherCell)
                        val relatedCells  = affectedCells.copy()

                        affectedCells.clear(cell.cellIndex)
                        affectedCells.clear(otherCell.cellIndex)
                        eliminateValuesFromCells(grid,
                                                 hintAggregator,
                                                 matchingCells,
                                                 relatedCells,
                                                 affectedCells,
                                                 possibleValues.copy())
                    }
                }
            }
        }
    }
}

/**
 * A [HintFinder] implementation that looks for houses where a subset
 * of 3 cells has the same three candidates left, forming a naked triple. The
 * candidates in other cells of the same house can be removed.
 */
class NakedTripleFinder : NakedSubsetFinder(3)
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.NAKED_TRIPLE
}

/**
 * A [HintFinder] implementation that looks for houses where a subset
 * of 4 cells has the same four candidates left, forming a naked quadruple.
 * The candidates in other cells of the same house can be removed.
 */
class NakedQuadrupleFinder : NakedSubsetFinder(4)
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.NAKED_QUADRUPLE
}

abstract class NakedSubsetFinder protected constructor(private val subSetSize: Int)
    : BaseHintFinder
{
    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        // look first in blocks to generate fewer hints.
        (grid.blocks + grid.rows + grid.columns).unsolved().forEach { house ->
            house.cells.unassigned().forEach { cell ->
                findSubset(grid,
                           hintAggregator,
                           house,
                           MutableCellSet.empty(grid),
                           cell,
                           MutableValueSet.empty(grid),
                           1)
            }
        }
    }

    private fun findSubset(grid:           Grid,
                           hintAggregator: HintAggregator,
                           house:          House,
                           visitedCells:   MutableCellSet,
                           currentCell:    Cell,
                           visitedValues:  MutableValueSet,
                           level:          Int): Boolean
    {
        if (level > subSetSize) return false

        val allVisitedValues = visitedValues.copy().or(currentCell.possibleValueSet)
        if (allVisitedValues.cardinality() > subSetSize) return false

        visitedCells.set(currentCell.cellIndex)

        if (level == subSetSize) {
            var foundHint = false
            if (allVisitedValues.cardinality() == subSetSize) {
                val affectedCells = house.cellSet.toMutableCellSet()
                val relatedCells  = affectedCells.copy()

                affectedCells.andNot(visitedCells)
                eliminateValuesFromCells(grid, hintAggregator, visitedCells.copy(), relatedCells, affectedCells, allVisitedValues)
                foundHint = true
            }
            visitedCells.clear(currentCell.cellIndex)
            return foundHint
        }

        var foundHint = false
        house.cellsAfter(currentCell).unassigned().forEach { nextCell ->
            foundHint = foundHint or findSubset(grid,
                                                hintAggregator,
                                                house,
                                                visitedCells,
                                                nextCell,
                                                allVisitedValues,
                                                level + 1)
        }

        visitedCells.clear(currentCell.cellIndex)
        return foundHint
    }
}