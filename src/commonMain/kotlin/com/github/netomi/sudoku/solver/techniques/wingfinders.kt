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
import com.github.netomi.sudoku.solver.*
import com.github.netomi.sudoku.solver.BaseHintFinder

/**
 * A [HintFinder] implementation ...
 */
class XYWingFinder : BaseWingFinder()
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.XY_WING

    override fun isPotentialPivotCell(cell: Cell): Boolean {
        return cell.isBiValue
    }

    override fun getXYZ(pivotCell: Cell, pincerOne: Cell, pincerTwo: Cell): XYZ? {
        val z = getZ(pivotCell, pincerOne) ?: return null

        var tmp = pincerOne.possibleValueSet.toMutableValueSet()
        if (!tmp[z]) return null
        tmp.clear(z)
        if (tmp.cardinality() != 1) return null
        val x = tmp.firstSetBit()

        tmp = pincerTwo.possibleValueSet.toMutableValueSet()
        if (!tmp[z]) return null
        tmp.clear(z)
        if (tmp.cardinality() != 1) return null
        val y = tmp.firstSetBit()

        val pivotValueSet = pivotCell.possibleValueSet
        return if (x != y && pivotValueSet[x] && pivotValueSet[y]) XYZ(x, y, z) else null
    }
}

/**
 * A [HintFinder] implementation ...
 */
class XYZWingFinder : BaseWingFinder()
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.XYZ_WING

    override fun isPotentialPivotCell(cell: Cell): Boolean {
        return cell.possibleValueSet.cardinality() == 3
    }

    override fun getAffectedCells(pivotCell: Cell, pincerOne: Cell, pincerTwo: Cell): MutableCellSet {
        val affectedCells = super.getAffectedCells(pivotCell, pincerOne, pincerTwo)
        affectedCells.and(pivotCell.peerSet)
        return affectedCells
    }

    override fun getXYZ(pivotCell: Cell, pincerOne: Cell, pincerTwo: Cell): XYZ? {
        var tmp = pivotCell.possibleValueSet.toMutableValueSet()
        tmp.and(pincerOne.possibleValueSet)
        tmp.and(pincerTwo.possibleValueSet)

        if (tmp.cardinality() != 1) return null
        val z = tmp.firstSetBit()

        tmp = pincerOne.possibleValueSet.toMutableValueSet()
        tmp.clear(z)
        if (tmp.cardinality() != 1) return null
        val x = tmp.firstSetBit()

        tmp = pincerTwo.possibleValueSet.toMutableValueSet()
        tmp.clear(z)
        if (tmp.cardinality() != 1) return null
        val y = tmp.firstSetBit()

        val pivotValueSet = pivotCell.possibleValueSet
        return if (x != y && pivotValueSet[x] && pivotValueSet[y]) XYZ(x, y, z) else null
    }
}

/**
 * A [HintFinder] implementation ...
 */
class WWingFinder : BaseHintFinder
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.W_WING

    private fun isPotentialPivotCell(cell: Cell): Boolean {
        return cell.isBiValue
    }

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        for (pivotCell in grid.cells.unassigned().filter(this::isPotentialPivotCell)) {
            for (candidate in pivotCell.possibleValues) {
                for (peerCell in pivotCell.peers.unassigned().filter { it.possibleValueSet[candidate] }) {
                    for (linkedCell in getStronglyLinkedCells(peerCell, candidate)) {
                        for (endCell in getEndCells(pivotCell, linkedCell)) {
                            val matchingCells = MutableCellSet.of(pivotCell, endCell, peerCell, linkedCell)
                            val matchingValues = pivotCell.possibleValueSet.copy()

                            val affectedCells = getCombinedPeers(pivotCell, endCell)
                            affectedCells.andNot(matchingCells)

                            val excludedValues = pivotCell.possibleValueSet.toMutableValueSet()
                            excludedValues.clear(candidate)

                            // TODO: highlight strongly linked values, an elimination hint does not yet support this information
                            eliminateValuesFromCells(grid, hintAggregator, matchingCells, matchingValues, MutableCellSet.empty(grid), affectedCells, excludedValues)
                        }
                    }
                }
            }
        }
    }

    private fun getCombinedPeers(cell: Cell, otherCell: Cell): MutableCellSet {
        val peers = cell.peerSet.toMutableCellSet()
        peers.and(otherCell.peerSet)
        return peers
    }

    private fun getStronglyLinkedCells(cell: Cell, candidate: Int): Sequence<Cell> {
        val cellList = mutableListOf<Cell>()

        for (house in cell.houses) {
            val potentialPositionSet = house.getPotentialPositionsAsSet(candidate)
            if (potentialPositionSet.isNotBiValue) continue

            cellList.addAll(house.cellsExcluding(cell)
                                 .unassigned()
                                 .filter { it.possibleValueSet[candidate] })
        }

        return cellList.asSequence()
    }

    private fun getEndCells(pivotCell: Cell, linkedCell: Cell): Sequence<Cell> {
        val cellList = mutableListOf<Cell>()

        cellList.addAll(linkedCell.peers.unassigned()
                                        .excluding(pivotCell)
                                        .filter { pivotCell.possibleValueSet == it.possibleValueSet })

        return cellList.asSequence()
    }
}

abstract class BaseWingFinder : BaseHintFinder
{
    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        for (pivotCell in grid.cells.unassigned().filter(this::isPotentialPivotCell)) {
            for (pincerOne in pivotCell.peers.biValue()) {
                if (pivotCell.possibleValueSet.intersects(pincerOne.possibleValueSet)) {
                    for (pincerTwo in pivotCell.peersAfter(pincerOne).biValue()) {
                        val xyz = getXYZ(pivotCell, pincerOne, pincerTwo)
                        xyz?.apply {
                            foundWing(grid, hintAggregator, pivotCell, pincerOne, pincerTwo, pivotCell.peerSet.copy(), xyz)
                        }
                    }
                }
            }
        }
    }

    protected abstract fun isPotentialPivotCell(cell: Cell): Boolean

    protected abstract fun getXYZ(pivotCell: Cell, pincerOne: Cell, pincerTwo: Cell): XYZ?

    protected open fun getAffectedCells(pivotCell: Cell, pincerOne: Cell, pincerTwo: Cell): MutableCellSet {
        val affectedCells = pincerOne.peerSet.toMutableCellSet()
        affectedCells.and(pincerTwo.peerSet)
        return affectedCells
    }

    protected fun getZ(pivotCell: Cell, pincerCell: Cell): Int? {
        val tempValues = pincerCell.possibleValueSet.toMutableValueSet()
        tempValues.andNot(pivotCell.possibleValueSet)

        return if (tempValues.cardinality() == 1) tempValues.firstSetBit() else null
    }

    private fun foundWing(grid:           Grid,
                          hintAggregator: HintAggregator,
                          pivotCell:      Cell,
                          pincerOne:      Cell,
                          pincerTwo:      Cell,
                          relatedCells:   CellSet,
                          xyz:            XYZ)
    {
        val matchingCells = MutableCellSet.of(pivotCell, pincerOne, pincerTwo)
        val matchingValues = pivotCell.possibleValueSet.copy()

        val affectedCells = getAffectedCells(pivotCell, pincerOne, pincerTwo)
        affectedCells.andNot(matchingCells)

        val excludedValues = MutableValueSet.of(grid, xyz.z)

        // TODO: highlight the z values, an elimination hint does not yet support this information
        eliminateValuesFromCells(grid, hintAggregator, matchingCells, matchingValues, relatedCells, affectedCells, excludedValues)
    }

    protected class XYZ(val x: Int, val y: Int, val z: Int)
}