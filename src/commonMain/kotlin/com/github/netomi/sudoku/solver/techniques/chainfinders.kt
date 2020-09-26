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
class RemotePairFinder : BaseChainFinder() {
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.REMOTE_PAIR

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        val visitedChains: MutableSet<CellSet> = HashSet()
        grid.cells.unassigned().biValue().forEach { cell ->
            val possibleValues = cell.possibleValueSet

            // initial chain setup.
            // the choice of the initial chain candidate does not matter as the
            // respective chains are symmetric. For simplicity reasons, we chose
            // the first value.
            val firstCandidate = possibleValues.firstSetBit()
            val chain = Chain(grid, cell.cellIndex, firstCandidate)
            chain.addLink(LinkType.STRONG, cell.cellIndex, possibleValues.otherSetBit(firstCandidate))

            findChain(grid, hintAggregator, cell, chain, visitedChains, 1)
        }
    }

    private fun findChain(grid:           Grid,
                          hintAggregator: HintAggregator,
                          currentCell:    Cell,
                          currentChain:   Chain,
                          visitedChains:  MutableSet<CellSet>,
                          cellCount:      Int)
    {
        // make sure we do not add chains twice: in forward and reverse order.
        if (visitedChains.contains(currentChain.cellSet)) return

        // to find a remote pair, the chain has to include at least
        // 4 cells. Also the start / end points of the chain need to
        // have opposite active states.
        if (cellCount >= 4 && cellCount % 2 == 0) {
            val matchingCells = addChainEliminationHint(grid, hintAggregator, currentCell, currentChain, currentCell.possibleValueSet.copy())
            matchingCells?.apply { visitedChains.add(this) }
        }

        for (nextCell in currentCell.peers.unassigned()) {
            if (currentChain.contains(nextCell)) continue

            val possibleValues           = currentCell.possibleValueSet
            val possibleValuesOfNextCell = nextCell.possibleValueSet

            if (possibleValuesOfNextCell.isNotBiValue ||
                possibleValues != possibleValuesOfNextCell) continue

            val linkedCandidate = currentChain.lastNode.candidate
            currentChain.addLink(LinkType.WEAK, nextCell.cellIndex, linkedCandidate)
            val otherCandidate = possibleValuesOfNextCell.filter { it != linkedCandidate }.first()
            currentChain.addLink(LinkType.STRONG, nextCell.cellIndex, otherCandidate)

            findChain(grid, hintAggregator, nextCell, currentChain, visitedChains, cellCount + 1)

            currentChain.removeLastLink()
            currentChain.removeLastLink()
        }
    }
}

open class XChainFinder protected constructor(private val maxCellCount: Int) : BaseChainFinder()
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.X_CHAIN

    constructor() : this(10)

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        val visitedChains: MutableSet<CellSet> = HashSet()
        grid.cells.unassigned().forEach { cell ->
            cell.possibleValueSet.forEach { value ->
                val chain = Chain(grid, cell.cellIndex, value)
                findChain(grid, hintAggregator, cell, chain, visitedChains, 1)
            }
        }
    }

    private fun findChain(grid:           Grid,
                          hintAggregator: HintAggregator,
                          currentCell:    Cell,
                          currentChain:   Chain,
                          visitedChains:  MutableSet<CellSet>,
                          cellCount:      Int)
    {
        // make sure we do not add chains twice: in forward and reverse order.
        if (visitedChains.contains(currentChain.cellSet)) return

        val chainCandidate = currentChain.lastNode.candidate

        // to find a x-chain, the chain has to start and end with a strong link.
        if (cellCount in 4..maxCellCount && currentChain.lastLinkType() == LinkType.STRONG) {
            val excludedValues = ValueSet.of(grid, chainCandidate)
            val matchingCells = addChainEliminationHint(grid, hintAggregator, currentCell, currentChain, excludedValues)
            matchingCells?.apply { visitedChains.add(this) }
        }

        if (cellCount >= maxCellCount) return

        val nextLinkType = currentChain.lastLinkType()?.opposite() ?: LinkType.STRONG

        for (house in currentCell.houses) {
            val potentialPositionSet = house.getPotentialPositionsAsSet(chainCandidate)
            if (potentialPositionSet.cardinality() <= 1) continue

            // if the number of possible positions within a house is equal to 2 we have found
            // a strong link, otherwise its a weak link. Strong links can be downgraded to weak
            // links if needed.
            val possibleLinkType = if (potentialPositionSet.isBiValue) LinkType.STRONG else LinkType.WEAK

            // if the house does not contain a link of the expected type,
            // no need to look at individual cells. Strong links can be downgraded
            // to weak links if needed.
            if (possibleLinkType < nextLinkType) continue

            for (nextCell in house.potentialCells(chainCandidate)) {
                if (currentChain.contains(nextCell)) continue

                currentChain.addLink(nextLinkType, nextCell.cellIndex, chainCandidate)
                findChain(grid, hintAggregator, nextCell, currentChain, visitedChains, cellCount + 1)
                currentChain.removeLastLink()
            }
        }
    }
}

class XYChainFinder constructor(private val maxCellCount: Int): BaseChainFinder()
{
    override val solvingTechnique: SolvingTechnique
        get() = SolvingTechnique.XY_CHAIN

    constructor() : this(10)

    override fun findHints(grid: Grid, hintAggregator: HintAggregator) {
        val visitedChains: MutableMap<CellSet, MutableSet<Int>> = HashMap()
        grid.cells.unassigned().biValue().forEach { cell ->
            for (value in cell.possibleValueSet) {
                val chain = Chain(grid, cell.cellIndex, value)
                val linkedCandidate = cell.possibleValueSet.otherSetBit(value)
                chain.addLink(LinkType.STRONG, cell.cellIndex, linkedCandidate)

                findChain(grid, hintAggregator, cell, chain, visitedChains, 1)
            }
        }
    }

    private fun findChain(grid:           Grid,
                          hintAggregator: HintAggregator,
                          currentCell:    Cell,
                          currentChain:   Chain,
                          visitedChains:  MutableMap<CellSet, MutableSet<Int>>,
                          cellCount:      Int)
    {
        // make sure we do not add chains twice: in forward and reverse order taking
        // into account the starting candidate.
        var candidateSet = visitedChains[currentChain.cellSet]
        if(candidateSet?.contains(currentChain.rootNode.candidate) == true) return

        // to find a xy-chain, the chain has to start and end with a strong link.
        if (cellCount in 2..maxCellCount &&
            currentChain.lastLinkType() == LinkType.STRONG &&
            currentChain.lastNode.candidate == currentChain.rootNode.candidate)
        {
            val excludedValues = ValueSet.of(grid, currentChain.rootNode.candidate)
            val matchingCells = addChainEliminationHint(grid, hintAggregator, currentCell, currentChain, excludedValues)
            matchingCells?.apply {
                candidateSet = visitedChains.getOrPut(this, { mutableSetOf() })
                candidateSet!!.add(currentChain.rootNode.candidate)
            }
        }

        if (cellCount >= maxCellCount) return

        for (nextCell in currentCell.peers) {
            if (currentChain.contains(nextCell)) continue

            val linkedCandidate = currentChain.lastNode.candidate
            val possibleValuesOfNextCell = nextCell.possibleValueSet

            if (possibleValuesOfNextCell.isNotBiValue ||
                !possibleValuesOfNextCell[linkedCandidate]) continue

            currentChain.addLink(LinkType.WEAK, nextCell.cellIndex, linkedCandidate)
            val otherCandidate = possibleValuesOfNextCell.filter { it != linkedCandidate }.first()
            currentChain.addLink(LinkType.STRONG, nextCell.cellIndex, otherCandidate)

            findChain(grid, hintAggregator, nextCell, currentChain, visitedChains, cellCount + 1)

            currentChain.removeLastLink()
            currentChain.removeLastLink()
        }
    }
}

abstract class BaseChainFinder : BaseHintFinder
{
    protected fun addChainEliminationHint(grid:           Grid,
                                          hintAggregator: HintAggregator,
                                          currentCell:    Cell,
                                          currentChain:   Chain,
                                          excludedValues: ValueSet) : CellSet?
    {
        val affectedCells = currentCell.peerSet.toMutableCellSet().andNot(currentChain.cellSet)

        for (affectedCell in affectedCells.cells(grid)) {
            val startCell = currentChain.rootNode.cellIndex
            val endPoints = CellSet.of(grid.getCell(startCell), currentCell)

            val peers = affectedCell.peerSet.toMutableCellSet().and(endPoints)

            // if the cell does not see both endpoints,
            // it can not be considered for elimination.
            if (peers.cardinality() < 2) {
                affectedCells.clear(affectedCell.cellIndex)
            }
        }

        val matchingCells = currentChain.cellSet.copy()
        return if (eliminateValuesFromCells(grid,
                                            hintAggregator,
                                            matchingCells,
                                            excludedValues,
                                            affectedCells,
                                            currentChain.copy(),
                                            affectedCells,
                                            excludedValues)) {
            matchingCells
        } else {
            null
        }
    }
}