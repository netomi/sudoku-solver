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

class XWingHintFinder : BasicFishFinder(2) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.X_WING
}

class SwordFishFinder : BasicFishFinder(3) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.SWORDFISH
}

class JellyFishFinder : BasicFishFinder(4) {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.JELLYFISH
}

/**
 * A [HintFinder] implementation that looks for houses
 * where a subset of candidates is constrained to some cells,
 * forming a hidden subset. All other candidates in these cells
 * can be removed.
 */
abstract class BasicFishFinder protected constructor(private val size: Int) : BaseHintFinder
{
    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        (grid.rows + grid.columns).unsolved().forEach { house ->
            val coverSetType = if (house.type == HouseType.ROW) HouseType.COLUMN else HouseType.ROW

            for (value in house.unassignedValues()) {
                val coverSet = MutableHouseSet.empty(grid, coverSetType)
                findBaseSet(grid, hintAggregator, ArrayList(), house, value, coverSet, 1)
            }
        }
    }

    private fun findBaseSet(grid:           Grid,
                            hintAggregator: HintAggregator,
                            visitedRegions: MutableList<House>,
                            house:          House,
                            value:          Int,
                            coverSet:       MutableHouseSet,
                            level:          Int)
    {
        visitedRegions.add(house)

        try {
            if (level > size) return

            val potentialPositions = house.getPotentialPositionsAsSet(value)
            if (potentialPositions.cardinality() < 2 ||
                potentialPositions.cardinality() > size) return

            val mergedCoverSet = coverSet.copy()
            mergedCoverSet.or(getCoverSet(grid, house, potentialPositions))
            if (mergedCoverSet.cardinality() > size) return

            if (level == size) {
                // get affected cells from the cover sets.
                val affectedCells = getCellsOfCoverSet(grid, house.type, mergedCoverSet)
                val matchingCells = MutableCellSet.empty(grid)

                // remove all cells from base sets.
                for (region in visitedRegions) {
                    affectedCells.andNot(region.cellSet)
                    matchingCells.or(region.cellSet)
                }

                val excludedValue = ValueSet.of(grid, value)

                // eliminate the detected fish value from all affected cells,
                // affected cells = cells of cover set - cells of base set
                eliminateValuesFromCells(grid, hintAggregator, matchingCells, matchingCells, affectedCells, excludedValue)
            } else {
                grid.regionsAfter(house).unsolved().forEach { nextHouse ->
                    if (!nextHouse.assignedValueSet[value]) {
                        findBaseSet(grid, hintAggregator, visitedRegions, nextHouse, value, mergedCoverSet, level + 1)
                    }
                }
            }
        } finally {
            visitedRegions.removeLast()
        }
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