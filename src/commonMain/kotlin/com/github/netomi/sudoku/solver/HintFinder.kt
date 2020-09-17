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
package com.github.netomi.sudoku.solver

import com.github.netomi.sudoku.model.*

interface HintFinder {
    val solvingTechnique: SolvingTechnique
    fun findHints(grid: Grid, hintAggregator: HintAggregator)
}

internal interface BaseHintFinder : HintFinder
{
    /**
     * Adds a direct placement hint to the `HintAggregator`.
     */
    fun placeValueInCell(grid:           Grid,
                         hintAggregator: HintAggregator,
                         cellIndex:      Int,
                         relatedCells:   CellSet,
                         value:          Int)
    {
        hintAggregator.addHint(AssignmentHint(grid.type, solvingTechnique, cellIndex, relatedCells, value))
    }

    /**
     * Adds an elimination hint to remove the given candidate value
     * from all cells in the affected house excluding cell in the excluded
     * house.
     *
     * @param affectedHouse the affected house for this elimination hint
     * @param excludedHouse the cells to be excluded from the affected house
     * @param excludedValue the candidate value to remove
     */
    fun eliminateValueFromCells(grid:           Grid,
                                hintAggregator: HintAggregator,
                                affectedHouse:  House,
                                relatedCells:   CellSet,
                                excludedHouse:  House,
                                excludedValue:  Int)
    {
        // only consider cells which have the excluded value as candidate.
        val affectedCells =
                MutableCellSet.of(grid, affectedHouse.cellsExcluding(excludedHouse)
                                                     .unassigned()
                                                     .filter { it.possibleValueSet[excludedValue] })

        val matchingCells =
                CellSet.of(grid, excludedHouse.cells.unassigned()
                                                    .filter { it.possibleValueSet[excludedValue] })

        val eliminations = MutableValueSet.of(grid, excludedValue)
        if (affectedCells.isNotEmpty) {
            hintAggregator.addHint(
                EliminationHint(grid.type,
                                solvingTechnique,
                                matchingCells,
                                eliminations,
                                relatedCells,
                                affectedCells,
                                eliminations))
        }
    }

    /**
     * Adds an elimination hint to remove all candidate values from the affected
     * cells that are not contained in the allowedValues array.
     *
     * @param affectedCells  the set of affected cell indices
     * @param allowedValues  the allowed set of candidates in the affected cells
     */
    fun eliminateNotAllowedValuesFromCells(grid:           Grid,
                                           hintAggregator: HintAggregator,
                                           affectedCells:  CellSet,
                                           allowedValues:  ValueSet,
                                           relatedCells:   CellSet)
    {
        val cellsToModify = MutableCellSet.empty(grid)
        val excludedValues: MutableList<ValueSet> = ArrayList()
        for (cell in affectedCells.cells(grid).unassigned()) {
            val valuesToExclude = valuesExcluding(cell.possibleValueSet, allowedValues)
            if (valuesToExclude.isNotEmpty) {
                cellsToModify.set(cell.cellIndex)
                excludedValues.add(valuesToExclude)
            }
        }

        if (cellsToModify.isNotEmpty) {
            hintAggregator.addHint(
                EliminationHint(grid.type,
                                solvingTechnique,
                                affectedCells,
                                allowedValues,
                                relatedCells,
                                cellsToModify,
                                excludedValues.toTypedArray()))
        }
    }

    fun eliminateValuesFromCells(grid:           Grid,
                                 hintAggregator: HintAggregator,
                                 matchingCells:  CellSet,
                                 relatedCells:   CellSet,
                                 affectedCells:  CellSet,
                                 excludedValues: ValueSet): Boolean
    {
        return eliminateValuesFromCells(grid,
                                        hintAggregator,
                                        matchingCells,
                                        excludedValues,
                                        relatedCells,
                                        affectedCells,
                                        excludedValues)
    }

    /**
     * Adds an elimination hint to remove all candidate values from the affected
     * cells (except the excluded ones) that are contained in the excludedValues bitset.
     *
     * @param affectedCells  the affected cells for this elimination hint
     * @param excludedValues the candidate value to remove
     */
    fun eliminateValuesFromCells(grid:           Grid,
                                 hintAggregator: HintAggregator,
                                 matchingCells:  CellSet,
                                 matchingValues: ValueSet,
                                 relatedCells:   CellSet,
                                 affectedCells:  CellSet,
                                 excludedValues: ValueSet): Boolean
    {
        val cellsToModify = MutableCellSet.empty(grid)
        val valuesToExcludeList: MutableList<ValueSet> = ArrayList()

        for (cell in affectedCells.cells(grid).unassigned()) {
            val valuesToExclude = valuesIncluding(cell.possibleValueSet, excludedValues)
            if (valuesToExclude.isNotEmpty) {
                cellsToModify.set(cell.cellIndex)
                valuesToExcludeList.add(valuesToExclude)
            }
        }

        return if (cellsToModify.isNotEmpty) {
            hintAggregator.addHint(
                EliminationHint(grid.type,
                                solvingTechnique,
                                matchingCells,
                                matchingValues,
                                relatedCells,
                                cellsToModify,
                                valuesToExcludeList.toTypedArray()))
            true
        } else {
            false
        }
    }

    /**
     * Adds an elimination hint to remove all candidate values from the affected
     * cells (except the excluded ones) that are contained in the excludedValues bitset.
     *
     * @param affectedCells  the affected cells for this elimination hint
     * @param excludedValues the candidate value to remove
     */
    fun eliminateValuesFromCells(grid:           Grid,
                                 hintAggregator: HintAggregator,
                                 matchingCells:  CellSet,
                                 matchingValues: ValueSet,
                                 relatedCells:   CellSet,
                                 relatedChain:   Chain,
                                 affectedCells:  CellSet,
                                 excludedValues: ValueSet): Boolean
    {
        val cellsToModify = MutableCellSet.empty(grid)
        val valuesToExcludeList: MutableList<ValueSet> = ArrayList()

        for (cell in affectedCells.cells(grid).unassigned()) {
            val valuesToExclude = valuesIncluding(cell.possibleValueSet, excludedValues)
            if (valuesToExclude.isNotEmpty) {
                cellsToModify.set(cell.cellIndex)
                valuesToExcludeList.add(valuesToExclude)
            }
        }

        return if (cellsToModify.isNotEmpty) {
            hintAggregator.addHint(
                    ChainEliminationHint(grid.type,
                                         solvingTechnique,
                                         matchingCells,
                                         matchingValues,
                                         relatedCells,
                                         relatedChain,
                                         cellsToModify,
                                         valuesToExcludeList.toTypedArray()))
            true
        } else {
            false
        }
    }

    companion object {
        /**
         * Returns a BitSet containing all values that have been set in the given bitset
         * excluding the values contained in the excludedValues array.
         */
        private fun valuesExcluding(values: ValueSet, excludedValues: ValueSet): ValueSet {
            val result = values.toMutableValueSet()
            result.andNot(excludedValues)
            return result
        }

        /**
         * Returns an array containing all values that have been set in the given bitset
         * only including the values contained in the includedValues bitset.
         */
        private fun valuesIncluding(values: ValueSet, includedValues: ValueSet): ValueSet {
            val result = includedValues.toMutableValueSet()
            result.and(values)
            return result
        }
    }
}