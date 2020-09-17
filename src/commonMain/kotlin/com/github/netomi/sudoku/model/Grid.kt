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
package com.github.netomi.sudoku.model

import kotlin.properties.Delegates

class Grid
{
    val type: Type

    val gridSize: Int
        get() = type.gridSize

    val cellCount: Int
        get() = type.cellCount

    private val _cells: MutableList<Cell>
    private val peerSets: Array<MutableCellSet>

    private val _rows:    MutableList<Row>
    private val _columns: MutableList<Column>
    private val _blocks:  MutableList<Block>

    private val cellSets: Array<MutableCellSet>

    private val potentialPositions: Array<MutableCellSet>

    private var stateValid: Boolean by Delegates.observable(false) { _, _, newValue ->
        if (newValue) _onUpdate.invoke()
    }

    private var _onUpdate: () -> Unit = {}
    fun onUpdate(target: () -> Unit) {
        _onUpdate = target
    }

    internal constructor(type: Type) {
        this.type = type

        val gridSize  = type.gridSize
        val cellCount = type.cellCount

        _cells    = ArrayList(cellCount)
        peerSets  = Array(cellCount) { MutableCellSet.empty(this) }

        _rows     = ArrayList(gridSize)
        _columns  = ArrayList(gridSize)
        _blocks   = ArrayList(gridSize)
        cellSets = Array(3 * gridSize) { MutableCellSet.empty(this) }

        var houseIndex = 0
        for (i in 0 until gridSize) {
            _rows.add(Row(this, i, houseIndex++))
            _columns.add(Column(this, i, houseIndex++))
            _blocks.add(Block(this, i, houseIndex++))
        }

        for (i in 0 until cellCount) {
            val rowIndex    = type.getRowIndex(i)
            val columnIndex = type.getColumnIndex(i)
            val blockIndex  = type.getBlockIndex(i)

            val cell = Cell(this, i, rowIndex, columnIndex, blockIndex)
            _cells.add(cell)

            addCell(_rows[rowIndex].houseIndex, cell)
            addCell(_columns[columnIndex].houseIndex, cell)
            if (blockIndex >= 0) {
                addCell(_blocks[blockIndex].houseIndex, cell)
            }
        }

        houses.forEach { house -> house.cells.forEach { cell -> addPeers(cell, house.cellSet) } }

        potentialPositions = Array(gridSize) { MutableCellSet.empty(this) }

        stateValid = true
    }

    /**
     * Copy constructor for grids.
     */
    internal constructor(other: Grid) {
        type = other.type

        _cells   = ArrayList(cellCount)
        _rows    = ArrayList(gridSize)
        _columns = ArrayList(gridSize)
        _blocks  = ArrayList(gridSize)

        // structural components are immutable and can be re-used
        peerSets = other.peerSets
        cellSets = other.cellSets

        // Copy houses
        other._rows.forEach    { _rows.add(it.copy(this)) }
        other._columns.forEach { _columns.add(it.copy(this)) }
        other._blocks.forEach  { _blocks.add(it.copy(this)) }

        // Copy cells
        other._cells.forEach { _cells.add(it.copy(this)) }

        potentialPositions = Array(gridSize) { MutableCellSet.empty(this) }

        stateValid = false

        updateState()
    }

    fun copy(): Grid {
        return Grid(this)
    }

    val cells: Sequence<Cell>
        get() = _cells.asSequence()

    val rows: Sequence<Row>
        get() = _rows.asSequence()

    val columns: Sequence<Column>
        get() = _columns.asSequence()

    val blocks: Sequence<Block>
        get() = _blocks.asSequence()

    val houses: Sequence<House>
        get() = rows + columns + blocks

    fun regionsAfter(house: House): Sequence<House> {
        return when (house.type) {
            HouseType.ROW    -> _rows.subList(house.regionIndex + 1, _rows.size)
            HouseType.COLUMN -> _columns.subList(house.regionIndex + 1, _columns.size)
            HouseType.BLOCK  -> _blocks.subList(house.regionIndex + 1, _blocks.size)
        }.asSequence()
    }

    fun getCell(cellIndex: Int): Cell {
        return _cells[cellIndex]
    }

    fun getCell(row: Int, column: Int): Cell {
        return _cells[type.getCellIndex(row, column)]
    }

    internal fun getPeerSet(cellIndex: Int): CellSet {
        return peerSets[cellIndex]
    }

    private fun addPeers(cell: Cell, cells: CellSet) {
        val cellIndex = cell.cellIndex
        val peerSet   = peerSets[cellIndex]
        peerSet.or(cells)
        peerSet.clear(cell.cellIndex)
    }

    fun getRow(rowIndex: Int): Row {
        return _rows[rowIndex]
    }

    fun getColumn(columnIndex: Int): Column {
        return _columns[columnIndex]
    }

    fun getBlock(blockIndex: Int): Block {
        return _blocks[blockIndex]
    }

    internal fun getCellSet(houseIndex: Int): CellSet {
        return cellSets[houseIndex]
    }

    /**
     * Adds the given [Cell] to this [House].
     */
    private fun addCell(houseIndex: Int, cell: Cell) {
        cellSets[houseIndex].set(cell.cellIndex)
    }

    /**
     * Returns whether the sudoku grid is fully solved with a valid solution.
     */
    val isSolved: Boolean
        get() = houses.all { it.isSolved }

    /**
     * Returns whether the current state of the sudoku grid is valid wrt the
     * normal sudoku constraints. The grid might not be fully solved yet.
     */
    val isValid: Boolean
        get() = houses.all { it.isValid }

    val conflicts: Array<Conflict>
        get() = accept(ConflictDetector())

    // Visitor methods.

    fun <T> accept(visitor: GridVisitor<T>): T {
        return visitor.visitGrid(this)
    }

    fun acceptCells(visitor: CellVisitor) {
        cells.forEach { cell -> visitor.visitCell(cell) }
    }

    fun acceptRows(visitor: HouseVisitor) {
        rows.forEach { row -> visitor.visitRow(row) }
    }

    fun acceptColumns(visitor: HouseVisitor) {
        columns.forEach { column -> visitor.visitColumn(column) }
    }

    fun acceptBlocks(visitor: HouseVisitor) {
        blocks.forEach { block -> visitor.visitBlock(block) }
    }

    fun acceptHouses(visitor: HouseVisitor) {
        acceptRows(visitor)
        acceptColumns(visitor)
        acceptBlocks(visitor)
    }

    // Internal state related methods.

    internal fun notifyCellValueChanged(cell: Cell, oldValue: Int, newValue: Int) {
        // If the value did not really change, there is nothing to do.
        if (oldValue == newValue) {
            stateValid = true
            return
        }

        // First: update assigned values in affected houses.
        cell.row   .updateAssignedValues()
        cell.column.updateAssignedValues()
        cell.block .updateAssignedValues()

        // Second: update possible values in affected cells.
        val affectedCells = sequenceOf(cell) + cell.peers
        for (affectedCell in affectedCells) {
            affectedCell.resetPossibleValues()
            affectedCell.updatePossibleValues(affectedCell.row._assignedValueSet)
            affectedCell.updatePossibleValues(affectedCell.column._assignedValueSet)
            affectedCell.updatePossibleValues(affectedCell.block._assignedValueSet)
        }
        // Third: update potential positions for affected cells.
        val peers = cell.peerSet
        for (positions in potentialPositions) {
            positions.clear(cell.cellIndex)
            positions.andNot(peers)
        }

        for (affectedCell in affectedCells) {
            for (value in affectedCell._possibleValueSet.values) {
                potentialPositions[value - 1].set(affectedCell.cellIndex)
            }
        }

        stateValid = true
    }

    internal fun notifyPossibleValuesChanged(cell: Cell) {
        potentialPositions.forEach { potentialPosition -> potentialPosition.clear(cell.cellIndex) }
        cell._possibleValueSet.values.forEach { value -> potentialPositions[value - 1].set(cell.cellIndex) }
        stateValid = true
    }

    internal fun invalidateState() {
        stateValid = false
    }

    internal fun throwIfStateIsInvalid() {
        check(stateValid) { "cache data is invalidated, need to call refreshCache() before accessing cached data" }
    }

    internal fun getPotentialPositions(value: Int): CellSet {
        return potentialPositions[value - 1].asCellSet()
    }

    fun updateState() {
        if (stateValid) {
            return
        }

        // First: reset the possible values in all cells.
        cells.forEach { obj -> obj.resetPossibleValues() }

        // Second: refresh all assigned values in each house.
        houses.forEach { obj -> obj.updateAssignedValues() }

        // Third: remove potential values in each cell which
        //        are already assigned in the houses it is contained.
        houses.forEach { obj -> obj.updatePossibleValuesInCells() }

        // Fourth: refresh all possible positions for each cell.
        potentialPositions.forEach { cellSet -> cellSet.clearAll() }
        cells.forEach { cell ->
            cell._possibleValueSet.values.forEach { value -> potentialPositions[value - 1].set(cell.cellIndex) }
        }

        stateValid = true
    }

    fun clear(updateGrid: Boolean = true) {
        cells.forEach  { cell -> cell.clear(false) }
        houses.forEach { house -> house.clear() }

        potentialPositions.forEach { cellSet -> cellSet.clearAll() }

        if (updateGrid) {
            updateState()
        }
    }

    fun reset(updateGrid: Boolean = true) {
        cells.forEach { obj: Cell -> obj.reset() }

        if (updateGrid) {
            updateState()
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Grid [").append(type).append("]:\n")
        cells.forEach { cell: Cell -> sb.append("  ").append(cell).append("\n") }
        return sb.toString()
    }

    // Inner helper classes.
    fun interface BlockFunction
    {
        fun getBlockIndex(cellIndex: Int): Int
    }

    class Type constructor(val gridSize: Int, private val blockFunction: BlockFunction)
    {
        val cellCount: Int = gridSize * gridSize

        fun getRowIndex(cellIndex: Int): Int {
            return cellIndex / gridSize
        }

        fun getColumnIndex(cellIndex: Int): Int {
            return cellIndex % gridSize
        }

        fun getBlockIndex(cellIndex: Int): Int {
            return blockFunction.getBlockIndex(cellIndex)
        }

        fun getCellIndex(row: Int, column: Int): Int {
            return (row - 1) * gridSize + (column - 1)
        }

        fun getCellName(cellIndex: Int): String {
            return "r${getRowIndex(cellIndex) + 1}c${getColumnIndex(cellIndex) + 1}"
        }

        override fun toString(): String {
            return "${gridSize}x${gridSize}"
        }
    }

    companion object {
        fun of(type: PredefinedType): Grid {
            return Grid(Type(type.gridSize, type.blockFunction))
        }

        fun of(gridSize: Int, blockFunction: BlockFunction): Grid {
            return Grid(Type(gridSize, blockFunction))
        }
    }
}
