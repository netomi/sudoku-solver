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
 * A [HintFinder] implementation that looks for houses
 * where a pair of candidates is constrained to the same two
 * cells, forming a hidden pair. Any other candidate in these
 * cells can be removed.
 */
class HiddenPairFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.HIDDEN_PAIR

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        grid.houses.unsolved().forEach { house ->
            for (value in house.unassignedValues()) {
                val potentialPositions = house.getPotentialPositionsAsSet(value)
                if (potentialPositions.isNotBiValue) continue

                for (otherValue in house.unassignedValuesAfter(value)) {
                    val otherPotentialPositions: CellSet = house.getPotentialPositionsAsSet(otherValue)
                    if (otherPotentialPositions.isNotBiValue) continue

                    // If the two bitsets, containing the possible positions for some values,
                    // share the exact same positions, we have found a hidden pair.
                    if (potentialPositions == otherPotentialPositions) {
                        val allowedValues = ValueSet.of(grid, value, otherValue)
                        eliminateNotAllowedValuesFromCells(grid, hintAggregator, potentialPositions, allowedValues, house.cellSet)
                    }
                }
            }
        }
    }
}

/**
 * A [HintFinder] implementation that looks for houses
 * where a subset of 3 candidates is constrained to three cells,
 * forming a hidden triple. All other candidates in these cells
 * can be removed.
 */
class HiddenTripleFinder : HiddenSubsetFinder(3) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.HIDDEN_TRIPLE
}

/**
 * A [HintFinder] implementation that looks for houses
 * where a subset of 4 candidates is constrained to four cells,
 * forming a hidden quadruple. All other candidates in these cells
 * can be removed.
 */
class HiddenQuadrupleFinder : HiddenSubsetFinder(4) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.HIDDEN_QUADRUPLE
}

abstract class HiddenSubsetFinder protected constructor(private val subSetSize: Int) : BaseHintFinder
{
    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        grid.houses.unsolved().forEach { house ->
            for (value in house.unassignedValues()) {
                findSubset(grid, hintAggregator, house, MutableValueSet.empty(grid), value, MutableCellSet.empty(grid), 1)
            }
        }
    }

    private fun findSubset(grid:             Grid,
                           hintAggregator:   HintAggregator,
                           house:            House,
                           visitedValues:    MutableValueSet,
                           currentValue:     Int,
                           visitedPositions: MutableCellSet,
                           level:            Int)
    {
        visitedValues.set(currentValue)

        try {
            if (level > subSetSize) return

            val potentialPositions    = house.getPotentialPositionsAsSet(currentValue)
            val allPotentialPositions = visitedPositions.copy()

            allPotentialPositions.or(potentialPositions)

            if (allPotentialPositions.cardinality() > subSetSize) return

            if (level == subSetSize) {
                if (allPotentialPositions.cardinality() == subSetSize) {
                    eliminateNotAllowedValuesFromCells(grid, hintAggregator, allPotentialPositions, visitedValues.copy(), house.cellSet)
                }
            } else {
                for (nextValue in house.unassignedValuesAfter(currentValue)) {
                    findSubset(grid, hintAggregator, house, visitedValues, nextValue, allPotentialPositions, level + 1)
                }
            }
        } finally {
            visitedValues.clear(currentValue)
        }
    }
}