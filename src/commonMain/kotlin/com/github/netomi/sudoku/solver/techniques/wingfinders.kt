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
 * A [HintFinder] implementation ...
 */
class XYWingFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.XY_WING

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        for (cell in grid.unassignedCells()) {
            val possibleValueSet = cell.possibleValueSet
            if (possibleValueSet.cardinality() != 2) continue

            val biValueCell: (Cell) -> Boolean = { c -> c.possibleValueSet.cardinality() == 2 }

            for (pincerOne in cell.peers().filter(biValueCell)) {
                val xz = getXZ(possibleValueSet, pincerOne.possibleValueSet)

                xz?.apply {
                    val x = this.x
                    val z = this.z

                    for (pincerTwo in cell.peerSet.allCells(grid, pincerOne.cellIndex + 1).filter(biValueCell)) {
                        val yz = getXZ(possibleValueSet, pincerTwo.possibleValueSet)

                        yz?.apply {
                            val y = this.x
                            val z2 = this.z

                            if (x != y && z == z2) {
                                foundXYWing(grid, hintAggregator, cell, pincerOne, pincerTwo, cell.peerSet.copy(), z)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun foundXYWing(grid:           Grid,
                            hintAggregator: HintAggregator,
                            pivotCell:      Cell,
                            pincerOne:      Cell,
                            pincerTwo:      Cell,
                            relatedCells:   CellSet,
                            z:              Int)
    {
        val matchingCells = MutableCellSet.of(pivotCell, pincerOne, pincerTwo)
        val matchingValues = pivotCell.possibleValueSet.copy()

        val affectedCells = pincerOne.peerSet.toMutableCellSet()
        affectedCells.and(pincerTwo.peerSet)

        val excludedValues = MutableValueSet.of(grid, z)

        // TODO: highlight the z values, an elimination hint does not yet support this information
        eliminateValuesFromCells(grid, hintAggregator, matchingCells, matchingValues, relatedCells, affectedCells, excludedValues)
    }

    private fun getXZ(pivotCandidates: ValueSet, pincerCandidates: ValueSet): XZ? {
        if (pincerCandidates.cardinality() != 2) return null

        var tempValues = pincerCandidates.toMutableValueSet()
        tempValues.andNot(pivotCandidates)

        if (tempValues.cardinality() != 1) return null

        val zCandidate = tempValues.firstSetBit()

        tempValues = pincerCandidates.toMutableValueSet()
        tempValues.clear(zCandidate)

        val xCandidate = tempValues.firstSetBit()

        return XZ(xCandidate, zCandidate)
    }

    private class XZ(val x: Int, val z: Int)
}
