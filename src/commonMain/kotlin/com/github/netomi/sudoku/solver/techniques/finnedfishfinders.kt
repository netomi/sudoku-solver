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
import com.github.netomi.sudoku.solver.SolvingTechnique

class FinnedXWingFinder : FinnedFishFinder(2) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.FINNED_X_WING
}

class FinnedSwordFishFinder : FinnedFishFinder(3) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.FINNED_SWORDFISH
}

class FinnedJellyFishFinder : FinnedFishFinder(4) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.FINNED_JELLYFISH
}

abstract class FinnedFishFinder protected constructor(private val size: Int) : BaseHintFinder
{
    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        (grid.rows + grid.columns).unsolved().forEach { house ->
            val coverSetType = if (house.type == HouseType.ROW) HouseType.COLUMN else HouseType.ROW

            for (value in house.unassignedValues()) {
                findBaseSet(grid, hintAggregator, ArrayList(), house, value, coverSetType, 1)
            }
        }
    }

    private fun findBaseSet(grid:           Grid,
                            hintAggregator: HintAggregator,
                            visitedRegions: MutableList<House>,
                            house:          House,
                            value:          Int,
                            coverSetType:   HouseType,
                            level:          Int)
    {
        visitedRegions.add(house)

        try {
            if (level > size) return

            val potentialPositions = house.getPotentialPositionsAsSet(value)
            if (potentialPositions.cardinality() < 2) return

            if (level == size) {
                val fullCoverSet = MutableHouseSet.empty(grid, coverSetType)
                for (houseOfBaseSet in visitedRegions) {
                    fullCoverSet.or(getCoverSet(grid, houseOfBaseSet, houseOfBaseSet.getPotentialPositionsAsSet(value)))
                }
                // if the size of the cover set is <= the expected size, we have no fin.
                if (fullCoverSet.cardinality() <= size) return

                // This is a naive search: test any house of the base set for potential fins.
                for (finHouse in visitedRegions) {
                    val finHouses = mutableListOf(finHouse)

                    val coverSet = MutableHouseSet.empty(grid, coverSetType)
                    for (otherHouse in visitedRegions.asSequence().excluding(finHouse)) {
                        if (otherHouse.getPotentialPositionsAsSet(value).cardinality() > size) {
                            // there might be multiple houses with fins
                            finHouses.add(otherHouse)
                        } else {
                            coverSet.or(getCoverSet(grid, otherHouse, otherHouse.getPotentialPositionsAsSet(value)))
                        }
                    }

                    if (coverSet.cardinality() > size) continue

                    val finCellSet = MutableCellSet.empty(grid)
                    finHouses.forEach { finCellSet.or(getFinCells(grid, it, value, coverSet)) }

                    // get affected cells from cover sets.
                    val affectedCells = getCellsOfCoverSet(grid, house.type, coverSet)
                    val matchingCells = MutableCellSet.empty(grid)

                    // remove all cells from base sets.
                    for (region in visitedRegions) {
                        affectedCells.andNot(region.cellSet)
                        matchingCells.or(region.cellSet)
                    }

                    // only consider cover set cells that see all fins
                    for (finCell in finCellSet.cells(grid)) {
                        affectedCells.and(finCell.peerSet)
                    }

                    val excludedValue = ValueSet.of(grid, value)

                    // eliminate the detected fish value from all affected cells,
                    // affected cells = cells of cover set - cells of base set
                    eliminateValuesFromCells(grid, hintAggregator, matchingCells, matchingCells, affectedCells, excludedValue)
                }
            } else {
                grid.regionsAfter(house).unsolved().forEach { nextHouse ->
                    if (!nextHouse.assignedValueSet[value]) {
                        findBaseSet(grid, hintAggregator, visitedRegions, nextHouse, value, coverSetType, level + 1)
                    }
                }
            }
        } finally {
            visitedRegions.removeLast()
        }
    }

    private fun getFinCells(grid: Grid, houseWithFin: House, fishValue: Int, coverSet: HouseSet): CellSet {
        val finCellSet = MutableCellSet.empty(grid)
        for (cell in houseWithFin.potentialCells(fishValue)) {
            val isFin = when(coverSet.type) {
                HouseType.ROW    -> !coverSet[cell.rowIndex]
                HouseType.COLUMN -> !coverSet[cell.columnIndex]
                else -> error("unexpected house type ${coverSet.type}")
            }
            if (isFin) {
                finCellSet.set(cell.cellIndex)
            }
        }
        return finCellSet
    }

    private fun getCoverSet(grid: Grid, house: House, potentialPositions: CellSet): HouseSet {
        return when (house.type) {
            HouseType.ROW    -> potentialPositions.toColumnSet(grid)
            HouseType.COLUMN -> potentialPositions.toRowSet(grid)
            else -> error("unsupported region type $house.type")
        }
    }

    private fun getCellsOfCoverSet(grid: Grid, baseSetType: HouseType, coverSet: HouseSet): MutableCellSet {
        val affectedCells = MutableCellSet.empty(grid)
        for (i in coverSet.setBits()) {
            val house = if (baseSetType === HouseType.ROW) grid.getColumn(i) else grid.getRow(i)
            affectedCells.or(house.cellSet)
        }
        return affectedCells
    }
}