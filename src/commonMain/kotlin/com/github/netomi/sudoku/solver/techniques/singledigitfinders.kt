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
 * A [HintFinder] implementation that looks for a pair of a rows or columns
 * which have a candidate value with only 2 possible positions left. If a pair of
 * these positions is in the same column or row respectively, any cell seeing the
 * other two cells can not contain the candidate value.
 *
 * Every Skyscraper is also a Turbot Fish (X-Chain of cell length 4). This [HintFinder]
 * implementation creates hint patterns that are easier to spot / learn, but are conceptually
 * equivalent to an X-Chain hint.
 */
class SkyscraperFinder : BaseSingleDigitFinder()
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.SKYSCRAPER

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        (grid.rows + grid.columns).unsolved().forEach { house ->
            // find houses for which a possible value has only 2 positions left.
            for (candidate in house.unassignedValues()) {
                val potentialPositions = house.getPotentialPositionsAsSet(candidate)
                if (potentialPositions.isBiValue) {
                    findMatchingHouse(grid, hintAggregator, house, potentialPositions, candidate)
                }
            }
        }
    }

    override fun getSingleHouse(grid: Grid, house: House, cellSet: CellSet): House? {
        return if (house.type == HouseType.ROW) cellSet.getSingleColumn(grid) else cellSet.getSingleRow(grid)
    }

    override fun otherHouses(grid: Grid, house: House): Sequence<House> {
        return grid.regionsAfter(house)
    }
}

/**
 * A [HintFinder] implementation that looks for a pair of a row and a column
 * which have a candidate value with only 2 possible positions left. If a pair of
 * these positions is in the same block, any cell seeing the other two cells
 * can not contain the candidate value.
 *
 * Every 2-String Kite is also a Turbot Fish (X-Chain of cell length 4). This [HintFinder]
 * implementation creates hint patterns that are easier to spot / learn, but are conceptually
 * equivalent to an X-Chain hint.
 */
class TwoStringKiteFinder : BaseSingleDigitFinder()
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.TWO_STRING_KITE

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        grid.rows.unsolved().forEach { row ->
            // find houses for which a possible value has only 2 positions left.
            for (candidate in row.unassignedValues()) {
                val potentialPositions = row.getPotentialPositionsAsSet(candidate)
                if (potentialPositions.isBiValue) {
                    findMatchingHouse(grid, hintAggregator, row, potentialPositions, candidate)
                }
            }
        }
    }

    override fun getSingleHouse(grid: Grid, house: House, cellSet: CellSet): House? {
        return cellSet.getSingleBlock(grid)
    }

    override fun otherHouses(grid: Grid, house: House): Sequence<House> {
        return grid.columns
    }
}

abstract class BaseSingleDigitFinder : BaseHintFinder
{
    protected abstract fun otherHouses(grid: Grid, house: House): Sequence<House>

    protected fun findMatchingHouse(grid:               Grid,
                                    hintAggregator:     HintAggregator,
                                    house:              House,
                                    potentialPositions: CellSet,
                                    candidate:          Int)
    {
        for (otherHouse in otherHouses(grid, house).unsolved()) {
            val assignedValues = otherHouse.assignedValueSet
            if (assignedValues[candidate]) continue

            val potentialOtherPositions = otherHouse.getPotentialPositionsAsSet(candidate)
            if (potentialOtherPositions.isNotBiValue) continue

            // check that the position sets are mutually exclusive.
            val combinedPositions = potentialPositions.toMutableCellSet()
            combinedPositions.and(potentialOtherPositions)
            if (combinedPositions.isNotEmpty) continue

            checkMatchingHouse(grid, hintAggregator, house, potentialPositions, otherHouse, potentialOtherPositions, candidate)
        }
    }

    protected abstract fun getSingleHouse(grid: Grid, house: House, cellSet: CellSet): House?

    private fun checkMatchingHouse(grid:                    Grid,
                                   hintAggregator:          HintAggregator,
                                   house:                   House,
                                   potentialPositions:      CellSet,
                                   otherHouse:              House,
                                   potentialOtherPositions: CellSet,
                                   candidate:               Int)
    {
        for (cellInFirstHouse in potentialPositions.cells(grid)) {
            for (cellInSecondHouse in potentialOtherPositions.cells(grid)) {
                val combinedSet = CellSet.of(cellInFirstHouse, cellInSecondHouse)
                val singleHouse = getSingleHouse(grid, house, combinedSet)

                // we found a pair of cells from the two houses that are in the same house.
                singleHouse?.apply {
                    val otherRowCell = potentialPositions.cells(grid).excluding(cellInFirstHouse).first()
                    val otherColCell = potentialOtherPositions.cells(grid).excluding(cellInSecondHouse).first()

                    // find all cells that see both, the cell in the matching houses.
                    val affectedCells = otherRowCell.peerSet.toMutableCellSet()
                    affectedCells.and(otherColCell.peerSet)

                    val excludedValues = ValueSet.of(grid, candidate)

                    val matchingCells = potentialPositions.toMutableCellSet()
                    matchingCells.or(potentialOtherPositions)

                    val relatedCells = house.cellSet.toMutableCellSet()
                    relatedCells.or(otherHouse.cellSet)

                    eliminateValuesFromCells(grid, hintAggregator, matchingCells, relatedCells, affectedCells, excludedValues)
                }
            }
        }
    }
}